package pe.edu.upc.medibridge.profiles.domain.services;

import pe.edu.upc.medibridge.profiles.domain.model.aggregates.PatientProfile;
import pe.edu.upc.medibridge.profiles.domain.model.commands.CreateAssignedPatientProfileCommand;

import java.util.Optional;

public interface AssignedPatientProfileCommandService {
    Optional<PatientProfile> handle(CreateAssignedPatientProfileCommand command);
}
