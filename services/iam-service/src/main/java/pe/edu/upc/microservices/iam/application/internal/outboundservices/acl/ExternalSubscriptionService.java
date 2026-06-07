package pe.edu.upc.microservices.iam.application.internal.outboundservices.acl;

public interface ExternalSubscriptionService {
    boolean hasActiveSubscription(Long userId);
}
