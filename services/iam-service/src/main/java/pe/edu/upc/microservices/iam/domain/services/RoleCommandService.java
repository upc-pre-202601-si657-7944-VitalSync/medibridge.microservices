package pe.edu.upc.microservices.iam.domain.services;

import pe.edu.upc.microservices.iam.domain.model.commands.SeedRolesCommand;

public interface RoleCommandService {
    void handle(SeedRolesCommand command);
}
