package cm.antic.cell_geolocator.model;

import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Result of priority-based geolocation resolution")
public class PriorityGeolocationResult {
    
    @Schema(description = "The chosen geolocation response after applying priority rules")
    private GeolocationResponse chosen;

    @Schema(description = "All provider responses with their results")
    private Map<String, GeolocationResponse> allResponses;

    @Schema(description = "Distances between different provider responses")
    private Map<String, Double> distances;

    @Schema(description = "The pair of providers with the shortest distance")
    private String shortestPair;

}
