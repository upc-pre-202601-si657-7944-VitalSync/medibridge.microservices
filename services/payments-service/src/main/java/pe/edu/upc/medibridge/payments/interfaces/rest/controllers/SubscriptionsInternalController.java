package pe.edu.upc.medibridge.payments.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.medibridge.payments.domain.model.queries.GetActiveSubscriptionQuery;
import pe.edu.upc.medibridge.payments.domain.services.SubscriptionQueryService;
import pe.edu.upc.medibridge.payments.interfaces.rest.resources.SubscriptionResponse;
import pe.edu.upc.medibridge.payments.interfaces.rest.transform.SubscriptionResponseFromEntityAssembler;

@RestController
@RequestMapping("/api/v1/internal/subscriptions")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class SubscriptionsInternalController {
    private final SubscriptionQueryService subscriptionQueryService;

    public SubscriptionsInternalController(SubscriptionQueryService subscriptionQueryService) {
        this.subscriptionQueryService = subscriptionQueryService;
    }

    @GetMapping("/users/{userId}/active")
    public ResponseEntity<SubscriptionResponse> getActiveSubscription(@PathVariable Long userId) {
        return subscriptionQueryService.handle(new GetActiveSubscriptionQuery(userId))
                .map(value -> ResponseEntity.ok(SubscriptionResponseFromEntityAssembler.toResourceFromEntity(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
