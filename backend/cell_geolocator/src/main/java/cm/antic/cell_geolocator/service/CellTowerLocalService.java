package cm.antic.cell_geolocator.service;

import cm.antic.cell_geolocator.model.GeolocationResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

@Service
public class CellTowerLocalService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
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
            // Try EXACT match 
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

            List<Map<String, Object>> exactResults = jdbcTemplate.queryForList(exactSql, lac, cellId);

            if (!exactResults.isEmpty()) {
                Map<String, Object> row = exactResults.get(0);
                GeolocationResponse resp = buildResponse(row);
                resp.setCellId(cellId);                   // actual used
                resp.setOriginalRequestedCellId(cellId);  // requested
                resp.setFallbackUsed(false);              // no fallback
                resp.setTechnoCell((String) row.get("techno_cell"));
                resp.setFrequenceCell((String) row.get("frequence_cell"));
                addAddressAsync(resp);
                System.out.println("LOCAL DB HIT (exact)");
                return resp;
            }

            // Fallback: Closest Cell ID in same LAC
            String fallbackSql = """
                    SELECT ci,
                           latitude,
                           longitude,
                           nomdusite AS operator_name,
                           "Id BTS New",
                           ABS(CAST(ci AS INTEGER) - ?) AS distance
                           "Techno Cell" AS techno_cell,
                           "Fréquence Cell" AS frequence_cell
                    FROM orange_cameroon
                    WHERE lac = ?
                    ORDER BY distance ASC
                    LIMIT 1
                    """;

            List<Map<String, Object>> fallbackResults = jdbcTemplate.queryForList(fallbackSql, Integer.parseInt(cellId), lac);

            if (!fallbackResults.isEmpty()) {
                Map<String, Object> row = fallbackResults.get(0);
                GeolocationResponse resp = buildResponse(row);
                String usedCellId = row.get("ci").toString();
                resp.setCellId(usedCellId);                   // actual used
                resp.setOriginalRequestedCellId(cellId);      // requested
                resp.setFallbackUsed(true);                   // fallback used!
                resp.setTechnoCell((String) row.get("techno_cell"));
                resp.setFrequenceCell((String) row.get("frequence_cell"));
                addAddressAsync(resp);
                System.out.println("LOCAL DB HIT (fallback: " + usedCellId + ")");
                return resp;
            }

            // No match at all
            return null;

        } catch (DataAccessException e) {
            System.err.println("Local DB lookup failed: " + e.getMessage());
        } catch (NumberFormatException e) {
            System.err.println("Invalid Cell ID format: " + cellId);
        }

        return null;
    }

    // Helper: Build response from DB row
    private GeolocationResponse buildResponse(Map<String, Object> row) {
        GeolocationResponse resp = new GeolocationResponse();

        Object latObj = row.get("latitude");
        Object lonObj = row.get("longitude");

        if (latObj != null && lonObj != null) {
            resp.setLatitude(Double.valueOf(latObj.toString()));
            resp.setLongitude(Double.valueOf(lonObj.toString()));
        } else {
            throw new RuntimeException("Latitude or Longitude is null in DB result");
        }

        resp.setProviderUsed("LOCAL_DB(orange_cameroon): " + row.get("operator_name"));
        return resp;
    }

    // Helper: Add address asynchronously
    private void addAddressAsync(GeolocationResponse resp) {
        reverseGeocodeService.addAddressToResponseAsync(resp).join();
    }
}