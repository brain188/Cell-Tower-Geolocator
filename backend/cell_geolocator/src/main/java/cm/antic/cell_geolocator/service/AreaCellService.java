package cm.antic.cell_geolocator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AreaCellService {

    private static final Logger log = LoggerFactory.getLogger(AreaCellService.class);

    private final JdbcTemplate jdbcTemplate;

    public AreaCellService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getCellsByArea(String query, String provider) {

        String normalizedArea = normalizeAreaName(query);
        String searchTerm = "%" + normalizedArea + "%";

        log.info(
            "Area cell search started | area='{}' | provider='{}'",
            normalizedArea, provider
        );

        List<Map<String, Object>> cells;

        try {
            if ("mtn".equalsIgnoreCase(provider)) {
                cells = searchMtn(searchTerm);
            } else {
                // default → ORANGE
                cells = searchOrange(searchTerm);
            }

            log.info(
                "Area cell search completed | area='{}' | provider='{}' | cellsFound={}",
                normalizedArea, provider, cells.size()
            );

            return cells;

        } catch (Exception e) {
            log.error(
                "Area cell search failed | area='{}' | provider='{}'",
                normalizedArea, provider, e
            );
            return List.of();
        }
    }

    // ORANGE SEARCH

    private List<Map<String, Object>> searchOrange(String term) {

        String sql = """
            SELECT DISTINCT ON (lac, ci)
                   latitude,
                   longitude,
                   lac,
                   ci,
                   nomdusite AS site_name,
                   "Id BTS New" AS bts_id,
                   localité AS localite,
                   quartier,
                   département AS departement,
                   "Region Terr" AS region_terr,
                   "Region Bus" AS region_bus,
                   "Techno Cell" AS techno_cell,
                   "Fréquence Cell" AS frequence_cell,
                   'orange' AS provider
            FROM orange_cameroon
            WHERE LOWER(localité) LIKE LOWER(?)
               OR LOWER(quartier) LIKE LOWER(?)
               OR LOWER(département) LIKE LOWER(?)
               OR LOWER("Region Terr") LIKE LOWER(?)
               OR LOWER("Region Bus") LIKE LOWER(?)
        """;

        return jdbcTemplate.queryForList(
            sql, term, term, term, term, term
        );
    }

    //MTN SEARCH 

    private List<Map<String, Object>> searchMtn(String term) {

        String sql = """
            SELECT DISTINCT ON (lac, ci)
                   latitude,
                   longitude,
                   lac,
                   ci,
                   operator_site AS site_name,
                   NULL::VARCHAR AS bts_id,
                   localité AS localite,
                   NULL::VARCHAR AS quartier,
                   département AS departement,
                   "Region Terr" AS region_terr,
                   NULL::VARCHAR AS region_bus,
                   NULL::VARCHAR AS techno_cell,
                   NULL::VARCHAR AS frequence_cell,
                   'mtn' AS provider
            FROM mtn_cameroon
            WHERE LOWER(localité) LIKE LOWER(?)
               OR LOWER(département) LIKE LOWER(?)
               OR LOWER("Region Terr") LIKE LOWER(?)
        """;

        return jdbcTemplate.queryForList(sql, term, term, term);
    }

    // NORMALIZATION 

    private String normalizeAreaName(String name) {
        if (name == null) return "";

        String normalized = name.toLowerCase();
        normalized = normalized.replace("-", " ");
        normalized = normalized.replaceAll("\\s+", " ");
        normalized = normalized.trim();

        // common variations
        if (normalized.equals("southwest")) normalized = "south west";
        if (normalized.equals("northwest")) normalized = "north west";

        return normalized;
    }
}
