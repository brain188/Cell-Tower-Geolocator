package cm.antic.cell_geolocator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NominatimResponse {

    @JsonProperty("display_name")
    private String displayName;

    private Map<String, String> address;

}
