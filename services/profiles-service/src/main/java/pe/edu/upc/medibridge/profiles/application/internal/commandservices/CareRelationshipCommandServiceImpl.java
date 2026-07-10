package pe.edu.upc.medibridge.profiles.application.internal.commandservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.DoctorPatientAssignment;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.FamilyPatientLink;
import pe.edu.upc.medibridge.profiles.domain.model.commands.AssignDoctorToPatientCommand;
import pe.edu.upc.medibridge.profiles.domain.model.commands.LinkFamilyMemberToPatientCommand;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.DuplicateCareRelationshipException;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.InvalidProfileRequestException;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.ProfileNotFoundException;
import pe.edu.upc.medibridge.profiles.domain.services.CareRelationshipCommandService;
import pe.edu.upc.medibridge.profiles.infrastructure.messaging.publishers.ProfileIntegrationEventPublisher;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.DoctorPatientAssignmentRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.FamilyPatientLinkRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.PatientProfileRepository;

import java.util.Optional;

@Service
public class CareRelationshipCommandServiceImpl implements CareRelationshipCommandService {
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorPatientAssignmentRepository doctorPatientAssignmentRepository;
    private final FamilyPatientLinkRepository familyPatientLinkRepository;
    private final ProfileIntegrationEventPublisher integrationEventPublisher;
    private final CareRelationshipEligibilityPolicy careRelationshipEligibilityPolicy;

    public CareRelationshipCommandServiceImpl(
            PatientProfileRepository patientProfileRepository,
            DoctorPatientAssignmentRepository doctorPatientAssignmentRepository,
            FamilyPatientLinkRepository familyPatientLinkRepository,
            ProfileIntegrationEventPublisher integrationEventPublisher,
            CareRelationshipEligibilityPolicy careRelationshipEligibilityPolicy) {
        this.patientProfileRepository = patientProfileRepository;
        this.doctorPatientAssignmentRepository = doctorPatientAssignmentRepository;
        this.familyPatientLinkRepository = familyPatientLinkRepository;
        this.integrationEventPublisher = integrationEventPublisher;
        this.careRelationshipEligibilityPolicy = careRelationshipEligibilityPolicy;
    }

    @Override
    @Transactional
    public Optional<DoctorPatientAssignment> handle(AssignDoctorToPatientCommand command) {
        validatePositiveId(command.patientId(), "Patient id is required");
        validatePositiveId(command.doctorProfileId(), "Doctor profile id is required");
        validatePositiveId(command.requestedByUserId(), "Authenticated user id is required");
        ensurePatientExists(command.patientId());
        var doctorProfile = careRelationshipEligibilityPolicy.resolveOwnedDoctor(
                command.doctorProfileId(),
                command.requestedByUserId());

        if (doctorPatientAssignmentRepository.existsByDoctorProfileIdAndPatientIdAndActiveTrue(
                command.doctorProfileId(),
                command.patientId())) {
            throw new DuplicateCareRelationshipException("Doctor is already assigned to patient");
        }

        careRelationshipEligibilityPolicy.requireDoctorAssignmentCapacity(doctorProfile);

        var assignment = doctorPatientAssignmentRepository.save(new DoctorPatientAssignment(command));
        integrationEventPublisher.publishDoctorAssignedToPatient(
                assignment.getId(),
                assignment.getDoctorProfileId(),
                assignment.getPatientId());
        return Optional.of(assignment);
    }

    @Override
    @Transactional
    public Optional<FamilyPatientLink> handle(LinkFamilyMemberToPatientCommand command) {
        validatePositiveId(command.patientId(), "Patient id is required");
        validatePositiveId(command.familyMemberProfileId(), "Family member profile id is required");
        validatePositiveId(command.requestedByUserId(), "Authenticated user id is required");
        ensurePatientExists(command.patientId());
        var familyMemberProfile = careRelationshipEligibilityPolicy.resolveOwnedFamilyMember(
                command.familyMemberProfileId(),
                command.requestedByUserId());

        if (familyPatientLinkRepository.existsByFamilyMemberProfileIdAndPatientIdAndActiveTrue(
                command.familyMemberProfileId(),
                command.patientId())) {
            throw new DuplicateCareRelationshipException("Family member is already linked to patient");
        }

        careRelationshipEligibilityPolicy.requireFamilyLinkCapacity(familyMemberProfile);

        var link = familyPatientLinkRepository.save(new FamilyPatientLink(command));
        integrationEventPublisher.publishFamilyMemberAssignedToPatient(
                link.getId(),
                link.getFamilyMemberProfileId(),
                link.getPatientId());
        return Optional.of(link);
    }

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new InvalidProfileRequestException(message);
        }
    }

    private void ensurePatientExists(Long patientId) {
        if (!patientProfileRepository.existsById(patientId)) {
            throw new ProfileNotFoundException("Patient profile", patientId);
        }
    }

}

