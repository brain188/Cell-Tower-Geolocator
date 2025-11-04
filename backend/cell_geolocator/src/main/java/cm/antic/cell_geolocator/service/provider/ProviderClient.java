package cm.antic.cell_geolocator.service.provider;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import java.util.concurrent.CompletableFuture;

public interface ProviderClient {

    GeolocationResponse resolve(GeolocationRequest request);
    CompletableFuture<GeolocationResponse> resolveAsync(GeolocationRequest request);
    String getProviderName();
}
