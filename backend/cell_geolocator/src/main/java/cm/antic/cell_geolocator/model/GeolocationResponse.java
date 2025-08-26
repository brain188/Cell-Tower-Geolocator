package cm.antic.cell_geolocator.model;

import lombok.Data;

import java.util.Map;

@Data
public class GeolocationResponse {
    private Double latitude;
    private Double longitude;
    private String providerUsed;
    private Map<String, Object> rawResponses;

    public Map<String, Object> getRawResponses() {
    return rawResponses;
}

    public void setRawResponses(Map<String, Object> rawResponses) {
        this.rawResponses = rawResponses;
}
}
