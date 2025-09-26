package cm.antic.cell_geolocator.service.provider;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.service.ReverseGeocodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import org.springframework.web.client.RestClientException;

@Component
public class OpenCellIdClient implements ProviderClient {

    @Value("${opencellid.api.key}")
    private String apiKey;

    @Value("${opencellid.api.url:https://opencellid.org/cell/get}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ReverseGeocodeService reverseGeocodeService;

    @Override
    public GeolocationResponse resolve(GeolocationRequest request) {
        GeolocationResponse response = new GeolocationResponse();
        response.setProviderUsed(getProviderName());

        try {
            String url = String.format("%s?key=%s&mcc=%s&mnc=%s&lac=%s&cellid=%s&format=json",
                    apiUrl, apiKey, request.getMcc(), request.getMnc(), request.getLac(), request.getCellId());

            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    url,
                    org.springframework.http.HttpMethod.GET,
                    null,
                    new org.springframework.core.ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = responseEntity.getBody();

            if (body != null && body.containsKey("lat") && body.containsKey("lon")) {
                response.setLatitude(((Number) body.get("lat")).doubleValue());
                response.setLongitude(((Number) body.get("lon")).doubleValue());
                reverseGeocodeService.addAddressToResponse(response);
            } else {
                response.setError("No coordinates returned");
            }
        } catch (RestClientException e) {
            response.setError("Error calling OpenCellID: " + e.getMessage());
        }

        return response;
    }

    @Override
    public String getProviderName() {
        return "OpenCellID";
    }
}
