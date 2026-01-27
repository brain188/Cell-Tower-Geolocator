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

    public List<Map<String, Object>> findCellsByBtsId(String cellId, String provider) {
        try {
            if ("orange".equals(provider)) {
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
            } else if ("mtn".equals(provider)) {
                String sql = """
                    SELECT DISTINCT ON (c2.lac, c2.ci) c2.lac, c2.ci, c2.latitude, c2.longitude
                    FROM mtn_cameroon c1
                    JOIN mtn_cameroon c2 ON c1.lac = c2.lac
                    WHERE c1.ci = CAST(? AS BIGINT)
                      AND c2.ci <> CAST(? AS BIGINT)
                    ORDER BY c2.ci
                """;
                return jdbcTemplate.queryForList(sql, cellId, cellId);
            }
            return List.of();
        } catch (Exception e) {
            log.error("Failed to fetch related cells for provider {}: {}", provider, e.getMessage());
            return List.of();
        }
    }

    public GeolocationResponse findLocalTower(String mcc, String mnc, String lac, String cellId) {
        try {
            // Exact match for orange_cameroon
            String orangeSql = """
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
                    jdbcTemplate.queryForList(orangeSql, lac, cellId);
            if (!exactResults.isEmpty()) {
                GeolocationResponse resp = buildResponse(exactResults.get(0), "orange");
                if (resp == null) return null;
                resp.setCellId(cellId);
                resp.setOriginalRequestedCellId(cellId);
                resp.setFallbackUsed(false);
                addAddressAsync(resp);
                log.info("LOCAL DB HIT (orange_exact) for cellId={}", cellId);
                return resp;
            }
            // If not in orange, try EXACT match in mtn_cameroon
            String mtnSql = """
                    SELECT latitude,
                           longitude,
                           operator_site AS operator_name,
                           ci,
                           NULL AS techno_cell,
                           NULL AS frequence_cell
                    FROM mtn_cameroon
                    WHERE lac = CAST(? AS BIGINT)
                        AND ci  = CAST(? AS BIGINT)
                    LIMIT 1
                    """;
            List<Map<String, Object>> mtnResults = jdbcTemplate.queryForList(mtnSql, lac, cellId);
            if (!mtnResults.isEmpty()) {
                Map<String, Object> row = mtnResults.get(0);
                GeolocationResponse resp = buildResponse(row, "mtn");
                resp.setCellId(cellId);
                resp.setOriginalRequestedCellId(cellId);
                resp.setFallbackUsed(false);
                resp.setTechnoCell((String) row.get("techno_cell"));
                resp.setFrequenceCell((String) row.get("frequence_cell"));
                addAddressAsync(resp);
                log.info("LOCAL DB HIT (mtn_exact) for cellId=" + cellId);
                return resp;
            }
            // Fallback: closest CI in same LAC for orange_cameroon
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
                GeolocationResponse resp = buildResponse(row, "orange");
                if (resp == null) return null;
                String usedCellId = row.get("ci").toString();
                resp.setCellId(usedCellId);
                resp.setOriginalRequestedCellId(cellId);
                resp.setFallbackUsed(true);
                addAddressAsync(resp);
                log.info("LOCAL DB HIT (orange_fallback) requested={}, used={}", cellId, usedCellId);
                return resp;
            }
            // Fallback: closest CI in same LAC for mtn_cameroon
            String mtnFallbackSql = """
                    SELECT ci,
                           latitude,
                           longitude,
                           operator_site AS operator_name,
                           ABS(CAST(ci AS INTEGER) - ?) AS distance,
                           NULL AS techno_cell,
                           NULL AS frequence_cell
                    FROM mtn_cameroon
                    WHERE lac = CAST(? AS BIGINT)
                    ORDER BY distance ASC
                    LIMIT 1
                    """;
            List<Map<String, Object>> mtnFallbackResults = jdbcTemplate.queryForList(mtnFallbackSql, Integer.parseInt(cellId), lac);
            if (!mtnFallbackResults.isEmpty()) {
                Map<String, Object> row = mtnFallbackResults.get(0);
                GeolocationResponse resp = buildResponse(row, "mtn");
                String usedCellId = row.get("ci").toString();
                resp.setCellId(usedCellId);
                resp.setOriginalRequestedCellId(cellId);
                resp.setFallbackUsed(true);
                resp.setTechnoCell((String) row.get("techno_cell"));
                resp.setFrequenceCell((String) row.get("frequence_cell"));
                addAddressAsync(resp);
                log.info("LOCAL DB HIT (mtn_fallback) requested={}, used={}", cellId, usedCellId);
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
    private GeolocationResponse buildResponse(Map<String, Object> row, String provider) {
        Object latObj = row.get("latitude");
        Object lonObj = row.get("longitude");
        if (latObj == null || lonObj == null) {
            log.warn("Latitude or Longitude is null for DB row: {}", row);
            return null;
        }
        GeolocationResponse resp = new GeolocationResponse();
        resp.setLatitude(Double.parseDouble(latObj.toString()));
        resp.setLongitude(Double.parseDouble(lonObj.toString()));
        resp.setProviderUsed("LOCAL_DB_" + provider.toUpperCase() + ": " + row.get("operator_name"));
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