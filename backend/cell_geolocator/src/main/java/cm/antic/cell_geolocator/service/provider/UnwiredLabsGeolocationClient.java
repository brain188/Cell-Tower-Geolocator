package cm.antic.cell_geolocator.service.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.service.ReverseGeocodeService;

@Component
public class UnwiredLabsGeolocationClient implements ProviderClient {

    @Value("${unwired_labs.api.key}")
    private String apiKey;

    @Value("${unwired_labs.api.url:https://us1.unwiredlabs.com/v2/process}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ReverseGeocodeService reverseGeocodeService;

    @Override
    public GeolocationResponse resolve(GeolocationRequest request) {
        GeolocationResponse response = new GeolocationResponse();
        response.setProviderUsed(getProviderName());

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("token", apiKey);

            Map<String, Object> tower = new HashMap<>();
            tower.put("mcc", request.getMcc());
            tower.put("mnc", request.getMnc());
            tower.put("lac", request.getLac());
            tower.put("cid", request.getCellId()); 

            body.put("cells", List.of(tower));
        
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, new HttpHeaders() {{
                setContentType(MediaType.APPLICATION_JSON);
            }});

            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    apiUrl,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<>() {}
            );

            Map<String, Object> result = responseEntity.getBody();

            if (result != null && result.containsKey("lat") && result.containsKey("lon")) {
                response.setLatitude(((Number) result.get("lat")).doubleValue());
                response.setLongitude(((Number) result.get("lon")).doubleValue());
                reverseGeocodeService.addAddressToResponse(response);
            } else {
                response.setError("No coordinates returned");
            }
        } catch (RestClientException e) {
            response.setError("Error calling UnwiredLabs: " + e.getMessage());
        }

        return response;
    }

    @Override
    public String getProviderName() {
        return "UnwiredLabs";
    }
    
}
