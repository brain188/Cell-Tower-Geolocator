package cm.antic.cell_geolocator.model;

import lombok.Data;
import java.util.List;

@Data
public class CoverageResponse {

    private double penetrationRate;   // e.g. 85.5
    private double coveredAreaKm2;    // e.g. 2.1
    private double totalAreaKm2;      // e.g. 2.5
    private String classification;    // "High"/"Medium"/"Low"
    private String message;           // formatted summary
    private List<String> coveragePolygonsGeoJson; // GeoJSON strings for each cell circle
}
