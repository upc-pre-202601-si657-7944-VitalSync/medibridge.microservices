package pe.edu.upc.medibridge.profiles.application.internal.commandservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.PatientProfile;
import pe.edu.upc.medibridge.profiles.domain.model.commands.AssignDoctorToPatientCommand;
import pe.edu.upc.medibridge.profiles.domain.model.commands.CreateAssignedPatientProfileCommand;
import pe.edu.upc.medibridge.profiles.domain.model.commands.CreatePatientProfileCommand;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.InvalidProfileRequestException;
import pe.edu.upc.medibridge.profiles.domain.services.AssignedPatientProfileCommandService;
import pe.edu.upc.medibridge.profiles.domain.services.CareRelationshipCommandService;
import pe.edu.upc.medibridge.profiles.domain.services.PatientProfileCommandService;

import java.util.Optional;

@Service
public class AssignedPatientProfileCommandServiceImpl implements AssignedPatientProfileCommandService {
    private final PatientProfileCommandService patientProfileCommandService;
    private final CareRelationshipCommandService careRelationshipCommandService;
    private final CareRelationshipEligibilityPolicy careRelationshipEligibilityPolicy;

    public AssignedPatientProfileCommandServiceImpl(
            PatientProfileCommandService patientProfileCommandService,
            CareRelationshipCommandService careRelationshipCommandService,
            CareRelationshipEligibilityPolicy careRelationshipEligibilityPolicy) {
        this.patientProfileCommandService = patientProfileCommandService;
        this.careRelationshipCommandService = careRelationshipCommandService;
        this.careRelationshipEligibilityPolicy = careRelationshipEligibilityPolicy;
    }

    @Override
    @Transactional
    public Optional<PatientProfile> handle(CreateAssignedPatientProfileCommand command) {
        validateFullName(command.fullName());

        var doctorProfile = careRelationshipEligibilityPolicy.resolveAuthenticatedDoctor(command.requestedByUserId());
        careRelationshipEligibilityPolicy.requireDoctorAssignmentCapacity(doctorProfile);

        var patientProfile = patientProfileCommandService
                .handle(new CreatePatientProfileCommand(command.fullName()))
                .orElseThrow(() -> new InvalidProfileRequestException("Patient profile could not be created"));

        careRelationshipCommandService.handle(new AssignDoctorToPatientCommand(
                        doctorProfile.getId(),
                        patientProfile.getId(),
                        command.requestedByUserId()))
                .orElseThrow(() -> new InvalidProfileRequestException("Doctor patient assignment could not be created"));

        return Optional.of(patientProfile);
    }

    private void validateFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new InvalidProfileRequestException("Patient full name is required");
        }
    }
}
