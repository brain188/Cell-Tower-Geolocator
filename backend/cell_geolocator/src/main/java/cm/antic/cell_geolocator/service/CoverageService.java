package cm.antic.cell_geolocator.service;

import cm.antic.cell_geolocator.model.CoverageResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class CoverageService {

    private static final Logger log = LoggerFactory.getLogger(CoverageService.class);

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;

    public CoverageService(JdbcTemplate jdbcTemplate, RestTemplate restTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.restTemplate = restTemplate;
    }

    public CoverageResponse calculateCoverage(String area, double radiusMeters) {

        log.info("Starting coverage calculation | area='{}', radius={}m", area, radiusMeters);

        CoverageResponse response = new CoverageResponse();

        try {
            // Get region polygon
            log.debug("Fetching region polygon for '{}'", area);
            String regionGeoJson = getRegionPolygonFromNominatim(area);

            if (regionGeoJson == null) {
                log.warn("No polygon found for area '{}'", area);
                response.setMessage("Region not found");
                return response;
            }

            // Fetch cells
            log.debug("Fetching cells in area '{}'", area);
            List<Map<String, Object>> cells = getCellsInArea(area);
            log.info("Found {} cells in area '{}'", cells.size(), area);

            // Total region area
            log.debug("Computing total region area");
            Double totalAreaM2 = jdbcTemplate.queryForObject(
                "SELECT ST_Area(ST_GeomFromGeoJSON(?)::geography)",
                Double.class,
                regionGeoJson
            );

            if (totalAreaM2 == null || totalAreaM2 <= 0) {
                log.warn("Invalid total area computed for '{}'", area);
                response.setMessage("Invalid region geometry");
                return response;
            }

            log.info("Total area for '{}' = {} km²", area, totalAreaM2 / 1_000_000);

            // Covered area using PostGIS 
            log.debug("Computing covered area using PostGIS buffers");

            String sql = """
                WITH cell_buffers AS (
                    SELECT
                        ST_Buffer(geom::geography, ?)::geometry AS buffer
                    FROM orange_cameroon
                    WHERE
                        LOWER(localité) LIKE LOWER(?)
                        OR LOWER(quartier) LIKE LOWER(?)
                        OR LOWER(département) LIKE LOWER(?)
                        OR LOWER("Region Terr") LIKE LOWER(?)
                        OR LOWER("Region Bus") LIKE LOWER(?)
                ),
                unioned AS (
                    SELECT ST_Union(buffer) AS coverage_geom
                    FROM cell_buffers
                )
                SELECT ST_Area(
                    ST_Intersection(
                        unioned.coverage_geom,
                        ST_GeomFromGeoJSON(?)
                    )::geography
                )
                FROM unioned
            """;

            String term = "%" + area + "%";

            Double coveredAreaM2 = jdbcTemplate.queryForObject(
                sql,
                Double.class,
                radiusMeters,
                term, term, term, term, term,
                regionGeoJson
            );

            if (coveredAreaM2 == null) {
                log.warn("Covered area returned NULL for '{}', defaulting to 0", area);
                coveredAreaM2 = 0.0;
            }

            log.info("Covered area for '{}' = {} km²", area, coveredAreaM2 / 1_000_000);

            // Penetration rate
            double penetration = (coveredAreaM2 / totalAreaM2) * 100.0;

            // Classification
            String classification =
                penetration < 50 ? "Low" :
                penetration < 80 ? "Medium" : "High";

            log.info(
                "Coverage result | area='{}', penetration={}%, classification={}",
                area,
                String.format("%.2f", penetration),
                classification
            );

            // Response
            response.setTotalAreaKm2(totalAreaM2 / 1_000_000);
            response.setCoveredAreaKm2(coveredAreaM2 / 1_000_000);
            response.setPenetrationRate(penetration);
            response.setClassification(classification);
            response.setMessage(
                String.format(
                    "Coverage of %s with %.0fm radius: %.1f%% (%s)",
                    area, radiusMeters, penetration, classification
                )
            );

            log.info("Coverage calculation completed successfully for '{}'", area);
            return response;

        } catch (Exception e) {
            log.error("Coverage calculation failed for '{}'", area, e);
            response.setMessage("Internal error while calculating coverage");
            return response;
        }
    }

    // Nominatim polygon fetch
    private String getRegionPolygonFromNominatim(String areaName) {
        try {
            log.debug("Calling Nominatim API for '{}'", areaName);

            String url =
                "https://nominatim.openstreetmap.org/search?q=" +
                URLEncoder.encode(areaName, StandardCharsets.UTF_8) +
                "&format=geojson&polygon_geojson=1&limit=1";

            String body = restTemplate.getForObject(url, String.class);

            if (body == null || body.isBlank()) {
                log.warn("Empty response from Nominatim for '{}'", areaName);
                return null;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);
            JsonNode features = root.path("features");

            if (!features.isArray() || features.isEmpty()) {
                log.warn("No features returned for area '{}'", areaName);
                return null;
            }

            log.info("Successfully extracted geometry for '{}'", areaName);
            return mapper.writeValueAsString(
                features.get(0).path("geometry")
            );

        } catch (Exception e) {
            log.error("Failed to fetch geometry for area '{}'", areaName, e);
            return null;
        }
    }
    // Database cell fetch
    private List<Map<String, Object>> getCellsInArea(String area) {
        log.debug("Querying database for cells in '{}'", area);

        String sql = """
            SELECT latitude, longitude, geom
            FROM orange_cameroon
            WHERE
                LOWER(localité) LIKE LOWER(?)
                OR LOWER(quartier) LIKE LOWER(?)
                OR LOWER(département) LIKE LOWER(?)
                OR LOWER("Region Terr") LIKE LOWER(?)
                OR LOWER("Region Bus") LIKE LOWER(?)
        """;

        String term = "%" + area + "%";

        List<Map<String, Object>> results =
            jdbcTemplate.queryForList(sql, term, term, term, term, term);

        log.debug("Database returned {} cell records for '{}'", results.size(), area);
        return results;
    }
}
