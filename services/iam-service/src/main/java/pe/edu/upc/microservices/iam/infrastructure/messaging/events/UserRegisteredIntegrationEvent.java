package pe.edu.upc.microservices.iam.infrastructure.messaging.events;

import java.time.Instant;

public record UserRegisteredIntegrationEvent(
        Long userId,
        String username,
        Instant occurredAt,
        int version
) {
    public UserRegisteredIntegrationEvent(Long userId, String username) {
        this(userId, username, Instant.now(), 1);
    }
}
