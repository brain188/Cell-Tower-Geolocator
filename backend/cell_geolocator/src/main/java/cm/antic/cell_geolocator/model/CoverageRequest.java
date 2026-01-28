package cm.antic.cell_geolocator.model;

import lombok.Data;

@Data
public class CoverageRequest {

    private String area;
    private double radiusMeters;
    private String provider;
}
