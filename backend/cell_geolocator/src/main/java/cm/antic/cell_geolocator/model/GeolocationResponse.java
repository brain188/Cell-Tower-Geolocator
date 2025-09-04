package cm.antic.cell_geolocator.model;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
public class GeolocationResponse {
    private Double latitude;
    private Double longitude;
    private String providerUsed;
    private String address;
    private AddressDetail addressDetail;


    private Map<String, Object> rawResponses = new HashMap<>();

    @Data
    public static class AddressDetail {
        private String country;
        private String countryCode;
        private String stateOrRegion;
        private String cityOrTown;
        private String postalCode;
        private String street;
    }
}
