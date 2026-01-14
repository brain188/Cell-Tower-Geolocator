package cm.antic.cell_geolocator.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AreaCellService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getCellsByArea(String query) {
        // Split the query into individual words for more flexible matching
        String[] words = query.replaceAll("[,]", "")
                                .trim()
                                .split("\\s+");  // split by space

        if (words.length == 0) {
            return new ArrayList<>(); // No words -> empty result
        }

        StringBuilder sqlBuilder = new StringBuilder("""
            SELECT ci, lac, latitude, longitude, nomdusite AS site_name, "Id BTS New" AS bts_id,
                   localité, quartier, "Region Terr", "Region Bus", département
            FROM orange_cameroon
            WHERE 
        """);

        List<String> params = new ArrayList<>();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                sqlBuilder.append(" OR ");
            }
            String wordParam = "%" + words[i] + "%"; // Fuzzy match for each word
            sqlBuilder.append("""
                LOWER(localité) LIKE LOWER(?) 
                OR LOWER(quartier) LIKE LOWER(?) 
                OR LOWER("Region Terr") LIKE LOWER(?) 
                OR LOWER("Region Bus") LIKE LOWER(?) 
                OR LOWER(département) LIKE LOWER(?)
            """);

            params.addAll(List.of(wordParam, wordParam, wordParam, wordParam, wordParam));
        }
            
        return jdbcTemplate.queryForList(sqlBuilder.toString(), params.toArray());
    }

}
