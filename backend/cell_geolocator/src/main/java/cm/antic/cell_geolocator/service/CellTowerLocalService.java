package cm.antic.cell_geolocator.service;

import cm.antic.cell_geolocator.model.GeolocationResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;

@Service
public class CellTowerLocalService {

    private final JdbcTemplate jdbcTemplate;
    private final ReverseGeocodeService reverseGeocodeService;

    public CellTowerLocalService(JdbcTemplate jdbcTemplate,
                                 ReverseGeocodeService reverseGeocodeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.reverseGeocodeService = reverseGeocodeService;
    }

    public GeolocationResponse findLocalTower(String mcc, String mnc, String lac, String cellId) {
        try {
            String sql = """
                    SELECT latitude,
                           longitude,
                           nomdusite AS operator_name
                    FROM orange_cameroon
                    WHERE CAST(lac AS TEXT) = ?
                      AND CAST(ci AS TEXT) = ?
                    LIMIT 1
                    """;

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, lac, cellId);

            if (!results.isEmpty()) {
    Map<String, Object> row = results.get(0);
    GeolocationResponse resp = new GeolocationResponse();

    // Convert String or Number safely to Double
    Object latObj = row.get("latitude");
    Object lonObj = row.get("longitude");

    if (latObj != null && lonObj != null) {
        resp.setLatitude(Double.valueOf(latObj.toString()));
        resp.setLongitude(Double.valueOf(lonObj.toString()));
    } else {
        throw new RuntimeException("Latitude or Longitude is null in database result");
    }

    resp.setProviderUsed("LOCAL_DB(orange_cameroon): " + row.get("operator_name"));
    resp.setAddress("Resolved from Orange-Cameroon dataset");

    reverseGeocodeService.addAddressToResponse(resp);
    return resp;
}
        } catch (DataAccessException e) {
            System.err.println("Local DB lookup failed: " + e.getMessage());
        }
        return null;
    }

}


// resp.setLatitude(Double.parseDouble(latObj.toString()));
// resp.setLongitude(Double.parseDouble(lonObj.toString()));