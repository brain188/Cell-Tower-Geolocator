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
import org.springframework.web.util.InvalidUrlException;
import org.springframework.web.util.UriComponentsBuilder;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.service.ReverseGeocodeService;

@Component
public class CombainClient implements ProviderClient {

    @Value("${combain.api.key}")
    private String apiKey;

    @Value("${combain.api.url:https://apiv2.combain.com}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ReverseGeocodeService reverseGeocodeService;

    @Override
    public GeolocationResponse resolve(GeolocationRequest request) {
        GeolocationResponse response = new GeolocationResponse();
        response.setProviderUsed(getProviderName());

        try {
            Map<String, Object> cellTower = new HashMap<>();
            cellTower.put("mobileCountryCode", request.getMcc());
            cellTower.put("mobileNetworkCode", request.getMnc());
            cellTower.put("locationAreaCode", request.getLac());
            cellTower.put("cellId", request.getCellId());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("cellTowers", List.of(cellTower));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String urlWithApiKey = UriComponentsBuilder.fromUriString(apiUrl)
                    .queryParam("key", apiKey)
                    .toUriString();

            ResponseEntity<Map<String, Object>> responseEntity = restTemplate.exchange(
                    urlWithApiKey,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = responseEntity.getBody();

            if (body != null && body.containsKey("location")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> location = (Map<String, Object>) body.get("location");
                response.setLatitude(((Number) location.get("lat")).doubleValue());
                response.setLongitude(((Number) location.get("lng")).doubleValue());
                reverseGeocodeService.addAddressToResponse(response);
            } else {
                response.setError("No coordinates returned");
            }
        } catch (RestClientException | InvalidUrlException e) {
            response.setError("Error calling Combain: " + e.getMessage());
        }

        return response;
    }

    @Override
    public String getProviderName() {
        return "Combain";
    }
    
}
