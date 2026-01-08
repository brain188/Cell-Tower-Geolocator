package cm.antic.cell_geolocator.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import cm.antic.cell_geolocator.entity.User;
import cm.antic.cell_geolocator.entity.RequestLog;
import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.repository.RequestLogRepository;
import cm.antic.cell_geolocator.repository.UserRepository;
import cm.antic.cell_geolocator.service.provider.ProviderClient;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class GeolocationService {

    @Autowired
    private PriorityService priorityService;

    @Autowired
    private final List<ProviderClient> providerClients;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private ReverseGeocodeService reverseGeocodeService;

    @Autowired
    private UserRepository userRepository;

    public GeolocationService(List<ProviderClient> providerClients) {
        this.providerClients = providerClients;
    }

    private void requireVerification() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Not authenticated");
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isVerified()) {
            throw new RuntimeException("Account Not Verified - Please Complete Company Verification");
        }
    }

    /**
     * Asynchronous resolve (main logic)
     */
    @Cacheable(
        value = "geolocation",
        key = "#request.mcc + '_' + #request.mnc + '_' + #request.lac + '_' + #request.cellId"
    )
    @Retry(name = "providerRetry")
    public CompletableFuture<GeolocationResponse> resolveAsync(GeolocationRequest request) {
        requireVerification();

        List<String> priorities = priorityService.getProviderPriorities();

        List<CompletableFuture<ProviderResult>> futures = priorities.stream()
            .map(provider -> {
                ProviderClient client = getClientByName(provider);
                if (client == null) {
                    return CompletableFuture.completedFuture(
                        new ProviderResult(provider, null, "Provider not found")
                    );
                }
                return client.resolveAsync(request)
                        .thenApply(resp -> new ProviderResult(provider, resp, null))
                        .exceptionally(e -> new ProviderResult(provider, null, e.getMessage()));
            })
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream().map(CompletableFuture::join).collect(Collectors.toList()))
            .thenApply(results -> {
                GeolocationResponse finalResponse = new GeolocationResponse();
                Map<String, Object> raw = new HashMap<>();

                results.forEach(r -> {
                    if (r.response != null) raw.put(r.provider, r.response);
                    else raw.put(r.provider, Map.of("error", r.error));

                    if (finalResponse.getLatitude() == null &&
                        r.response != null &&
                        r.response.getLatitude() != null) {

                        finalResponse.setLatitude(r.response.getLatitude());
                        finalResponse.setLongitude(r.response.getLongitude());
                        finalResponse.setAccuracy(r.response.getAccuracy());
                        finalResponse.setProviderUsed(r.provider);
                    }
                });

                finalResponse.setRawResponses(raw);

                if (finalResponse.getLatitude() != null) {
                    reverseGeocodeService.addAddressToResponseAsync(finalResponse)
                        .exceptionally(ex -> null);
                }

                saveLogAsync(request, finalResponse);
                return finalResponse;
            });
    }

    /**
     * Synchronous wrapper for controller usage
     */
    public GeolocationResponse resolve(GeolocationRequest request) {
        requireVerification();

        try {
            return resolveAsync(request).join();
        } catch (Exception e) {
            System.err.println("Sync resolve failed: " + e.getMessage());
            return null;
        }
    }

    private static class ProviderResult {
        String provider;
        GeolocationResponse response;
        String error;
        ProviderResult(String p, GeolocationResponse r, String e) {
            provider = p; response = r; error = e;
        }
    }

    private ProviderClient getClientByName(String name) {
        return providerClients.stream()
                .filter(c -> c.getProviderName().equals(name))
                .findFirst().orElse(null);
    }

    private void saveLogAsync(GeolocationRequest request, GeolocationResponse resp) {
        CompletableFuture.runAsync(() -> {
            RequestLog log = new RequestLog();
            log.setMcc(request.getMcc());
            log.setMnc(request.getMnc());
            log.setLac(request.getLac());
            log.setCellId(request.getCellId());
            log.setAccuracy(resp.getAccuracy());
            log.setProviderUsed(resp.getProviderUsed());
            log.setLatitude(resp.getLatitude());
            log.setLongitude(resp.getLongitude());
            log.setTimestamp(LocalDateTime.now());
            requestLogRepository.save(log);
        });
    }
}
