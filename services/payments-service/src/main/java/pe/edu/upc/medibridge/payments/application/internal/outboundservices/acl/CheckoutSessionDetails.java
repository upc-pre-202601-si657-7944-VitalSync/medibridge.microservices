package pe.edu.upc.medibridge.payments.application.internal.outboundservices.acl;

import java.util.Map;

public record CheckoutSessionDetails(
        String id,
        String status,
        String paymentStatus,
        String customerId,
        Map<String, String> metadata) {
}
