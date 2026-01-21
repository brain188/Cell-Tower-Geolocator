package cm.antic.cell_geolocator.service;

import cm.antic.cell_geolocator.model.GeolocationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CellTowerLocalService {

    private static final Logger log = LoggerFactory.getLogger(CellTowerLocalService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ReverseGeocodeService reverseGeocodeService;

    public CellTowerLocalService(JdbcTemplate jdbcTemplate,
                                 ReverseGeocodeService reverseGeocodeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.reverseGeocodeService = reverseGeocodeService;
    }

    public List<Map<String, Object>> findCellsByBtsId(String cellId) {
        String sql = """
            SELECT c2.lac, c2.ci, c2."Id BTS New", c2.latitude, c2.longitude,
                   c2."Techno Cell" AS techno_cell,
                   c2."Fréquence Cell" AS frequence_cell
            FROM orange_cameroon c1
            JOIN orange_cameroon c2 ON c1."Id BTS New" = c2."Id BTS New"
            WHERE c1.ci = ?
              AND c2.ci <> ?
            ORDER BY c2.ci
        """;

        return jdbcTemplate.queryForList(sql, cellId, cellId);
    }

    public GeolocationResponse findLocalTower(String mcc, String mnc, String lac, String cellId) {
        try {
            // Exact match
            String exactSql = """
                SELECT latitude,
                       longitude,
                       nomdusite AS operator_name,
                       "Id BTS New",
                       "Techno Cell" AS techno_cell,
                       "Fréquence Cell" AS frequence_cell,
                       ci
                FROM orange_cameroon
                WHERE lac = ?
                  AND ci = ?
                LIMIT 1
            """;

            List<Map<String, Object>> exactResults =
                    jdbcTemplate.queryForList(exactSql, lac, cellId);

            if (!exactResults.isEmpty()) {
                GeolocationResponse resp = buildResponse(exactResults.get(0));
                if (resp == null) return null;

                resp.setCellId(cellId);
                resp.setOriginalRequestedCellId(cellId);
                resp.setFallbackUsed(false);

                addAddressAsync(resp);
                log.info("LOCAL DB HIT (exact) for cellId={}", cellId);
                return resp;
            }

            // Fallback: closest CI in same LAC
            String fallbackSql = """
                SELECT ci,
                       latitude,
                       longitude,
                       nomdusite AS operator_name,
                       "Id BTS New",
                       ABS(CAST(ci AS INTEGER) - ?) AS distance,
                       "Techno Cell" AS techno_cell,
                       "Fréquence Cell" AS frequence_cell
                FROM orange_cameroon
                WHERE lac = ?
                ORDER BY distance ASC
                LIMIT 1
            """;

            List<Map<String, Object>> fallbackResults =
                    jdbcTemplate.queryForList(fallbackSql, Integer.parseInt(cellId), lac);

            if (!fallbackResults.isEmpty()) {
                Map<String, Object> row = fallbackResults.get(0);
                GeolocationResponse resp = buildResponse(row);
                if (resp == null) return null;

                String usedCellId = row.get("ci").toString();
                resp.setCellId(usedCellId);
                resp.setOriginalRequestedCellId(cellId);
                resp.setFallbackUsed(true);

                addAddressAsync(resp);
                log.info("LOCAL DB HIT (fallback) requested={}, used={}", cellId, usedCellId);
                return resp;
            }

            return null;

        } catch (DataAccessException e) {
            log.error("Local DB lookup failed", e);
        } catch (NumberFormatException e) {
            log.warn("Invalid Cell ID format: {}", cellId);
        }

        return null;
    }

    private GeolocationResponse buildResponse(Map<String, Object> row) {
        Object latObj = row.get("latitude");
        Object lonObj = row.get("longitude");

        if (latObj == null || lonObj == null) {
            log.warn("Latitude or Longitude is null for DB row: {}", row);
            return null;
        }

        GeolocationResponse resp = new GeolocationResponse();
        resp.setLatitude(Double.parseDouble(latObj.toString()));
        resp.setLongitude(Double.parseDouble(lonObj.toString()));
        resp.setProviderUsed("LOCAL_DB(orange_cameroon): " + row.get("operator_name"));
        resp.setTechnoCell((String) row.get("techno_cell"));
        resp.setFrequenceCell((String) row.get("frequence_cell"));

        return resp;
    }

    private void addAddressAsync(GeolocationResponse resp) {
        try {
            reverseGeocodeService
                .addAddressToResponseAsync(resp)
                .get(); // BLOCKS until address is set
        } catch (Exception e) {
            log.warn("Reverse geocoding failed, returning response without address", e);
        }
    }
}    