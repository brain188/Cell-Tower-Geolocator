package cm.antic.cell_geolocator.service;

import cm.antic.cell_geolocator.entity.RequestLog;
import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.repository.RequestLogRepository;
import cm.antic.cell_geolocator.service.provider.ProviderClient;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeolocationService {

    @Autowired
    private PriorityService priorityService;

    @Autowired
    private List<ProviderClient> providerClients;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Cacheable(value = "geolocation", key = "#request.mcc + '_' + #request.mnc + '_' + #request.lac + '_' + #request.cellId")
    @Retry(name = "providerRetry")
    public GeolocationResponse resolve(GeolocationRequest request) {
        List<String> priorities = priorityService.getProviderPriorities();
        Map<String, Object> rawResponses = new HashMap<>();
        GeolocationResponse response = new GeolocationResponse();

        for (String providerName : priorities) {
            ProviderClient client = getClientByName(providerName);
            if (client != null) {
                try {
                    GeolocationResponse providerResponse = client.resolve(request);
                    rawResponses.put(providerName, providerResponse);
                    if (providerResponse.getLatitude() != null && providerResponse.getLongitude() != null) {
                        response.setLatitude(providerResponse.getLatitude());
                        response.setLongitude(providerResponse.getLongitude());
                        response.setProviderUsed(providerName);
                        break; 
                    }
                } catch (Exception e) {
                    
                }
            }
        }
        response.setRawResponses(rawResponses);

        // Log to DB
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

        return response;
    }

    private ProviderClient getClientByName(String name) {
        return providerClients.stream().filter(c -> c.getProviderName().equals(name)).findFirst().orElse(null);
    }

}
