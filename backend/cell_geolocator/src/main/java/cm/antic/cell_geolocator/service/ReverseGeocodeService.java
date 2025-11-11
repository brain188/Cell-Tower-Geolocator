package cm.antic.cell_geolocator.service;

import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.model.GeolocationResponse.AddressDetail;
import cm.antic.cell_geolocator.model.NominatimResponse;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class ReverseGeocodeService {

    @Autowired
    private WebClient webClient;

    @Value("${locationiq.api.key:}")
    private String locationIqApiKey;  

    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> addAddressToResponseAsync(GeolocationResponse response) {

        if (response.getLatitude() == null || response.getLongitude() == null) {
            return CompletableFuture.completedFuture(null);
        }

        // Try LocationIQ first if API key is available
        if (locationIqApiKey != null && !locationIqApiKey.isBlank()) {
            String locationIqUrl = String.format(
                    "https://us1.locationiq.com/v1/reverse?key=%s&lat=%s&lon=%s&format=json&addressdetails=1",
                    locationIqApiKey, response.getLatitude(), response.getLongitude()
            );

            return webClient.get()
                    .uri(locationIqUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .toFuture()
                    .thenAccept(data -> {
                        try {
                            if (data != null && data.containsKey("display_name")) {
                                response.setAddress(data.get("display_name").toString());

                                Object addrObj = data.get("address");
                                if (addrObj instanceof Map) {
                                    Map<String, Object> addressMap = (Map<String, Object>) addrObj;
                                    AddressDetail detail = new AddressDetail();

                                    detail.setCountry(addressMap.getOrDefault("country", "").toString());
                                    detail.setCountryCode(addressMap.getOrDefault("country_code", "").toString());
                                    detail.setCityOrTown(addressMap.getOrDefault("city",
                                            addressMap.getOrDefault("town",
                                            addressMap.getOrDefault("village", ""))).toString());
                                    detail.setStateOrRegion(addressMap.getOrDefault("state",
                                            addressMap.getOrDefault("region",
                                            addressMap.getOrDefault("state_district", ""))).toString());
                                    detail.setPostalCode(addressMap.getOrDefault("postcode", "").toString());
                                    detail.setStreet(addressMap.getOrDefault("road", "").toString());

                                    response.setAddressDetail(detail);
                                }
                            } else {
                                // Fallback to Nominatim if LocationIQ gives no valid response
                                fetchFromNominatim(response);
                            }
                        } catch (Exception ex) {
                            System.err.println("LocationIQ parse failed: " + ex.getMessage());
                            fetchFromNominatim(response);
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("LocationIQ reverse geocoding failed: " + ex.getMessage());
                        fetchFromNominatim(response);
                        return null;
                    });
        }

        // Fallback directly to Nominatim if no API key configured
        return fetchFromNominatim(response);
    }

    private CompletableFuture<Void> fetchFromNominatim(GeolocationResponse response) {
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
                    System.err.println("Nominatim reverse geocoding failed: " + ex.getMessage());
                    return null;
                });
    }

    public void addAddressToResponse(GeolocationResponse response) {
        try {
            addAddressToResponseAsync(response).join();
        } catch (Exception e) {
            System.err.println("Reverse geocoding sync failed: " + e.getMessage());
        }
    }
}
