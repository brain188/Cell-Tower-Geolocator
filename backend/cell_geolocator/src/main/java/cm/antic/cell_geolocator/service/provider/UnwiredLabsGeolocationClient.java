package cm.antic.cell_geolocator.service.provider;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.service.ReverseGeocodeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class UnwiredLabsGeolocationClient implements ProviderClient {

    @Value("${unwired_labs.api.key}")
    private String apiKey;

    @Value("${unwired_labs.api.url:https://us1.unwiredlabs.com/v2/process}")
    private String apiUrl;

    @Autowired
    private WebClient webClient;

    @Autowired
    private ReverseGeocodeService reverseGeocodeService;

    @Override
    public GeolocationResponse resolve(GeolocationRequest request) {
        try {
            return resolveAsync(request).join();
        } catch (Exception e) {
            GeolocationResponse err = new GeolocationResponse();
            err.setProviderUsed(getProviderName());
            err.setError("UnwiredLabs resolve failed: " + e.getMessage());
            return err;
        }
    }

    @Override
    public CompletableFuture<GeolocationResponse> resolveAsync(GeolocationRequest request) {
        GeolocationResponse base = new GeolocationResponse();
        base.setProviderUsed(getProviderName());

        Map<String, Object> tower = new HashMap<>();
        tower.put("mcc", tryParseNumber(request.getMcc()));
        tower.put("mnc", tryParseNumber(request.getMnc()));
        tower.put("lac", tryParseNumber(request.getLac()));
        tower.put("cid", tryParseNumber(request.getCellId())); // Unwired uses cid or cellid depending on doc

        Map<String, Object> body = new HashMap<>();
        body.put("token", apiKey);
        body.put("cells", List.of(tower));
        body.put("address", 1); // request address in response if supported

        return webClient.post()
                .uri(apiUrl)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(result -> {
                    // Unwired typically returns top-level "lat" and "lon" for the whole request
                    Object latObj = result.get("lat");
                    Object lonObj = result.get("lon");
                    Object accObj = result.get("accuracy");

                    if (latObj instanceof Number && lonObj instanceof Number && accObj instanceof Number) {
                        GeolocationResponse resp = new GeolocationResponse();
                        resp.setProviderUsed(getProviderName());
                        resp.setLatitude(((Number) latObj).doubleValue());
                        resp.setLongitude(((Number) lonObj).doubleValue());
                        resp.setAccuracy(((Number) accObj).doubleValue());
                        
                        return Mono.fromFuture(reverseGeocodeService.addAddressToResponseAsync(resp))
                                   .then(Mono.just(resp));
                    }

                    // Try nested "result" object
                    Object resultObj = result.get("result");
                    if (resultObj instanceof Map<?, ?> resMap) {
                        Object rlat = resMap.get("lat");
                        Object rlon = resMap.get("lon");
                        Object racc = resMap.get("accuracy");

                        if (rlat instanceof Number && rlon instanceof Number && accObj instanceof Number) {
                            GeolocationResponse resp = new GeolocationResponse();
                            resp.setProviderUsed(getProviderName());
                            resp.setLatitude(((Number) rlat).doubleValue());
                            resp.setLongitude(((Number) rlon).doubleValue());
                            resp.setAccuracy(((Number) racc).doubleValue());
                            
                            return Mono.fromFuture(reverseGeocodeService.addAddressToResponseAsync(resp))
                                       .then(Mono.just(resp));
                        }
                    }

                    GeolocationResponse err = base;
                    err.setError("No coordinates returned from UnwiredLabs");
                    return Mono.just(err);
                })
                .onErrorResume(ex -> {
                    GeolocationResponse err = base;
                    err.setError("UnwiredLabs error: " + ex.getMessage());
                    return Mono.just(err);
                })
                .toFuture();
    }

    private Number tryParseNumber(String s) {
        if (s == null) return null;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                return null; // unable to parse as a number
            }
        }
    }

    @Override
    public String getProviderName() {
        return "UnwiredLabs";
    }

}
