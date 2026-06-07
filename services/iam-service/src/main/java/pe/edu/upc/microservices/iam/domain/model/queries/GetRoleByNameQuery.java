package pe.edu.upc.microservices.iam.domain.model.queries;

import pe.edu.upc.microservices.iam.domain.model.valueobjects.Roles;

public record GetRoleByNameQuery(Roles name) {
}
