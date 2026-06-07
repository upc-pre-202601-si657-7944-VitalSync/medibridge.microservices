package pe.edu.upc.microservices.iam.domain.model.commands;

public record SignInCommand(String username, String password) {
}
