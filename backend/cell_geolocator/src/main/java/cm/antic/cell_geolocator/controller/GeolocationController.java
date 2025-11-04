package cm.antic.cell_geolocator.controller;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.model.PriorityGeolocationResult;
import cm.antic.cell_geolocator.service.GeolocationService;
import cm.antic.cell_geolocator.service.GeolocationAggregatorService;
import io.github.bucket4j.Bucket;

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

    
    // PRIORITY-FIRST FASTEST RESULT FROM PROVIDERS

    @Operation(
        summary = "Resolve geolocation (async priority-first)",
        description = "Returns first successful provider async based on priority."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Resolved",
            content = @Content(schema = @Schema(implementation = GeolocationResponse.class))),
        @ApiResponse(responseCode = "429", description = "Too Many Requests")
    })
    @PostMapping("/geolocate")
    public ResponseEntity<CompletableFuture<GeolocationResponse>> resolveGeolocation(
            @RequestBody GeolocationRequest request) {

        if (!rateLimiterBucket.tryConsumeAndReturnRemaining(1).isConsumed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        CompletableFuture<GeolocationResponse> future = geolocationService.resolveAsync(request);
        return ResponseEntity.ok(future);
    }

    // ------------------------------------------------------
    // QUERY **ALL** PROVIDERS IN PARALLEL - RETURN ALL
    // ------------------------------------------------------
    // @Operation(
    //     summary = "Resolve from all providers (async)",
    //     description = "Calls all providers concurrently & returns individual results."
    // )
    // @PostMapping("/geolocate/all")
    // public ResponseEntity<CompletableFuture<Map<String, GeolocationResponse>>> resolveWithAll(
    //         @RequestBody GeolocationRequest request) {

    //     if (!rateLimiterBucket.tryConsumeAndReturnRemaining(1).isConsumed()) {
    //         return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
    //     }

    //     CompletableFuture<Map<String, GeolocationResponse>> future =
    //             aggregatorService.resolveWithAllAsync(request);

    //     return ResponseEntity.ok(future);
    // }


    // PRIORITY + DISTANCE ALGORITHM (ASYNC)

    @Operation(
        summary = "Resolve with priority + distance optimization ",
        description = "Compares provider distances & priority rankings."
    )
    @PostMapping("/geolocate/priority")
    public ResponseEntity<CompletableFuture<PriorityGeolocationResult>> resolveWithPriority(
            @RequestBody GeolocationRequest request) {

        if (!rateLimiterBucket.tryConsumeAndReturnRemaining(1).isConsumed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        CompletableFuture<PriorityGeolocationResult> future =
                aggregatorService.resolveWithPriorityAsync(request);

        return ResponseEntity.ok(future);
    }


    // SIMPLE ENDPOINT — ONLY CHOSEN BEST RESULT

    @Operation(
        summary = "Get only the chosen best provider",
        description = "Only returns final selected result"
    )
    @PostMapping("geolocate/priority/chosen")
    public CompletableFuture<GeolocationResponse> getPriorityChosen(
            @RequestBody GeolocationRequest request) {

        return aggregatorService.resolveWithPriorityAsync(request)
                .thenApply(PriorityGeolocationResult::getChosen);
    }
}
