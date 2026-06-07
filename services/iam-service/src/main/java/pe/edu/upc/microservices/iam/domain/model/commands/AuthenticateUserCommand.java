package pe.edu.upc.microservices.iam.domain.model.commands;

public record AuthenticateUserCommand(String username, String password) {
}
