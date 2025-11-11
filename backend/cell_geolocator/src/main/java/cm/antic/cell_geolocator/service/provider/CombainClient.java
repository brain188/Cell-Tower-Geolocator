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
public class CombainClient implements ProviderClient {

    @Value("${combain.api.key}")
    private String apiKey;

    @Value("${combain.api.url:https://apiv2.combain.com}")
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
            err.setError("Combain resolve failed: " + e.getMessage());
            return err;
        }
    }

    @Override
    public CompletableFuture<GeolocationResponse> resolveAsync(GeolocationRequest request) {
        GeolocationResponse base = new GeolocationResponse();
        base.setProviderUsed(getProviderName());

        // Build request body
        Map<String, Object> cellTower = new HashMap<>();
        cellTower.put("mobileCountryCode", tryParseNumber(request.getMcc()));
        cellTower.put("mobileNetworkCode", tryParseNumber(request.getMnc()));
        cellTower.put("locationAreaCode", tryParseNumber(request.getLac()));
        cellTower.put("cellId", tryParseNumber(request.getCellId()));

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("cellTowers", List.of(cellTower));

        // POST to Combain with API key as query param (per their docs)
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("") // base path is apiUrl value
                        .queryParam("key", apiKey)
                        .build())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(body -> {
                    // Combain's successful response often has "location": {"lat":.., "lng":..}
                    Object locObj = body.get("location");
                    if (locObj instanceof Map<?, ?> locMap) {
                        Number lat = (Number) locMap.get("lat");
                        Number lng = (Number) locMap.get("lng");
                        if (lat != null && lng != null) {
                            GeolocationResponse resp = new GeolocationResponse();
                            resp.setProviderUsed(getProviderName());
                            resp.setLatitude(lat.doubleValue());
                            resp.setLongitude(lng.doubleValue());

                            Object accObj = locMap.get("accuracy");
                            if (accObj instanceof Number) {
                                resp.setAccuracy(((Number) accObj).doubleValue());
                            }
                            // Do reverse geocoding asynchronously and then complete with resp
                            return Mono.fromFuture(reverseGeocodeService.addAddressToResponseAsync(resp))
                                       .then(Mono.just(resp));
                        }
                    }
                    // fallback: may contain lat/lon at top-level; check
                    Object latObj = body.get("lat");
                    Object lonObj = body.get("lon");
                    if (latObj instanceof Number && lonObj instanceof Number) {
                        GeolocationResponse resp = new GeolocationResponse();
                        resp.setProviderUsed(getProviderName());
                        resp.setLatitude(((Number) latObj).doubleValue());
                        resp.setLongitude(((Number) lonObj).doubleValue());

                        Object accObj = body.get("accuracy");
                        if (accObj instanceof Number) {
                            resp.setAccuracy(((Number) accObj).doubleValue());
                        }
                        return Mono.fromFuture(reverseGeocodeService.addAddressToResponseAsync(resp))
                                   .then(Mono.just(resp));
                    }

                    GeolocationResponse err = base;
                    err.setError("No coordinates returned from Combain");
                    return Mono.just(err);
                })
                .onErrorResume(ex -> {
                    GeolocationResponse err = base;
                    err.setError("Combain error: " + ex.getMessage());
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
        return "Combain";
    }

}
