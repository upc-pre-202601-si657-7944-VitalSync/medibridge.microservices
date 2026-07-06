package pe.edu.upc.medibridge.payments.application.internal.commandservices;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import pe.edu.upc.medibridge.payments.application.internal.outboundservices.acl.IamExternalSubscriptionService;
import pe.edu.upc.medibridge.payments.application.internal.outboundservices.acl.StripePaymentGatewayService;
import pe.edu.upc.medibridge.payments.domain.model.aggregates.Invoice;
import pe.edu.upc.medibridge.payments.domain.model.aggregates.Subscription;
import pe.edu.upc.medibridge.payments.domain.model.commands.ActivateCheckoutSubscriptionCommand;
import pe.edu.upc.medibridge.payments.domain.model.commands.CancelSubscriptionCommand;
import pe.edu.upc.medibridge.payments.domain.model.commands.CreateSubscriptionCommand;
import pe.edu.upc.medibridge.payments.domain.model.commands.RenewSubscriptionCommand;
import pe.edu.upc.medibridge.payments.domain.model.entities.Transaction;
import pe.edu.upc.medibridge.payments.domain.model.events.SubscriptionActivatedEvent;
import pe.edu.upc.medibridge.payments.domain.model.events.SubscriptionCancelledEvent;
import pe.edu.upc.medibridge.payments.domain.model.events.SubscriptionRenewedEvent;
import pe.edu.upc.medibridge.payments.domain.model.exceptions.PaymentProcessingException;
import pe.edu.upc.medibridge.payments.domain.model.exceptions.SubscriptionAlreadyActiveException;
import pe.edu.upc.medibridge.payments.domain.model.exceptions.SubscriptionNotFoundException;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.InvoiceStatus;
import pe.edu.upc.medibridge.payments.domain.model.valueobjects.SubscriptionStatus;
import pe.edu.upc.medibridge.payments.domain.services.SubscriptionCommandService;
import pe.edu.upc.medibridge.payments.infrastructure.persistence.jpa.repositories.InvoiceRepository;
import pe.edu.upc.medibridge.payments.infrastructure.persistence.jpa.repositories.PlanRepository;
import pe.edu.upc.medibridge.payments.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import pe.edu.upc.medibridge.payments.infrastructure.persistence.jpa.repositories.TransactionRepository;
import pe.edu.upc.medibridge.payments.infrastructure.messaging.publishers.PaymentIntegrationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final StripePaymentGatewayService stripePaymentGatewayService;
    private final IamExternalSubscriptionService iamExternalSubscriptionService;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentIntegrationEventPublisher integrationEventPublisher;

    public SubscriptionCommandServiceImpl(
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            InvoiceRepository invoiceRepository,
            TransactionRepository transactionRepository,
            StripePaymentGatewayService stripePaymentGatewayService,
            IamExternalSubscriptionService iamExternalSubscriptionService,
            ApplicationEventPublisher eventPublisher,
            PaymentIntegrationEventPublisher integrationEventPublisher) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.invoiceRepository = invoiceRepository;
        this.transactionRepository = transactionRepository;
        this.stripePaymentGatewayService = stripePaymentGatewayService;
        this.iamExternalSubscriptionService = iamExternalSubscriptionService;
        this.eventPublisher = eventPublisher;
        this.integrationEventPublisher = integrationEventPublisher;
    }

    @Override
    public Optional<Subscription> handle(CreateSubscriptionCommand command) {
        if (!iamExternalSubscriptionService.userExists(command.userId())) {
            throw new PaymentProcessingException("User not found in IAM: " + command.userId());
        }
        if (subscriptionRepository.existsByUserIdAndStatus(command.userId(), SubscriptionStatus.ACTIVE)) {
            throw new SubscriptionAlreadyActiveException(command.userId());
        }
        var plan = planRepository.findByCommercialLineAndPlanTypeAndBillingCycleAndActiveTrue(
                        command.commercialLine(),
                        command.planType(),
                        command.billingCycle())
                .orElseThrow(() -> new PaymentProcessingException("Plan not found"));
        var stripeCustomerId = plan.getPrice().signum() > 0
                ? stripePaymentGatewayService.createCustomer(command.userId())
                : "free-local-user-" + command.userId();
        var now = LocalDate.now();
        var periodEnd = command.billingCycle().name().equals("ANNUALLY") ? now.plusYears(1) : now.plusMonths(1);
        var subscription = subscriptionRepository.save(new Subscription(command.userId(), plan, stripeCustomerId, now, periodEnd));
        if (plan.getPrice().signum() > 0) {
            var paymentIntentId = stripePaymentGatewayService.createPaymentIntent(command.userId(), plan);
            transactionRepository.save(new Transaction(command.userId(), plan.getPrice(), plan.getCurrency(), paymentIntentId, "SUCCEEDED"));
        }
        invoiceRepository.save(new Invoice(command.userId(), subscription.getId(), plan.getPrice(), plan.getCurrency(), InvoiceStatus.PAID));
        eventPublisher.publishEvent(new SubscriptionActivatedEvent(subscription.getId(), subscription.getUserId()));
        integrationEventPublisher.publishSubscriptionActivated(subscription.getUserId(), subscription.getId());
        return Optional.of(subscription);
    }

    @Override
    public Optional<Subscription> handle(ActivateCheckoutSubscriptionCommand command) {
        if (!iamExternalSubscriptionService.userExists(command.userId())) {
            throw new PaymentProcessingException("User not found in IAM: " + command.userId());
        }
        var plan = planRepository.findByCommercialLineAndPlanTypeAndBillingCycleAndActiveTrue(
                        command.commercialLine(),
                        command.planType(),
                        command.billingCycle())
                .orElseThrow(() -> new PaymentProcessingException("Plan not found"));

        var activeSubscription = subscriptionRepository.findByUserIdAndStatus(command.userId(), SubscriptionStatus.ACTIVE);
        if (activeSubscription.isPresent()) {
            var active = activeSubscription.get();
            var activeUsesSamePlan = active.getPlan().getId().equals(plan.getId());
            var activeIsLocal = isLocalSubscription(active.getStripeCustomerId());
            var incomingIsLocal = isLocalSubscription(command.stripeCustomerId());
            if (activeUsesSamePlan && (!activeIsLocal || incomingIsLocal)) {
                return activeSubscription;
            }
            if (!activeIsLocal) {
                stripePaymentGatewayService.cancelActiveSubscriptions(active.getStripeCustomerId());
            }
            active.cancel();
            var cancelled = subscriptionRepository.save(active);
            eventPublisher.publishEvent(new SubscriptionCancelledEvent(cancelled.getId(), cancelled.getUserId()));
        }

        var now = LocalDate.now();
        var periodEnd = command.billingCycle().name().equals("ANNUALLY") ? now.plusYears(1) : now.plusMonths(1);
        var subscription = subscriptionRepository.save(new Subscription(
                command.userId(),
                plan,
                command.stripeCustomerId(),
                now,
                periodEnd));

        transactionRepository.save(new Transaction(
                command.userId(),
                plan.getPrice(),
                plan.getCurrency(),
                command.checkoutSessionId(),
                "SUCCEEDED"));
        invoiceRepository.save(new Invoice(command.userId(), subscription.getId(), plan.getPrice(), plan.getCurrency(), InvoiceStatus.PAID));
        eventPublisher.publishEvent(new SubscriptionActivatedEvent(subscription.getId(), subscription.getUserId()));
        integrationEventPublisher.publishSubscriptionActivated(subscription.getUserId(), subscription.getId());
        return Optional.of(subscription);
    }

    @Override
    public Optional<Subscription> handle(CancelSubscriptionCommand command) {
        var subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));
        if (subscription.getStripeCustomerId() != null
                && !subscription.getStripeCustomerId().isBlank()
                && !isLocalSubscription(subscription.getStripeCustomerId())) {
            stripePaymentGatewayService.cancelActiveSubscriptions(subscription.getStripeCustomerId());
        }
        subscription.cancel();
        var saved = subscriptionRepository.save(subscription);
        eventPublisher.publishEvent(new SubscriptionCancelledEvent(saved.getId(), saved.getUserId()));
        return Optional.of(saved);
    }

    @Override
    public Optional<Subscription> handle(RenewSubscriptionCommand command) {
        var subscription = subscriptionRepository.findById(command.subscriptionId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.subscriptionId()));
        var plan = subscription.getPlan();
        var newPeriodEnd = subscription.getCurrentPeriodEnd().plusMonths(1);
        subscription.renew(newPeriodEnd);
        var saved = subscriptionRepository.save(subscription);
        if (plan.getPrice().signum() > 0 && !isLocalSubscription(saved.getStripeCustomerId())) {
            var paymentIntentId = stripePaymentGatewayService.createPaymentIntent(subscription.getUserId(), plan);
            transactionRepository.save(new Transaction(saved.getUserId(), plan.getPrice(), plan.getCurrency(), paymentIntentId, "SUCCEEDED"));
        }
        invoiceRepository.save(new Invoice(saved.getUserId(), saved.getId(), plan.getPrice(), plan.getCurrency(), InvoiceStatus.PAID));
        eventPublisher.publishEvent(new SubscriptionRenewedEvent(saved.getId(), saved.getUserId()));
        return Optional.of(saved);
    }

    private boolean isLocalSubscription(String stripeCustomerId) {
        return stripeCustomerId.startsWith("free-local-user-") || stripeCustomerId.startsWith("mock-local-user-");
    }
}

