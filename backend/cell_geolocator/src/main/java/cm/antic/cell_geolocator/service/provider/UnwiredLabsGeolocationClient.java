package cm.antic.cell_geolocator.service.provider;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.service.ReverseGeocodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class UnwiredLabsGeolocationClient implements ProviderClient {

    @Value("${unwired_labs.api.key}")
    private String apiKey;

    @Value("${unwired_labs.api.url:https://us1.unwiredlabs.com/v2/process.php}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

     @Autowired
    private ReverseGeocodeService reverseGeocodeService;

    @Override
    @SuppressWarnings("unchecked")
    public GeolocationResponse resolve(GeolocationRequest request) {
        String url = apiUrl;
        Map<String, Object> body = new HashMap<>();
        body.put("token", apiKey);
        Map<String, Object> tower = new HashMap<>();
        tower.put("mcc", request.getMcc());
        tower.put("mnc", request.getMnc());
        tower.put("lac", request.getLac());
        tower.put("cellid", request.getCellId());
        body.put("tower", new Object[]{tower});

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map<String, Object> responseMap = (Map<String, Object>) restTemplate.exchange(url, HttpMethod.POST, entity, Map.class).getBody();
        GeolocationResponse response = new GeolocationResponse();
        if (responseMap != null && responseMap.containsKey("result")) {
            Map<String, Object> result = (Map<String, Object>) responseMap.get("result");
            response.setLatitude((Double) result.get("lat"));
            response.setLongitude((Double) result.get("lon"));

            // address details using reverse geocoding
            reverseGeocodeService.addAddressToResponse(response);
        }
        return response;
    }

    @Override
    public String getProviderName() {
        return "UnwiredLabs";
    }

}
