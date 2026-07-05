package pe.edu.upc.medibridge.payments.infrastructure.stripe;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.checkout.SessionCreateParams;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.edu.upc.medibridge.payments.application.internal.outboundservices.acl.StripePaymentGatewayService;
import pe.edu.upc.medibridge.payments.domain.model.entities.Plan;
import pe.edu.upc.medibridge.payments.domain.model.exceptions.PaymentProcessingException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class StripePaymentGatewayAdapter implements StripePaymentGatewayService {
    public StripePaymentGatewayAdapter(@Value("${stripe.secret.key}") String stripeSecretKey) {
        Stripe.apiKey = stripeSecretKey;
    }

    @Override
    @CircuitBreaker(name = "stripeApi", fallbackMethod = "createCustomerFallback")
    public String createCustomer(Long userId) {
        try {
            var params = CustomerCreateParams.builder()
                    .putMetadata("medibridge_user_id", String.valueOf(userId))
                    .build();
            Customer customer = Customer.create(params);
            return customer.getId();
        } catch (StripeException exception) {
            throw new PaymentProcessingException("Stripe customer creation failed: " + exception.getMessage());
        }
    }

    @Override
    @CircuitBreaker(name = "stripeApi", fallbackMethod = "createPaymentIntentFallback")
    public String createPaymentIntent(Long userId, Plan plan) {
        try {
            var params = PaymentIntentCreateParams.builder()
                    .setAmount(toMinorCurrencyUnit(plan.getPrice()))
                    .setCurrency(plan.getCurrency().toLowerCase())
                    .setDescription("MediBridge " + plan.getPlanType() + " " + plan.getBillingCycle() + " subscription")
                    .putMetadata("medibridge_user_id", String.valueOf(userId))
                    .putMetadata("plan_type", plan.getPlanType().name())
                    .putMetadata("billing_cycle", plan.getBillingCycle().name())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .build();
            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return paymentIntent.getId();
        } catch (StripeException exception) {
            throw new PaymentProcessingException("Stripe payment intent creation failed: " + exception.getMessage());
        }
    }

    @Override
    @CircuitBreaker(name = "stripeApi", fallbackMethod = "createCheckoutSessionFallback")
    public String createCheckoutSession(Long userId, Plan plan, String successUrl, String cancelUrl) {
        try {
            var customerId = createCustomer(userId);
            var interval = plan.getBillingCycle().name().equals("ANNUALLY")
                    ? SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR
                    : SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH;

            var params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(plan.getCurrency().toLowerCase())
                                                    .setUnitAmount(toMinorCurrencyUnit(plan.getPrice()))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(plan.getDisplayName())
                                                                    .build())
                                                    .setRecurring(
                                                            SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                                                    .setInterval(interval)
                                                                    .build())
                                                    .build())
                                    .build())
                    .putMetadata("medibridge_user_id", String.valueOf(userId))
                    .putMetadata("commercial_line", plan.getCommercialLine().name())
                    .putMetadata("plan_type", plan.getPlanType().name())
                    .putMetadata("billing_cycle", plan.getBillingCycle().name())
                    .build();

            var session = Session.create(params);
            return session.getUrl();
        } catch (StripeException exception) {
            throw new PaymentProcessingException("Stripe checkout session creation failed: " + exception.getMessage());
        }
    }

    @Override
    @CircuitBreaker(name = "stripeApi", fallbackMethod = "cancelActiveSubscriptionsFallback")
    public void cancelActiveSubscriptions(String stripeCustomerId) {
        try {
            var params = SubscriptionListParams.builder()
                    .setCustomer(stripeCustomerId)
                    .setStatus(SubscriptionListParams.Status.ALL)
                    .build();
            for (var subscription : Subscription.list(params).getData()) {
                if ("active".equals(subscription.getStatus()) || "trialing".equals(subscription.getStatus())) {
                    subscription.cancel();
                }
            }
        } catch (StripeException exception) {
            throw new PaymentProcessingException("Stripe subscription cancellation failed: " + exception.getMessage());
        }
    }

    private String createCustomerFallback(Long userId, Throwable exception) {
        throw new PaymentProcessingException("Stripe customer creation circuit breaker fallback: " + exception.getMessage());
    }

    private String createPaymentIntentFallback(Long userId, Plan plan, Throwable exception) {
        throw new PaymentProcessingException("Stripe payment intent circuit breaker fallback: " + exception.getMessage());
    }

    private String createCheckoutSessionFallback(Long userId, Plan plan, String successUrl, String cancelUrl, Throwable exception) {
        throw new PaymentProcessingException("Stripe checkout session circuit breaker fallback: " + exception.getMessage());
    }

    private void cancelActiveSubscriptionsFallback(String stripeCustomerId, Throwable exception) {
        throw new PaymentProcessingException("Stripe subscription cancellation circuit breaker fallback: " + exception.getMessage());
    }

    private Long toMinorCurrencyUnit(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }
}

