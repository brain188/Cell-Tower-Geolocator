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
                            if (data != null && data.containsKey("address")) {

                                Map<String, Object> addressMap = (Map<String, Object>) data.get("address");

                                String providerUsed = response.getProviderUsed();
                                String siteName = null;

                                if (providerUsed != null && providerUsed.contains(":")) {
                                    siteName = providerUsed.split(":", 2)[1].trim();
                                }

                                // ✅ Extract only required fields
                                String country = addressMap.getOrDefault("country", "").toString();
                                String city = addressMap.getOrDefault("city",
                                        addressMap.getOrDefault("town",
                                        addressMap.getOrDefault("village", ""))).toString();
                                String state = addressMap.getOrDefault("state",
                                        addressMap.getOrDefault("region",
                                        addressMap.getOrDefault("state_district", ""))).toString();

                                // ✅ Build CLEAN address (NO street, NO POI)
                                StringBuilder cleanAddress = new StringBuilder();

                                if (providerUsed != null && providerUsed.startsWith("LOCAL_DB") &&
                                        siteName != null && !siteName.isBlank()) {
                                    cleanAddress.append(siteName).append(", ");
                                }

                                cleanAddress
                                        .append(city).append(", ")
                                        .append(state).append(", ")
                                        .append(country);

                                response.setAddress(cleanAddress.toString());
                                System.out.println("Clean address: " + response.getAddress());

                                // ✅ AddressDetail (same logic)
                                AddressDetail detail = new AddressDetail();
                                detail.setCountry(country);
                                detail.setCountryCode(addressMap.getOrDefault("country_code", "").toString());
                                detail.setCityOrTown(city);
                                detail.setStateOrRegion(state);
                                detail.setPostalCode(addressMap.getOrDefault("postcode", "").toString());

                                // ✅ Street rule
                                if (providerUsed != null && providerUsed.startsWith("LOCAL_DB") &&
                                        siteName != null && !siteName.isBlank()) {
                                    detail.setStreet(siteName);
                                } else {
                                    detail.setStreet("");
                                }

                                response.setAddressDetail(detail);

                            } else {
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

                        String providerUsed = response.getProviderUsed();
                        String siteName = null;

                        if (providerUsed != null && providerUsed.contains(":")) {
                            siteName = providerUsed.split(":", 2)[1].trim();
                        }

                        String country = addressMap.getOrDefault("country", "");
                        String city = addressMap.getOrDefault("city",
                                addressMap.getOrDefault("town",
                                addressMap.getOrDefault("village", "")));
                        String state = addressMap.getOrDefault("state",
                                addressMap.getOrDefault("region",
                                addressMap.getOrDefault("state_district", "")));

                        // ✅ Build CLEAN address
                        StringBuilder cleanAddress = new StringBuilder();

                        if (providerUsed != null && providerUsed.startsWith("LOCAL_DB") &&
                                siteName != null && !siteName.isBlank()) {
                            cleanAddress.append(siteName).append(", ");
                        }

                        cleanAddress
                                .append(city).append(", ")
                                .append(state).append(", ")
                                .append(country);

                        response.setAddress(cleanAddress.toString());
                        System.out.println("Clean address: " + response.getAddress());

                        AddressDetail detail = new AddressDetail();
                        detail.setCountry(country);
                        detail.setCountryCode(addressMap.getOrDefault("country_code", ""));
                        detail.setCityOrTown(city);
                        detail.setStateOrRegion(state);
                        detail.setPostalCode(addressMap.getOrDefault("postcode", ""));

                        // ✅ Street rule
                        if (providerUsed != null && providerUsed.startsWith("LOCAL_DB") &&
                                siteName != null && !siteName.isBlank()) {
                            detail.setStreet(siteName);
                        } else {
                            detail.setStreet("");
                        }

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
