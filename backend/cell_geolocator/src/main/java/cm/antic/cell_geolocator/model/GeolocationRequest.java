package cm.antic.cell_geolocator.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Request payload for resolving a cell tower geolocation")
public class GeolocationRequest {

    @Schema(description = "Mobile Country Code", example = "310")
    private String mcc;

    @Schema(description = "Mobile Network Code", example = "260")
    private String mnc;

    @Schema(description = "Location Area Code", example = "7033")
    private String lac;

    @Schema(description = "Cell ID", example = "56789")
    private String cellId; 

    @Schema(description = "Range in meters", example = "100")
    private Integer range;
}
