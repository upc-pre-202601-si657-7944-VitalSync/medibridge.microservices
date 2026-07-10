package pe.edu.upc.medibridge.payments.interfaces.rest.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import pe.edu.upc.medibridge.payments.application.internal.outboundservices.acl.StripePaymentGatewayService;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.BillingCycle;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.CommercialLine;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.PlanType;
import pe.edu.upc.medibridge.payments.domain.services.PaymentMethodCommandService;
import pe.edu.upc.medibridge.payments.domain.services.SubscriptionCommandService;
import pe.edu.upc.medibridge.payments.domain.services.SubscriptionQueryService;
import pe.edu.upc.medibridge.payments.infrastructure.persistence.jpa.repositories.PlanRepository;
import pe.edu.upc.medibridge.payments.interfaces.rest.resources.CreateCheckoutSessionRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionCommandService subscriptionCommandService;

    @Mock
    private SubscriptionQueryService subscriptionQueryService;

    @Mock
    private PaymentMethodCommandService paymentMethodCommandService;

    @Mock
    private StripePaymentGatewayService stripePaymentGatewayService;

    @Mock
    private PlanRepository planRepository;

    @Test
    void createCheckoutSessionReturnsBadRequestWhenPlanDoesNotExist() {
        var request = new CreateCheckoutSessionRequest(
                15L,
                CommercialLine.FAMILY,
                PlanType.FAMILY_PREMIUM,
                BillingCycle.MONTHLY,
                "http://localhost:8081/dashboard");
        when(planRepository.findByCommercialLineAndPlanTypeAndBillingCycleAndActiveTrue(
                request.commercialLine(),
                request.planType(),
                request.billingCycle()))
                .thenReturn(Optional.empty());

        var controller = controller(false);
        var response = controller.createCheckoutSession(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void approveMockSubscriptionReturnsNotFoundWhenMocksAreDisabled() {
        var request = new CreateCheckoutSessionRequest(
                15L,
                CommercialLine.FAMILY,
                PlanType.FAMILY_PREMIUM,
                BillingCycle.MONTHLY,
                "http://localhost:8081/dashboard");

        var controller = controller(false);
        var response = controller.approveMockSubscription(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private SubscriptionController controller(boolean paymentMocksEnabled) {
        return new SubscriptionController(
                subscriptionCommandService,
                subscriptionQueryService,
                paymentMethodCommandService,
                stripePaymentGatewayService,
                planRepository,
                "http://localhost:5173",
                paymentMocksEnabled);
    }
}
