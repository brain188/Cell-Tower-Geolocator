package cm.antic.cell_geolocator.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;

import cm.antic.cell_geolocator.entity.User;
import cm.antic.cell_geolocator.entity.RequestLog;
import cm.antic.cell_geolocator.exception.AccountNotVerifiedException;
import cm.antic.cell_geolocator.exception.UnauthenticatedException;
import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.repository.RequestLogRepository;
import cm.antic.cell_geolocator.repository.UserRepository;
import cm.antic.cell_geolocator.service.provider.ProviderClient;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class GeolocationService {

    private static final Logger log = LoggerFactory.getLogger(GeolocationService.class);

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
            log.warn("Unauthenticated access attempt to geolocation service");
            throw new UnauthenticatedException("Not authenticated");
        }

        String username = auth.getName();
        log.debug("Verifying account for user '{}'", username);

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                log.error("Authenticated user '{}' not found in database", username);
                return new UnauthenticatedException("User not found");
            });

        if (!user.isVerified()) {
            log.warn("Unverified account '{}' attempted geolocation access", username);
            throw new AccountNotVerifiedException(
                "Account not verified - please complete company verification"
            );
        }

        log.debug("User '{}' successfully verified", username);
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

        log.info(
            "Starting async geolocation resolve [MCC={}, MNC={}, LAC={}, CELL={}]",
            request.getMcc(), request.getMnc(), request.getLac(), request.getCellId()
        );

        requireVerification();

        List<String> priorities = priorityService.getProviderPriorities();
        log.debug("Provider priority order: {}", priorities);

        List<CompletableFuture<ProviderResult>> futures = priorities.stream()
            .map(provider -> {
                ProviderClient client = getClientByName(provider);
                if (client == null) {
                    log.warn("Provider '{}' not found in configured clients", provider);
                    return CompletableFuture.completedFuture(
                        new ProviderResult(provider, null, "Provider not found")
                    );
                }

                return client.resolveAsync(request)
                    .thenApply(resp -> {
                        log.debug("Provider '{}' returned response", provider);
                        return new ProviderResult(provider, resp, null);
                    })
                    .exceptionally(e -> {
                        log.error("Provider '{}' failed: {}", provider, e.getMessage());
                        return new ProviderResult(provider, null, e.getMessage());
                    });
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
                    try {
                    reverseGeocodeService.addAddressToResponseAsync(finalResponse)
                        .get(); // BLOCKS until address is set
                    } catch (Exception e) {
                            log.warn("Reverse geocoding failed", e);
                            // return null;
                    }
                }

                saveLogAsync(request, finalResponse);
                log.info("Geolocation resolve completed successfully");
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
            log.error("Synchronous geolocation resolve failed", e);
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
            try {
                RequestLog logEntry = new RequestLog();
                logEntry.setMcc(request.getMcc());
                logEntry.setMnc(request.getMnc());
                logEntry.setLac(request.getLac());
                logEntry.setCellId(request.getCellId());
                logEntry.setAccuracy(resp.getAccuracy());
                logEntry.setProviderUsed(resp.getProviderUsed());
                logEntry.setLatitude(resp.getLatitude());
                logEntry.setLongitude(resp.getLongitude());
                logEntry.setTimestamp(LocalDateTime.now());

                requestLogRepository.save(logEntry);
                log.debug("Request log saved successfully");

            } catch (Exception e) {
                log.error("Failed to persist request log", e);
            }
        });
    }
}
