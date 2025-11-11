package cm.antic.cell_geolocator.model;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Schema(description = "Response containing resolved geolocation information")
public class GeolocationResponse {

    @Schema(description = "Latitude of the resolved location", example = "37.7749")
    private Double latitude;

    @Schema(description = "Longitude of the resolved location", example = "-122.4194")
    private Double longitude;

    @Schema(description = "Accuracy of the resolved location in meters", example = "100")
    private Double accuracy;
    
    @Schema(description = "The provider used for resolution", example = "ProviderA")
    private String providerUsed;

    @Schema(description = "Address of the resolved location", example = "1 Infinite Loop, Cupertino, CA 95014, USA")
    private String address;

    private AddressDetail addressDetail;

    @Schema(description = "Error message if resolution failed", example = "Provider error")
    private String error;

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

    public void setError(String error) {
        this.error = error;
    }
}
