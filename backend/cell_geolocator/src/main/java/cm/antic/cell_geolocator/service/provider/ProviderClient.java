package cm.antic.cell_geolocator.service.provider;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;

public interface ProviderClient {

    GeolocationResponse resolve(GeolocationRequest request);
    String getProviderName();

}
