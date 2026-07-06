package pe.edu.upc.medibridge.payments.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.medibridge.payments.application.internal.outboundservices.acl.StripePaymentGatewayService;
import pe.edu.upc.medibridge.payments.domain.model.commands.ActivateCheckoutSubscriptionCommand;
import pe.edu.upc.medibridge.payments.domain.model.commands.CancelSubscriptionCommand;
import pe.edu.upc.medibridge.payments.domain.model.commands.RenewSubscriptionCommand;
import pe.edu.upc.medibridge.payments.domain.model.queries.GetActiveSubscriptionQuery;
import pe.edu.upc.medibridge.payments.domain.model.queries.GetSubscriptionByUserQuery;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.BillingCycle;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.CommercialLine;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.PlanType;
import pe.edu.upc.medibridge.payments.domain.services.PaymentMethodCommandService;
import pe.edu.upc.medibridge.payments.domain.services.SubscriptionCommandService;
import pe.edu.upc.medibridge.payments.domain.services.SubscriptionQueryService;
import pe.edu.upc.medibridge.payments.infrastructure.persistence.jpa.repositories.PlanRepository;
import pe.edu.upc.medibridge.payments.interfaces.rest.resources.AddPaymentMethodRequest;
import pe.edu.upc.medibridge.payments.interfaces.rest.resources.CheckoutSessionResponse;
import pe.edu.upc.medibridge.payments.interfaces.rest.resources.ConfirmCheckoutSessionRequest;
import pe.edu.upc.medibridge.payments.interfaces.rest.resources.CreateCheckoutSessionRequest;
import pe.edu.upc.medibridge.payments.interfaces.rest.resources.CreateSubscriptionRequest;
import pe.edu.upc.medibridge.payments.interfaces.rest.resources.PaymentMethodResponse;
import pe.edu.upc.medibridge.payments.interfaces.rest.resources.SubscriptionResponse;
import pe.edu.upc.medibridge.payments.interfaces.rest.transform.AddPaymentMethodCommandFromResourceAssembler;
import pe.edu.upc.medibridge.payments.interfaces.rest.transform.CreateSubscriptionCommandFromResourceAssembler;
import pe.edu.upc.medibridge.payments.interfaces.rest.transform.PaymentMethodResponseFromEntityAssembler;
import pe.edu.upc.medibridge.payments.interfaces.rest.transform.SubscriptionResponseFromEntityAssembler;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Subscriptions", description = "Subscription Management Endpoints")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class SubscriptionController {
    private final SubscriptionCommandService subscriptionCommandService;
    private final SubscriptionQueryService subscriptionQueryService;
    private final PaymentMethodCommandService paymentMethodCommandService;
    private final StripePaymentGatewayService stripePaymentGatewayService;
    private final PlanRepository planRepository;
    private final String frontendAppUrl;
    private final boolean paymentMocksEnabled;

    public SubscriptionController(
            SubscriptionCommandService subscriptionCommandService,
            SubscriptionQueryService subscriptionQueryService,
            PaymentMethodCommandService paymentMethodCommandService,
            StripePaymentGatewayService stripePaymentGatewayService,
            PlanRepository planRepository,
            @Value("${frontend.app.url}") String frontendAppUrl,
            @Value("${payments.mock.enabled:false}") boolean paymentMocksEnabled) {
        this.subscriptionCommandService = subscriptionCommandService;
        this.subscriptionQueryService = subscriptionQueryService;
        this.paymentMethodCommandService = paymentMethodCommandService;
        this.stripePaymentGatewayService = stripePaymentGatewayService;
        this.planRepository = planRepository;
        this.frontendAppUrl = frontendAppUrl;
        this.paymentMocksEnabled = paymentMocksEnabled;
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(@RequestBody CreateSubscriptionRequest resource) {
        var command = CreateSubscriptionCommandFromResourceAssembler.toCommandFromResource(resource);
        var subscription = subscriptionCommandService.handle(command);
        return subscription
                .map(value -> new ResponseEntity<>(SubscriptionResponseFromEntityAssembler.toResourceFromEntity(value), HttpStatus.CREATED))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(@RequestBody CreateCheckoutSessionRequest resource) {
        var plan = planRepository.findByCommercialLineAndPlanTypeAndBillingCycleAndActiveTrue(
                        resource.commercialLine(),
                        resource.planType(),
                        resource.billingCycle())
                .orElse(null);
        if (plan == null) {
            return ResponseEntity.badRequest().build();
        }

        var checkoutUrl = stripePaymentGatewayService.createCheckoutSession(
                resource.userId(),
                plan,
                frontendAppUrl + "/subscriptions?checkout=success&session_id={CHECKOUT_SESSION_ID}",
                frontendAppUrl + "/subscriptions?checkout=cancelled");
        return ResponseEntity.ok(new CheckoutSessionResponse(checkoutUrl));
    }

    @PostMapping("/checkout/confirm")
    public ResponseEntity<SubscriptionResponse> confirmCheckoutSession(@RequestBody ConfirmCheckoutSessionRequest resource) {
        if (resource.sessionId() == null || resource.sessionId().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        var session = stripePaymentGatewayService.retrieveCheckoutSession(resource.sessionId());
        if (!"complete".equals(session.status())) {
            return ResponseEntity.badRequest().build();
        }
        if (!"paid".equals(session.paymentStatus()) && !"no_payment_required".equals(session.paymentStatus())) {
            return ResponseEntity.badRequest().build();
        }

        var metadata = session.metadata();
        if (metadata == null
                || !metadata.containsKey("medibridge_user_id")
                || !metadata.containsKey("commercial_line")
                || !metadata.containsKey("plan_type")
                || !metadata.containsKey("billing_cycle")) {
            return ResponseEntity.badRequest().build();
        }

        var subscription = subscriptionCommandService.handle(new ActivateCheckoutSubscriptionCommand(
                parseUserId(metadata.get("medibridge_user_id")),
                CommercialLine.valueOf(metadata.get("commercial_line")),
                PlanType.valueOf(metadata.get("plan_type")),
                BillingCycle.valueOf(metadata.get("billing_cycle")),
                session.customerId(),
                session.id()));

        return subscription
                .map(value -> ResponseEntity.ok(SubscriptionResponseFromEntityAssembler.toResourceFromEntity(value)))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/mock/approve")
    public ResponseEntity<SubscriptionResponse> approveMockSubscription(@RequestBody CreateCheckoutSessionRequest resource) {
        if (!paymentMocksEnabled) {
            return ResponseEntity.notFound().build();
        }

        var subscription = subscriptionCommandService.handle(new ActivateCheckoutSubscriptionCommand(
                resource.userId(),
                resource.commercialLine(),
                resource.planType(),
                resource.billingCycle(),
                "mock-local-user-" + resource.userId() + "-" + UUID.randomUUID(),
                "mock-checkout-session-" + UUID.randomUUID()));

        return subscription
                .map(value -> ResponseEntity.ok(SubscriptionResponseFromEntityAssembler.toResourceFromEntity(value)))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/{subscriptionId}/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(@PathVariable Long subscriptionId) {
        var subscription = subscriptionCommandService.handle(new CancelSubscriptionCommand(subscriptionId));
        return subscription
                .map(value -> ResponseEntity.ok(SubscriptionResponseFromEntityAssembler.toResourceFromEntity(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{subscriptionId}/renew")
    public ResponseEntity<SubscriptionResponse> renewSubscription(@PathVariable Long subscriptionId) {
        var subscription = subscriptionCommandService.handle(new RenewSubscriptionCommand(subscriptionId));
        return subscription
                .map(value -> ResponseEntity.ok(SubscriptionResponseFromEntityAssembler.toResourceFromEntity(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<SubscriptionResponse> getSubscriptionByUser(@PathVariable Long userId) {
        var subscription = subscriptionQueryService.handle(new GetSubscriptionByUserQuery(userId));
        return subscription
                .map(value -> ResponseEntity.ok(SubscriptionResponseFromEntityAssembler.toResourceFromEntity(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/users/{userId}/active")
    public ResponseEntity<SubscriptionResponse> getActiveSubscription(@PathVariable Long userId) {
        var subscription = subscriptionQueryService.handle(new GetActiveSubscriptionQuery(userId));
        return subscription
                .map(value -> ResponseEntity.ok(SubscriptionResponseFromEntityAssembler.toResourceFromEntity(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping("/payment-methods")
    public ResponseEntity<PaymentMethodResponse> addPaymentMethod(@RequestBody AddPaymentMethodRequest resource) {
        var command = AddPaymentMethodCommandFromResourceAssembler.toCommandFromResource(resource);
        var paymentMethod = paymentMethodCommandService.handle(command);
        return paymentMethod
                .map(value -> new ResponseEntity<>(PaymentMethodResponseFromEntityAssembler.toResourceFromEntity(value), HttpStatus.CREATED))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    private Long parseUserId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
