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
        SELECT c2.lac, c2.ci, c2."Id BTS New", c2.latitude, c2.longitude
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
            String sql = """
                    SELECT latitude,
                           longitude,
                           nomdusite AS operator_name,
                           "Id BTS New"
                    FROM orange_cameroon
                    WHERE CAST(lac AS TEXT) = ?
                      AND CAST(ci AS TEXT) = ?
                    LIMIT 1
                    """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, lac, cellId);

            if (!results.isEmpty()) {
                Map<String, Object> row = results.get(0);
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

                // Wait so DB result returns with address properly
                reverseGeocodeService.addAddressToResponseAsync(resp).join();

                System.out.println("LOCAL DB HIT");
                return resp;
            }

        } catch (DataAccessException e) {
            System.err.println("Local DB lookup failed: " + e.getMessage());
        }

        return null;
    }
}
