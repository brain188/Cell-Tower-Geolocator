package cm.antic.cell_geolocator.service.provider;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class OpenCellIdClient implements ProviderClient {

    @Value("${opencellid.api.key}")
    private String apiKey;

    @Value("${opencellid.api.url:https://opencellid.org/cell/get}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public GeolocationResponse resolve(GeolocationRequest request) {
        String url = String.format("%s?key=%s&mcc=%s&mnc=%s&lac=%s&cellid=%s&format=json", apiUrl, apiKey, request.getMcc(), request.getMnc(), request.getLac(), request.getCellId());
        ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
            url,
            org.springframework.http.HttpMethod.GET,
            null,
            new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
        );
        Map<String, Object> body = responseEntity.getBody();
        GeolocationResponse response = new GeolocationResponse();
        if (body != null && body.containsKey("lat") && body.containsKey("lon")) {
            response.setLatitude((Double) body.get("lat"));
            response.setLongitude((Double) body.get("lon"));
            response.setProviderUsed(getProviderName());
        }
        return response;
    }

    @Override
    public String getProviderName() {
        return "OpenCellID";
    }
}
