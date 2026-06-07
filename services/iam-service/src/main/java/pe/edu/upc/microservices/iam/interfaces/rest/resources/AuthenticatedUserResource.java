package pe.edu.upc.microservices.iam.interfaces.rest.resources;

public record AuthenticatedUserResource(Long id, String username, String token) {
}
