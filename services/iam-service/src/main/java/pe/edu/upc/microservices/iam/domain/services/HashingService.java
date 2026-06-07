package pe.edu.upc.microservices.iam.domain.services;

public interface HashingService {
    String encode(String rawPassword);
    boolean matches(String rawPassword, String encodedPassword);
}
