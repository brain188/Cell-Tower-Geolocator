package cm.antic.cell_geolocator.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import cm.antic.cell_geolocator.entity.RequestLog;
import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.repository.RequestLogRepository;
import cm.antic.cell_geolocator.service.provider.ProviderClient;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class GeolocationService {

    @Autowired
    private PriorityService priorityService;

    @Autowired
    private List<ProviderClient> providerClients;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private ReverseGeocodeService reverseGeocodeService;

    @Cacheable(value = "geolocation", key = "#request.mcc + '_' + #request.mnc + '_' + #request.lac + '_' + #request.cellId")
    @Retry(name = "providerRetry")
    public GeolocationResponse resolve(GeolocationRequest request) {
        List<String> priorities = priorityService.getProviderPriorities();
        Map<String, Object> rawResponses = new HashMap<>();
        GeolocationResponse finalResponse = new GeolocationResponse();

        for (String providerName : priorities) {
            ProviderClient client = getClientByName(providerName);
            if (client != null) {
                try {
                    GeolocationResponse providerResponse = client.resolve(request);
                    rawResponses.put(providerName, providerResponse);
                    if (providerResponse.getLatitude() != null && providerResponse.getLongitude() != null && finalResponse.getLatitude() == null) {
                        finalResponse.setLatitude(providerResponse.getLatitude());
                        finalResponse.setLongitude(providerResponse.getLongitude());
                        finalResponse.setProviderUsed(providerName);

                        // address details for the first successful response
                        reverseGeocodeService.addAddressToResponse(finalResponse);
                    }
                } catch (Exception e) {
                    rawResponses.put(providerName, Map.of("error", e.getMessage()));
                }
            }
        }
        finalResponse.setRawResponses(rawResponses);

        // Log to DB
        RequestLog log = new RequestLog();
        log.setMcc(request.getMcc());
        log.setMnc(request.getMnc());
        log.setLac(request.getLac());
        log.setCellId(request.getCellId());
        log.setProviderUsed(finalResponse.getProviderUsed());
        log.setLatitude(finalResponse.getLatitude());
        log.setLongitude(finalResponse.getLongitude());
        log.setTimestamp(LocalDateTime.now());
        requestLogRepository.save(log);

        return finalResponse;
    }

    private ProviderClient getClientByName(String name) {
        return providerClients.stream().filter(c -> c.getProviderName().equals(name)).findFirst().orElse(null);
    }

}
