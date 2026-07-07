package pe.edu.upc.medibridge.profiles.interfaces.rest.resources;

public record ChatContactResource(
        Long userId,
        String fullName,
        String contactType,
        Long profileId,
        Long patientId,
        String patientFullName) {
}
