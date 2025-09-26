package cm.antic.cell_geolocator.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import cm.antic.cell_geolocator.entity.RequestLog;
import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.model.PriorityGeolocationResult;
import cm.antic.cell_geolocator.repository.RequestLogRepository;
import cm.antic.cell_geolocator.service.provider.ProviderClient;

/**
 * Service that aggregates results from multiple geolocation providers
 * (OpenCellID, UnwiredLabs, Combain). It calculates distances between
 * providers' results and chooses the most reliable one based on
 * shortest distance + configured priority order.
 */
@Service
public class GeolocationAggregatorService {

    private final List<ProviderClient> providers;
    private final PriorityService priorityService;
    private final RequestLogRepository requestLogRepository;

    public GeolocationAggregatorService(
            List<ProviderClient> providers,
            PriorityService priorityService,
            RequestLogRepository requestLogRepository) {
        this.providers = providers;
        this.priorityService = priorityService;
        this.requestLogRepository = requestLogRepository;
    }

    // Calls every provider with the same request and collects their responses
    public Map<String, GeolocationResponse> resolveWithAllProviders(GeolocationRequest request) {
        Map<String, GeolocationResponse> results = new HashMap<>();

        for (ProviderClient provider : providers) {
            try {
                GeolocationResponse resp = provider.resolve(request);
                results.put(provider.getProviderName(), resp);

                // Save each provider result in DB
                saveLog(request, resp);
            } catch (Exception e) {
                // Capture errors gracefully
                GeolocationResponse errorResponse = new GeolocationResponse();
                errorResponse.setProviderUsed(provider.getProviderName());
                errorResponse.setAddress("Error: " + e.getMessage());
                results.put(provider.getProviderName(), errorResponse);

                saveLog(request, errorResponse);
            }
        }

        return results;
    }

    /** 
     * Gets results from all providers, calculates pairwise distances,
     * and selects the best one based on shortest distance and priority.
     * Adds fallback logic if chosen provider has no valid result.
     */
    public PriorityGeolocationResult resolveWithPriority(GeolocationRequest request) {
        Map<String, GeolocationResponse> results = resolveWithAllProviders(request);

        GeolocationResponse a = results.get("OpenCellID");
        GeolocationResponse b = results.get("UnwiredLabs");
        GeolocationResponse c = results.get("Combain");

        double ab = distanceIfValid(a, b);
        double ac = distanceIfValid(a, c);
        double bc = distanceIfValid(b, c);

        double min = Math.min(ab, Math.min(ac, bc));
        GeolocationResponse chosen = null;
        String shortestPair = null;

        if (min == ab) {
            chosen = pickByPriorityWithFallback(a, b, c);
            shortestPair = "AB";
        } else if (min == ac) {
            chosen = pickByPriorityWithFallback(a, c, b);
            shortestPair = "AC";
        } else if (min == bc) {
            chosen = pickByPriorityWithFallback(b, c, a);
            shortestPair = "BC";
        }

        // If no provider returned a valid result at all
        if (chosen == null || chosen.getLatitude() == null || chosen.getLongitude() == null) {
            GeolocationResponse noResult = new GeolocationResponse();
            noResult.setProviderUsed("None");
            noResult.setAddress("No result found in any provider");
            chosen = noResult;
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

        // Save chosen result
        saveLog(request, chosen);

        return result;
    }

    /**
     * Enhanced priority picker with fallback:
     * - Chooses by configured priority
     * - If chosen has no coordinates → fallback to other in pair
     * - If both invalid → fallback to third provider if valid
     */
    private GeolocationResponse pickByPriorityWithFallback(
            GeolocationResponse r1,
            GeolocationResponse r2,
            GeolocationResponse fallback) {

        GeolocationResponse preferred = pickByPriority(r1, r2);

        // Check if preferred provider has valid coordinates
        if (isValid(preferred)) {
            return preferred;
        }

        // Otherwise, try the other provider in the pair
        GeolocationResponse alternative = (preferred == r1) ? r2 : r1;
        if (isValid(alternative)) {
            return alternative;
        }

        // Finally, fallback to the third provider if valid
        if (isValid(fallback)) {
            return fallback;
        }

        // No valid result in any provider
        return null;
    }

    // Checks if a response has valid lat/lon
    private boolean isValid(GeolocationResponse resp) {
        return resp != null && resp.getLatitude() != null && resp.getLongitude() != null;
    }

    // checks validity before distance calculation
    private double distanceIfValid(GeolocationResponse r1, GeolocationResponse r2) {
        if (!isValid(r1) || !isValid(r2)) {
            return Double.MAX_VALUE;
        }
        return haversine(r1.getLatitude(), r1.getLongitude(),
                         r2.getLatitude(), r2.getLongitude());
    }

    /**
     * implements the haversine formula to calculate distance(in kilometers)
     * between two on earth given thier lat/lon 
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Chooses between two providers based on priority order
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
    private void saveLog(GeolocationRequest request, GeolocationResponse response) {
        RequestLog log = new RequestLog();
        log.setMcc(request.getMcc());
        log.setMnc(request.getMnc());
        log.setLac(request.getLac());
        log.setCellId(request.getCellId());
        log.setProviderUsed(response.getProviderUsed());
        log.setLatitude(response.getLatitude());
        log.setLongitude(response.getLongitude());
        log.setTimestamp(LocalDateTime.now());

        requestLogRepository.save(log);
    }
}
