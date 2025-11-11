package cm.antic.cell_geolocator.service.provider;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.service.ReverseGeocodeService;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;


@Component
public class OpenCellIdClient implements ProviderClient {

    @Value("${opencellid.api.key}")
    private String apiKey;

    @Value("${opencellid.api.url:https://opencellid.org/cell/get}")
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
            err.setError("OpenCellID resolve failed: " + e.getMessage());
            return err;
        }
    }

    @Override
    public CompletableFuture<GeolocationResponse> resolveAsync(GeolocationRequest request) {

        String url = String.format("%s?key=%s&mcc=%s&mnc=%s&lac=%s&cellid=%s&format=json",
                apiUrl, apiKey, request.getMcc(), request.getMnc(), request.getLac(), request.getCellId());

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .flatMap(body -> {
                    GeolocationResponse resp = new GeolocationResponse();
                    resp.setProviderUsed(getProviderName());

                    if (body.containsKey("lat") && body.containsKey("lon")) {
                        resp.setLatitude(((Number) body.get("lat")).doubleValue());
                        resp.setLongitude(((Number) body.get("lon")).doubleValue());

                        Object rangeObj = body.get("range");
                        if (rangeObj instanceof Number) {
                            resp.setAccuracy(((Number) rangeObj).doubleValue());
                        }

                        // async reverse geocode
                        return Mono.fromFuture(reverseGeocodeService.addAddressToResponseAsync(resp))
                                   .then(Mono.just(resp));
                    }

                    resp.setError("No coordinates returned");
                    return Mono.just(resp);
                })
                .onErrorResume(e -> {
                    GeolocationResponse errorResp = new GeolocationResponse();
                    errorResp.setProviderUsed(getProviderName());
                    errorResp.setError("OpenCellID API error: " + e.getMessage());
                    return Mono.just(errorResp);
                })
                .toFuture();
    }

    @Override
    public String getProviderName() {
        return "OpenCellID";
    }
}
