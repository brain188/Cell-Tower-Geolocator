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

    // MAIN ENTRY 

    public CoverageResponse calculateCoverage(String area, double radiusMeters, String provider) {

        if (provider == null || provider.isBlank()) {
            provider = "orange";
        }

        log.info(
            "Starting coverage calculation | area='{}', radius={}m, provider='{}'",
            area, radiusMeters, provider
        );

        CoverageResponse response = new CoverageResponse();

        try {
            // Get region polygon
            String regionGeoJson = getRegionPolygonFromNominatim(area);
            if (regionGeoJson == null) {
                response.setMessage("Region not found");
                return response;
            }

            // Fetch cells
            List<Map<String, Object>> cells = getCellsInArea(area, provider);
            log.info(
                "Cells fetched | area='{}', provider='{}', count={}",
                area, provider, cells.size()
            );

            // Total region area
            Double totalAreaM2 = jdbcTemplate.queryForObject(
                "SELECT ST_Area(ST_GeomFromGeoJSON(?)::geography)",
                Double.class,
                regionGeoJson
            );

            if (totalAreaM2 == null || totalAreaM2 <= 0) {
                response.setMessage("Invalid region geometry");
                return response;
            }

            // Covered area
            Double coveredAreaM2 = calculateCoveredArea(
                area, radiusMeters, provider, regionGeoJson
            );

            if (coveredAreaM2 == null) {
                coveredAreaM2 = 0.0;
            }

            // Penetration
            double penetration = (coveredAreaM2 / totalAreaM2) * 100.0;

            String classification =
                penetration < 50 ? "Low" :
                penetration < 80 ? "Medium" : "High";

            log.info(
                "Coverage result | area='{}', provider='{}', penetration={}%, classification={}",
                area,
                provider,
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
                    "Coverage of %s (%s) with %.0fm radius: %.1f%% (%s)",
                    area, provider, radiusMeters, penetration, classification
                )
            );

            return response;

        } catch (Exception e) {
            log.error(
                "Coverage calculation failed | area='{}', provider='{}'",
                area, provider, e
            );
            response.setMessage("Internal error while calculating coverage");
            return response;
        }
    }

    // COVERED AREA

    private Double calculateCoveredArea(
        String area,
        double radiusMeters,
        String provider,
        String regionGeoJson
    ) {

        String term = "%" + area + "%";

        String sql;

        if ("mtn".equalsIgnoreCase(provider)) {

            sql = """
                WITH cell_buffers AS (
                    SELECT
                        ST_Buffer(
                            ST_SetSRID(ST_Point(longitude, latitude), 4326)::geography,
                            ?
                        )::geometry AS buffer
                    FROM mtn_cameroon
                    WHERE
                        LOWER(localité) LIKE LOWER(?)
                        OR LOWER(département) LIKE LOWER(?)
                        OR LOWER("Region Terr") LIKE LOWER(?)
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

            return jdbcTemplate.queryForObject(
                sql,
                Double.class,
                radiusMeters,
                term, term, term,
                regionGeoJson
            );

        } else {
            // DEFAULT → ORANGE

            sql = """
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

            return jdbcTemplate.queryForObject(
                sql,
                Double.class,
                radiusMeters,
                term, term, term, term, term,
                regionGeoJson
            );
        }
    }

    // CELL FETCH

    private List<Map<String, Object>> getCellsInArea(String area, String provider) {

        String term = "%" + area + "%";

        if ("mtn".equalsIgnoreCase(provider)) {

            String sql = """
                SELECT latitude, longitude
                FROM mtn_cameroon
                WHERE
                    LOWER(localité) LIKE LOWER(?)
                    OR LOWER(département) LIKE LOWER(?)
                    OR LOWER("Region Terr") LIKE LOWER(?)
            """;

            return jdbcTemplate.queryForList(sql, term, term, term);

        } else {
            // ORANGE 

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

            return jdbcTemplate.queryForList(
                sql, term, term, term, term, term
            );
        }
    }

    //NOMINATIM

    private String getRegionPolygonFromNominatim(String areaName) {
        try {
            String url =
                "https://nominatim.openstreetmap.org/search?q=" +
                URLEncoder.encode(areaName, StandardCharsets.UTF_8) +
                "&format=geojson&polygon_geojson=1&limit=1";

            String body = restTemplate.getForObject(url, String.class);
            if (body == null || body.isBlank()) return null;

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);
            JsonNode features = root.path("features");

            if (!features.isArray() || features.isEmpty()) return null;

            return mapper.writeValueAsString(
                features.get(0).path("geometry")
            );

        } catch (Exception e) {
            log.error("Failed to fetch geometry for '{}'", areaName, e);
            return null;
        }
    }
}
