package cm.antic.cell_geolocator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AreaCellService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getCellsByArea(String areaName) {
        String sql = """
            SELECT ci, lac, latitude, longitude, nomdusite AS site_name, "Id BTS New" AS bts_id,
                   localité, quartier, "Region Terr", "Region Bus", département
            FROM orange_cameroon
            WHERE LOWER(localité) LIKE LOWER(?) 
               OR LOWER(quartier) LIKE LOWER(?) 
               OR LOWER("Region Terr") LIKE LOWER(?) 
               OR LOWER("Region Bus") LIKE LOWER(?) 
               OR LOWER(département) LIKE LOWER(?)
            """;

        String searchTerm = "%" + areaName + "%"; // Fuzzy match

        return jdbcTemplate.queryForList(sql, searchTerm, searchTerm, searchTerm, searchTerm, searchTerm);
    }

}
