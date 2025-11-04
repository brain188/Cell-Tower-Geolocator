package cm.antic.cell_geolocator.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Service;

import cm.antic.cell_geolocator.entity.RequestLog;
import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.model.PriorityGeolocationResult;
import cm.antic.cell_geolocator.repository.RequestLogRepository;
import cm.antic.cell_geolocator.service.provider.ProviderClient;

/**
 * Service that aggregates results from multiple geolocation providers
 * (MyDatabase(cellGeolocator),OpenCellID, UnwiredLabs, Combain) 
 * The logic is:
 *   Try localDB(cellGeolocator) first
 *   If not found → use external providers + distance comparison
 */

@Service
public class GeolocationAggregatorService {

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
    public CompletableFuture<PriorityGeolocationResult> resolveWithPriorityAsync(GeolocationRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            // checks Local DB FIRST
            GeolocationResponse localResp = cellTowerLocalService.findLocalTower(
                    request.getMcc(), request.getMnc(), request.getLac(), request.getCellId());

            if (localResp != null && localResp.getLatitude() != null) {
                PriorityGeolocationResult result = new PriorityGeolocationResult();
                result.setChosen(localResp);
                result.setAllResponses(Map.of("LOCAL_DB", localResp));
                result.setDistances(Collections.emptyMap());
                result.setShortestPair("LOCAL_DB_ONLY");
                saveLogAsync(request, localResp);
                return result;
            }

            /** 
             * Otherwise, query external providers asynchronously
             * and selects the best one based on shortest distance and priority.
             * Adds fallback logic if chosen provider has no valid result.
            */
            Map<String, CompletableFuture<GeolocationResponse>> futures = new HashMap<>();

            for (ProviderClient provider : providers) {
                futures.put(provider.getProviderName(),
                        CompletableFuture.supplyAsync(() -> provider.resolve(request))
                                .exceptionally(e -> {
                                    GeolocationResponse err = new GeolocationResponse();
                                    err.setProviderUsed(provider.getProviderName());
                                    err.setAddress("Error: " + e.getMessage());
                                    return err;
                                }));
            }

            // Wait for all async results
            CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();

            Map<String, GeolocationResponse> results = new HashMap<>();
            futures.forEach((k, v) -> results.put(k, v.join()));

            GeolocationResponse a = results.get("OpenCellID");
            GeolocationResponse b = results.get("UnwiredLabs");
            GeolocationResponse c = results.get("Combain");

            double ab = distanceIfValid(a, b);
            double ac = distanceIfValid(a, c);
            double bc = distanceIfValid(b, c);

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

            // No valid provider result
            if (chosen == null || chosen.getLatitude() == null) {
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
            return result;
        });
    }
    /**
     * Selects the best provider result based on configured priority and fallbacks.
     * and selects the best one based on shortest distance and priority.
     * Adds fallback logic if chosen provider has no valid result.
     */
    private GeolocationResponse pickByPriorityWithFallback(
            GeolocationResponse r1, GeolocationResponse r2, GeolocationResponse fallback) {
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
        return haversine(r1.getLatitude(), r1.getLongitude(), r2.getLatitude(), r2.getLongitude());
    }

    /**
     * implements the haversine formula to calculate distance(in kilometers)
     * between two on earth given their lat/lon 
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2)*Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R*c;
    }

    // Chooses between two providers bases on priority order
    private GeolocationResponse pickByPriority(GeolocationResponse r1, GeolocationResponse r2) {
        List<String> priorities = priorityService.getProviderPriorities();
        int idx1 = priorities.indexOf(r1.getProviderUsed());
        int idx2 = priorities.indexOf(r2.getProviderUsed());

        if (idx1 == -1 && idx2 == -1) return r1;
        if (idx1 == -1) return r2;
        if (idx2 == -1) return r1;
        return (idx1 < idx2) ? r1 : r2;
    }

    // Save request/response log to DB
    private void saveLogAsync(GeolocationRequest req, GeolocationResponse resp) {
        CompletableFuture.runAsync(() -> {
            RequestLog log = new RequestLog();
            log.setMcc(req.getMcc());
            log.setMnc(req.getMnc());
            log.setLac(req.getLac());
            log.setCellId(req.getCellId());
            log.setProviderUsed(resp.getProviderUsed());
            log.setLatitude(resp.getLatitude());
            log.setLongitude(resp.getLongitude());
            log.setTimestamp(LocalDateTime.now());
            requestLogRepository.save(log);
        });
    }
}
