package cm.antic.cell_geolocator.service;

import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.model.GeolocationResponse.AddressDetail;
import cm.antic.cell_geolocator.model.NominatimResponse;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ReverseGeocodeService {

    @Autowired
    private WebClient webClient;

    public CompletableFuture<Void> addAddressToResponseAsync(GeolocationResponse response) {

        if (response.getLatitude() == null || response.getLongitude() == null) {
            return CompletableFuture.completedFuture(null);
        }

        String url = String.format(
                "https://nominatim.openstreetmap.org/reverse?format=json&lat=%s&lon=%s&addressdetails=1",
                response.getLatitude(), response.getLongitude()
        );

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(NominatimResponse.class)
                .toFuture()
                .thenAccept(data -> {
                    if (data != null && data.getAddress() != null) {
                        Map<String, String> addressMap = data.getAddress();
                        response.setAddress(data.getDisplayName());

                        AddressDetail detail = new AddressDetail();
                        detail.setCountry(addressMap.getOrDefault("country", ""));
                        detail.setCountryCode(addressMap.getOrDefault("country_code", ""));
                        detail.setCityOrTown(
                                addressMap.getOrDefault("city",
                                addressMap.getOrDefault("town",
                                addressMap.getOrDefault("village", "")))
                        );
                        detail.setStateOrRegion(
                                addressMap.getOrDefault("state",
                                addressMap.getOrDefault("region",
                                addressMap.getOrDefault("state_district", "")))
                        );
                        detail.setPostalCode(addressMap.getOrDefault("postcode", ""));
                        detail.setStreet(addressMap.getOrDefault("road", ""));

                        response.setAddressDetail(detail);
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("Reverse geocoding async failed: " + ex.getMessage());
                    return null;
                });

    }

    public void addAddressToResponse(GeolocationResponse response) {
                    try {
                        addAddressToResponseAsync(response).join();
                    } 
                    catch (Exception e) {
                        System.err.println("Reverse geocoding sync failed: " + e.getMessage());
                    }
                }
}