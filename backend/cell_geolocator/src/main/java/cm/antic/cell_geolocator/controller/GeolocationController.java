package cm.antic.cell_geolocator.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import cm.antic.cell_geolocator.model.GeolocationRequest;
import cm.antic.cell_geolocator.model.GeolocationResponse;
import cm.antic.cell_geolocator.service.GeolocationService;
import cm.antic.cell_geolocator.service.GeolocationAggregatorService;
import cm.antic.cell_geolocator.service.CellTowerLocalService;
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
    private CellTowerLocalService cellTowerLocalService;

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


    /**
     * Returns all prioritized geolocation results (from all sources)
     * plus related cells if the local DB result is used.
     */
    @Operation(
        summary = "Resolve with priority + distance optimization",
        description = "Compares provider distances & priority rankings, and returns chosen + related cells."
    )
    @PostMapping("/geolocate/priority")
    public ResponseEntity<Map<String, Object>> getPriority(@RequestBody GeolocationRequest request) {

        Map<String, Object> result =
            aggregatorService.resolveWithPriorityAsync(request)
                .thenApply(priorityResult -> {
                    List<Map<String, Object>> relatedCells = List.of();

                    // Check if the top priority (chosen) result came from the local DB
                    if (priorityResult.getChosen() != null &&
                        priorityResult.getChosen().getProviderUsed() != null &&
                        priorityResult.getChosen().getProviderUsed().contains("LOCAL_DB(orange_cameroon)")) {

                        try {
                            relatedCells = cellTowerLocalService.findCellsByBtsId(
                                    String.valueOf(request.getCellId()));
                        } catch (Exception e) {
                            System.err.println("Failed to fetch related cells: " + e.getMessage());
                        }
                    }

                    // Return all priority results and related local cells (if applicable)
                    return Map.of(
                        "priorityResults", priorityResult,
                        "relatedCells", relatedCells
                    );
                })
                .join(); 

        return ResponseEntity.ok(result);
    }


    /**
     * Returns only the chosen (best) geolocation result,
     * plus other nearby cells if the result is from the local DB.
     */
    @Operation(
        summary = "Get only the chosen best provider (with related cells)",
        description = "Returns the selected result and related cells based on same BTS."
    )
    @PostMapping("/geolocate/priority/chosen")
    public ResponseEntity<Map<String, Object>> getPriorityChosen(@RequestBody GeolocationRequest request) {

        Map<String, Object> result =
        aggregatorService.resolveWithPriorityAsync(request)
            .thenApply(priorityResult -> {
                GeolocationResponse chosen = priorityResult.getChosen();

                List<Map<String, Object>> relatedCells = List.of(); // Empty by default

                // Only get related cells if local DB provided the chosen result
                if (chosen != null && chosen.getProviderUsed() != null &&
                        chosen.getProviderUsed().contains("LOCAL_DB(orange_cameroon)")) {
                    try {
                        relatedCells = cellTowerLocalService.findCellsByBtsId(
                                String.valueOf(request.getCellId()));
                    } catch (Exception e) {
                        System.err.println("Failed to fetch related cells: " + e.getMessage());
                    }
                }

                // Send back both the requested cell and others from the same BTS
                return Map.of(
                    "requestedCell", chosen,
                    "relatedCells", relatedCells
                );

            })
            .join(); 

        return ResponseEntity.ok(result);
    }

}