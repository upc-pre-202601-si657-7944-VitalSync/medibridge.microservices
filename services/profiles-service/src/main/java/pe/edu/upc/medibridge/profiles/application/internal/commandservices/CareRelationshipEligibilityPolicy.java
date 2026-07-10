package pe.edu.upc.medibridge.profiles.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.medibridge.profiles.application.internal.outboundservices.acl.ExternalSubscriptionService;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.DoctorProfile;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.FamilyMemberProfile;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.ActiveSubscriptionRequiredException;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.InvalidProfileRequestException;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.PatientLimitExceededException;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.ProfileNotFoundException;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.ProfileOwnershipRequiredException;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.DoctorPatientAssignmentRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.DoctorProfileRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.FamilyMemberProfileRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.FamilyPatientLinkRepository;

@Service
public class CareRelationshipEligibilityPolicy {
    private static final int FAMILY_FREE_MAX_PATIENTS = 1;
    private static final String COMMERCIAL_LINE_FAMILY = "FAMILY";
    private static final String COMMERCIAL_LINE_INSTITUTION = "INSTITUTION";

    private final DoctorProfileRepository doctorProfileRepository;
    private final FamilyMemberProfileRepository familyMemberProfileRepository;
    private final DoctorPatientAssignmentRepository doctorPatientAssignmentRepository;
    private final FamilyPatientLinkRepository familyPatientLinkRepository;
    private final ExternalSubscriptionService externalSubscriptionService;

    public CareRelationshipEligibilityPolicy(
            DoctorProfileRepository doctorProfileRepository,
            FamilyMemberProfileRepository familyMemberProfileRepository,
            DoctorPatientAssignmentRepository doctorPatientAssignmentRepository,
            FamilyPatientLinkRepository familyPatientLinkRepository,
            ExternalSubscriptionService externalSubscriptionService) {
        this.doctorProfileRepository = doctorProfileRepository;
        this.familyMemberProfileRepository = familyMemberProfileRepository;
        this.doctorPatientAssignmentRepository = doctorPatientAssignmentRepository;
        this.familyPatientLinkRepository = familyPatientLinkRepository;
        this.externalSubscriptionService = externalSubscriptionService;
    }

    public DoctorProfile resolveAuthenticatedDoctor(Long requestedByUserId) {
        validatePositiveId(requestedByUserId, "Authenticated user id is required");
        var doctorProfile = doctorProfileRepository.findByUserId(requestedByUserId)
                .orElseThrow(() -> new ProfileNotFoundException("Doctor profile for user", requestedByUserId));
        ensureProfileBelongsToUser(
                doctorProfile.getUserId(),
                requestedByUserId,
                "Doctor profile does not belong to authenticated user");
        return doctorProfile;
    }

    public DoctorProfile resolveOwnedDoctor(Long doctorProfileId, Long requestedByUserId) {
        validatePositiveId(doctorProfileId, "Doctor profile id is required");
        validatePositiveId(requestedByUserId, "Authenticated user id is required");
        var doctorProfile = doctorProfileRepository.findById(doctorProfileId)
                .orElseThrow(() -> new ProfileNotFoundException("Doctor profile", doctorProfileId));
        ensureProfileBelongsToUser(
                doctorProfile.getUserId(),
                requestedByUserId,
                "Doctor profile does not belong to authenticated user");
        return doctorProfile;
    }

    public FamilyMemberProfile resolveOwnedFamilyMember(Long familyMemberProfileId, Long requestedByUserId) {
        validatePositiveId(familyMemberProfileId, "Family member profile id is required");
        validatePositiveId(requestedByUserId, "Authenticated user id is required");
        var familyMemberProfile = familyMemberProfileRepository.findById(familyMemberProfileId)
                .orElseThrow(() -> new ProfileNotFoundException("Family member profile", familyMemberProfileId));
        ensureProfileBelongsToUser(
                familyMemberProfile.getUserId(),
                requestedByUserId,
                "Family member profile does not belong to authenticated user");
        return familyMemberProfile;
    }

    public void requireDoctorAssignmentCapacity(DoctorProfile doctorProfile) {
        var subscription = externalSubscriptionService.findActiveSubscriptionByUserId(doctorProfile.getUserId())
                .filter(activeSubscription -> COMMERCIAL_LINE_INSTITUTION.equals(activeSubscription.commercialLine()))
                .orElseThrow(() -> new ActiveSubscriptionRequiredException(
                        "An active institutional subscription is required to assign patients to a doctor"));

        enforceMaxPatients(
                doctorPatientAssignmentRepository.countActivePatientsByDoctorProfileId(doctorProfile.getId()),
                subscription.maxPatients(),
                "Doctor patient limit reached for current institutional plan");
    }

    public void requireFamilyLinkCapacity(FamilyMemberProfile familyMemberProfile) {
        var maxPatients = externalSubscriptionService.findActiveSubscriptionByUserId(familyMemberProfile.getUserId())
                .filter(activeSubscription -> COMMERCIAL_LINE_FAMILY.equals(activeSubscription.commercialLine()))
                .map(ExternalSubscriptionService.ActiveSubscription::maxPatients)
                .orElse(FAMILY_FREE_MAX_PATIENTS);

        enforceMaxPatients(
                familyPatientLinkRepository.countActivePatientsByFamilyMemberProfileId(familyMemberProfile.getId()),
                maxPatients,
                "Family member patient limit reached for current plan");
    }

    private void validatePositiveId(Long id, String message) {
        if (id == null || id <= 0) {
            throw new InvalidProfileRequestException(message);
        }
    }

    private void enforceMaxPatients(long currentActivePatients, Integer maxPatients, String message) {
        if (maxPatients == null || maxPatients <= 0 || currentActivePatients >= maxPatients) {
            throw new PatientLimitExceededException(message);
        }
    }

    private void ensureProfileBelongsToUser(Long profileUserId, Long requestedByUserId, String message) {
        if (!profileUserId.equals(requestedByUserId)) {
            throw new ProfileOwnershipRequiredException(message);
        }
    }
}
