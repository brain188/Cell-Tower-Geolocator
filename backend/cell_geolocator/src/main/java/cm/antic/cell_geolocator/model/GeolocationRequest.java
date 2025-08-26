package cm.antic.cell_geolocator.model;

import lombok.Data;

@Data
public class GeolocationRequest {
    private String mcc;
    private String mnc;
    private String lac;
    private String cellId;
}
