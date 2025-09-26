package cm.antic.cell_geolocator.model;

import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

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

    
    public GeolocationResponse getChosen() {
        return chosen;
    }

    public void setChosen(GeolocationResponse chosen) {
        this.chosen = chosen;
    }

    public Map<String, GeolocationResponse> getAllResponses() {
        return allResponses;
    }

    public void setAllResponses(Map<String, GeolocationResponse> allResponses) {
        this.allResponses = allResponses;
    }

    public Map<String, Double> getDistances() {
        return distances;
    }

    public void setDistances(Map<String, Double> distances) {
        this.distances = distances;
    }

    public String getShortestPair() {
        return shortestPair;
    }

    public void setShortestPair(String shortestPair) {
        this.shortestPair = shortestPair;
    }
}
