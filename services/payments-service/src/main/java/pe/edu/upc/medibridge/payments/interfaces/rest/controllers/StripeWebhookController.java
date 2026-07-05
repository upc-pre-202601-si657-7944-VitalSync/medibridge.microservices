package pe.edu.upc.medibridge.payments.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.medibridge.payments.domain.model.commands.ActivateCheckoutSubscriptionCommand;
import pe.edu.upc.medibridge.payments.domain.model.events.PaymentFailedEvent;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.BillingCycle;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.CommercialLine;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.PlanType;
import pe.edu.upc.medibridge.payments.domain.services.SubscriptionCommandService;

@RestController
@RequestMapping(value = "/api/v1/stripe-webhooks", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Stripe Webhooks", description = "Stripe Webhook Endpoints")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class StripeWebhookController {
    private final ApplicationEventPublisher eventPublisher;
    private final SubscriptionCommandService subscriptionCommandService;
    private final String webhookSecret;

    public StripeWebhookController(
            ApplicationEventPublisher eventPublisher,
            SubscriptionCommandService subscriptionCommandService,
            @Value("${stripe.webhook.secret}") String webhookSecret) {
        this.eventPublisher = eventPublisher;
        this.subscriptionCommandService = subscriptionCommandService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> processWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank() || signatureHeader == null || signatureHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        if ("payment_intent.payment_failed".equals(event.getType())) {
            event.getDataObjectDeserializer().getObject()
                    .filter(PaymentIntent.class::isInstance)
                    .map(PaymentIntent.class::cast)
                    .ifPresent(paymentIntent -> eventPublisher.publishEvent(new PaymentFailedEvent(
                            parseUserId(paymentIntent.getMetadata().get("medibridge_user_id")),
                            paymentIntent.getId())));
        }
        if ("checkout.session.completed".equals(event.getType())) {
            event.getDataObjectDeserializer().getObject()
                    .filter(Session.class::isInstance)
                    .map(Session.class::cast)
                    .ifPresent(this::activateSubscriptionFromCheckoutSession);
        }

        return ResponseEntity.ok().build();
    }

    private void activateSubscriptionFromCheckoutSession(Session session) {
        var metadata = session.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return;
        }
        subscriptionCommandService.handle(new ActivateCheckoutSubscriptionCommand(
                parseUserId(metadata.get("medibridge_user_id")),
                CommercialLine.valueOf(metadata.get("commercial_line")),
                PlanType.valueOf(metadata.get("plan_type")),
                BillingCycle.valueOf(metadata.get("billing_cycle")),
                session.getCustomer(),
                session.getId()));
    }

    private Long parseUserId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.valueOf(value);
    }
}
