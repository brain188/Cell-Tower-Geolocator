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
            String orangeSql = """
                SELECT latitude,
                       longitude,
                       nomdusite AS operator_name,
                       localité,
                       quartier,
                       "Region Terr",
                       département,
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
                log.info("DB HIT: orange exact for cellId={}", cellId);

                GeolocationResponse resp = buildResponse(exactResults.get(0), "orange");
                if (resp == null) return null;

                resp.setCellId(cellId);
                resp.setOriginalRequestedCellId(cellId);
                resp.setFallbackUsed(false);

                addAddressAsync(resp);
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

        // ✅ Use DB address FIRST (no format change)
        String address = buildAddressFromDb(row, provider);
        resp.setAddress(address);

        log.info("Address from DB set: {}", address);

        return resp;
    }

    private String buildAddressFromDb(Map<String, Object> row, String provider) {

        if ("orange".equals(provider)) {
            return String.join(", ",
                safe(row.get("quartier")),
                safe(row.get("localité")),
                safe(row.get("département")),
                safe(row.get("Region Terr"))
            );
        }

        if ("mtn".equals(provider)) {
            return String.join(", ",
                safe(row.get("localité")),
                safe(row.get("département")),
                safe(row.get("Region Terr"))
            );
        }

        return null;
    }

    private String safe(Object val) {
        return val != null ? val.toString() : "";
    }

    private void addAddressAsync(GeolocationResponse resp) {
        // Skip reverse geocoding if DB already gave an address
        if (resp.getAddress() != null && !resp.getAddress().isEmpty()) {
            log.info("Skipping reverse geocoding (DB already provides address)");
            return;
        }

        try {
            log.info("Calling reverse geocoding (DB missing address)");
            reverseGeocodeService.addAddressToResponseAsync(resp).get();
        } catch (Exception e) {
            log.warn("Reverse geocoding failed", e);
        }
    }    
}
