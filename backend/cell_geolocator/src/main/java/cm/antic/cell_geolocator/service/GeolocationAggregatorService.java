package cm.antic.cell_geolocator.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import cm.antic.cell_geolocator.entity.RequestLog;
import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.model.PriorityGeolocationResult;
import cm.antic.cell_geolocator.repository.RequestLogRepository;
import cm.antic.cell_geolocator.service.provider.ProviderClient;

/**
 * Service that aggregates results from multiple geolocation providers
 * (MyDatabase(cellGeolocator), OpenCellID, UnwiredLabs, Combain)
 */
@Service
public class GeolocationAggregatorService {

    private static final Logger log =
            LoggerFactory.getLogger(GeolocationAggregatorService.class);

    private final CellTowerLocalService cellTowerLocalService;
    private final List<ProviderClient> providers;
    private final PriorityService priorityService;
    private final RequestLogRepository requestLogRepository;

    public GeolocationAggregatorService(
            List<ProviderClient> providers,
            PriorityService priorityService,
            RequestLogRepository requestLogRepository,
            CellTowerLocalService cellTowerLocalService) {

        this.providers = providers;
        this.priorityService = priorityService;
        this.requestLogRepository = requestLogRepository;
        this.cellTowerLocalService = cellTowerLocalService;
    }

    /**
     * Resolves a geolocation request using local DB first, then external providers if needed.
     */
    public CompletableFuture<PriorityGeolocationResult> resolveWithPriorityAsync(
            GeolocationRequest request) {

        log.info(
            "Starting priority geolocation [MCC={}, MNC={}, LAC={}, CELL={}]",
            request.getMcc(), request.getMnc(),
            request.getLac(), request.getCellId()
        );

        return CompletableFuture.supplyAsync(() -> {

            //  Local DB check
            GeolocationResponse localResp = cellTowerLocalService.findLocalTower(
                    request.getMcc(),
                    request.getMnc(),
                    request.getLac(),
                    request.getCellId()
            );

            if (localResp != null && localResp.getLatitude() != null) {
                log.info("Local DB hit — skipping external providers");

                PriorityGeolocationResult result = new PriorityGeolocationResult();
                result.setChosen(localResp);
                result.setAllResponses(Map.of("LOCAL_DB", localResp));
                result.setDistances(Collections.emptyMap());
                result.setShortestPair("LOCAL_DB_ONLY");

                saveLogAsync(request, localResp);
                return result;
            }

            log.info("Local DB miss — querying external providers");

            //  Query providers asynchronously
            Map<String, CompletableFuture<GeolocationResponse>> futures = new HashMap<>();

            for (ProviderClient provider : providers) {
                log.debug("Dispatching request to provider '{}'", provider.getProviderName());

                futures.put(
                    provider.getProviderName(),
                    CompletableFuture.supplyAsync(() -> provider.resolve(request))
                        .exceptionally(e -> {
                            log.error(
                                "Provider '{}' failed: {}",
                                provider.getProviderName(),
                                e.getMessage()
                            );
                            GeolocationResponse err = new GeolocationResponse();
                            err.setProviderUsed(provider.getProviderName());
                            err.setAddress("Error: " + e.getMessage());
                            return err;
                        })
                );
            }

            CompletableFuture.allOf(
                futures.values().toArray(new CompletableFuture[0])
            ).join();

            Map<String, GeolocationResponse> results = new HashMap<>();
            futures.forEach((k, v) -> results.put(k, v.join()));

            GeolocationResponse a = results.get("OpenCellID");
            GeolocationResponse b = results.get("UnwiredLabs");
            GeolocationResponse c = results.get("Combain");

            double ab = distanceIfValid(a, b);
            double ac = distanceIfValid(a, c);
            double bc = distanceIfValid(b, c);

            log.debug(
                "Distance matrix computed — AB={}, AC={}, BC={}",
                ab, ac, bc
            );

            double min = Math.min(ab, Math.min(ac, bc));
            GeolocationResponse chosen;
            String shortestPair;

            if (min == ab) {
                chosen = pickByPriorityWithFallback(a, b, c);
                shortestPair = "AB";
            } else if (min == ac) {
                chosen = pickByPriorityWithFallback(a, c, b);
                shortestPair = "AC";
            } else {
                chosen = pickByPriorityWithFallback(b, c, a);
                shortestPair = "BC";
            }

            if (chosen == null || chosen.getLatitude() == null) {
                log.warn("No valid provider result — returning fallback response");
                GeolocationResponse none = new GeolocationResponse();
                none.setProviderUsed("None");
                none.setAddress("No provider matched");
                chosen = none;
            }

            Map<String, Double> distances = new HashMap<>();
            distances.put("AB", ab);
            distances.put("AC", ac);
            distances.put("BC", bc);

            PriorityGeolocationResult result = new PriorityGeolocationResult();
            result.setChosen(chosen);
            result.setAllResponses(results);
            result.setDistances(distances);
            result.setShortestPair(shortestPair);

            saveLogAsync(request, chosen);

            log.info(
                "Priority geolocation completed — providerUsed={}, shortestPair={}",
                chosen.getProviderUsed(),
                shortestPair
            );

            return result;
        });
    }

    private GeolocationResponse pickByPriorityWithFallback(
            GeolocationResponse r1,
            GeolocationResponse r2,
            GeolocationResponse fallback) {

        GeolocationResponse preferred = pickByPriority(r1, r2);
        if (isValid(preferred)) return preferred;

        GeolocationResponse alternative = (preferred == r1) ? r2 : r1;
        if (isValid(alternative)) return alternative;

        if (isValid(fallback)) return fallback;
        return null;
    }

    private boolean isValid(GeolocationResponse r) {
        return r != null && r.getLatitude() != null && r.getLongitude() != null;
    }

    private double distanceIfValid(GeolocationResponse r1, GeolocationResponse r2) {
        if (!isValid(r1) || !isValid(r2)) return Double.MAX_VALUE;
        return haversine(
            r1.getLatitude(), r1.getLongitude(),
            r2.getLatitude(), r2.getLongitude()
        );
    }

    /**
     * Haversine formula (km)
     */
    private double haversine(
            double lat1, double lon1,
            double lat2, double lon2) {

        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a =
            Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) *
            Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);

        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private GeolocationResponse pickByPriority(
            GeolocationResponse r1,
            GeolocationResponse r2) {

        List<String> priorities = priorityService.getProviderPriorities();
        int idx1 = priorities.indexOf(r1.getProviderUsed());
        int idx2 = priorities.indexOf(r2.getProviderUsed());

        if (idx1 == -1 && idx2 == -1) return r1;
        if (idx1 == -1) return r2;
        if (idx2 == -1) return r1;
        return (idx1 < idx2) ? r1 : r2;
    }

    private void saveLogAsync(
            GeolocationRequest req,
            GeolocationResponse resp) {

        CompletableFuture.runAsync(() -> {
            try {
                RequestLog logEntry = new RequestLog();
                logEntry.setMcc(req.getMcc());
                logEntry.setMnc(req.getMnc());
                logEntry.setLac(req.getLac());
                logEntry.setCellId(req.getCellId());
                logEntry.setAccuracy(resp.getAccuracy());
                logEntry.setProviderUsed(resp.getProviderUsed());
                logEntry.setLatitude(resp.getLatitude());
                logEntry.setLongitude(resp.getLongitude());
                logEntry.setTimestamp(LocalDateTime.now());

                requestLogRepository.save(logEntry);
                log.debug("Request log saved successfully");

            } catch (Exception e) {
                log.error("Failed to save request log", e);
            }
        });
    }
}
