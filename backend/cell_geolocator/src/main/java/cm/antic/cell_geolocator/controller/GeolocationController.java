package cm.antic.cell_geolocator.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.model.PriorityGeolocationResult;
import cm.antic.cell_geolocator.service.GeolocationAggregatorService;
import cm.antic.cell_geolocator.service.GeolocationService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


@RestController
@RequestMapping("/api/v1")
@Tag(name = "Geolocation", description = "APIs for resolving cell tower geolocation")
public class GeolocationController {

    @Autowired
    private GeolocationService geolocationService;

    @Autowired
    private GeolocationAggregatorService aggregatorService;

    @Autowired
    private Bucket rateLimiterBucket;

    // endpoint for all providers and also displaying the first successful response(following priority order)
    @Operation(
        summary = "Resolve geolocation (priority-based first result)",
        description = "Resolves cell geolocation using all providers, but only returns the first successful response."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully resolved geolocation",
                    content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = GeolocationResponse.class))),
        @ApiResponse(responseCode = "429", description = "Too many requests"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })

    @PostMapping("/geolocate")
    public ResponseEntity<GeolocationResponse> resolveGeolocation(@RequestBody GeolocationRequest request) {
        ConsumptionProbe probe = rateLimiterBucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        GeolocationResponse response = geolocationService.resolve(request);
        return ResponseEntity.ok(response);
    }

    // endpoint for all providers
    @Operation(
        summary = "Resolve with all providers",
        description = "Returns responses from all providers simultaneously for comparison."
    )
    @PostMapping("/geolocate/all")
    public ResponseEntity<Map<String, GeolocationResponse>> resolveGeolocationWithAllProviders(@RequestBody GeolocationRequest request) {
        ConsumptionProbe probe = rateLimiterBucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        Map<String, GeolocationResponse> responses = aggregatorService.resolveWithAllProviders(request);
        return ResponseEntity.ok(responses);
    }

    // endpoint for priority-based resolution based on distance calculation
    @Operation(
        summary = "Resolve with provider priority",
        description = "Performs provider resolution and chooses based on distance and priority rules."
    )
    @PostMapping("/geolocate/priority")
    public ResponseEntity<PriorityGeolocationResult> resolveGeolocationWithPriority(@RequestBody GeolocationRequest request) {
        ConsumptionProbe probe = rateLimiterBucket.tryConsumeAndReturnRemaining(1);

        if(!probe.isConsumed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        PriorityGeolocationResult result = aggregatorService.resolveWithPriority(request);
        return ResponseEntity.ok(result);
    }
    
    // This endpoint displays only the chosen provider's result from the priority-based resolution after distance calculation.
    // It is useful when the client only wants the final resolved location without extra details
    @Operation(
        summary = "Get only the chosen priority result",
        description = "Returns only the final chosen provider's geolocation result after applying priority rules."
    )
    @GetMapping("geolocate/priority/chosen")
    public GeolocationResponse getPriorityChosen(@RequestBody GeolocationRequest request) {
    PriorityGeolocationResult result = aggregatorService.resolveWithPriority(request);
        return result.getChosen();
}

}
