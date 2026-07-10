package pe.edu.upc.medibridge.profiles.domain.model.commands;

public record CreateAssignedPatientProfileCommand(String fullName, Long requestedByUserId) {
}
