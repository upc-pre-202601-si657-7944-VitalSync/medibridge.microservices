package pe.edu.upc.medibridge.medicationmanagement.domain.model.commands;

public record DeactivateMedicationCommand(
        Integer medicationId,
        Long requestedByUserId) {
}
