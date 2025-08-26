package cm.antic.cell_geolocator.controller;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.service.GeolocationService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class GeolocationController {

    @Autowired
    private GeolocationService geolocationService;

    @Autowired
    private Bucket rateLimiterBucket;

    @PostMapping("/resolve")
    public ResponseEntity<GeolocationResponse> resolveGeolocation(@RequestBody GeolocationRequest request) {
        ConsumptionProbe probe = rateLimiterBucket.tryConsumeAndReturnRemaining(1);

        if(!probe.isConsumed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        GeolocationResponse response = geolocationService.resolve(request);
        return ResponseEntity.ok(response);
    }
}
