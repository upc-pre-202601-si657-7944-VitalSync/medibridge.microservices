package pe.edu.upc.medibridge.appointments.interfaces.rest.acl;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pe.edu.upc.medibridge.appointments.application.internal.outboundservices.acl.ExternalProfilesContextService;
import pe.edu.upc.medibridge.appointments.domain.model.entities.PatientReference;
import pe.edu.upc.medibridge.appointments.infrastructure.acl.ProfilesServiceClient;
import pe.edu.upc.medibridge.appointments.infrastructure.acl.resources.PatientProfileResponse;
import pe.edu.upc.medibridge.appointments.infrastructure.persistence.jpa.repositories.DoctorPatientRelationRepository;
import pe.edu.upc.medibridge.appointments.infrastructure.persistence.jpa.repositories.FamilyPatientRelationRepository;
import pe.edu.upc.medibridge.appointments.infrastructure.persistence.jpa.repositories.PatientReferenceRepository;

@Service
public class ProfilesContextFacade implements ExternalProfilesContextService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilesContextFacade.class);

    private final PatientReferenceRepository patientReferenceRepository;
    private final DoctorPatientRelationRepository doctorPatientRelationRepository;
    private final FamilyPatientRelationRepository familyPatientRelationRepository;
    private final ProfilesServiceClient profilesServiceClient;

    public ProfilesContextFacade(
            PatientReferenceRepository patientReferenceRepository,
            DoctorPatientRelationRepository doctorPatientRelationRepository,
            FamilyPatientRelationRepository familyPatientRelationRepository,
            ProfilesServiceClient profilesServiceClient) {
        this.patientReferenceRepository = patientReferenceRepository;
        this.doctorPatientRelationRepository = doctorPatientRelationRepository;
        this.familyPatientRelationRepository = familyPatientRelationRepository;
        this.profilesServiceClient = profilesServiceClient;
    }

    @Override
    public boolean patientExists(Long patientId) {
        if (patientId == null) {
            return false;
        }
        if (patientReferenceRepository.existsByPatientIdAndActiveTrue(patientId)) {
            return true;
        }
        return synchronizePatientReferenceFromProfiles(patientId);
    }

    @Override
    public boolean familyMemberCanAccessPatient(Long familyMemberProfileId, Long patientId) {
        if (familyMemberProfileId == null || patientId == null) {
            return false;
        }
        if (familyPatientRelationRepository.existsByFamilyMemberProfileIdAndPatientIdAndActiveTrue(
                familyMemberProfileId,
                patientId)) {
            return true;
        }
        return familyMemberCanAccessPatientFromProfiles(familyMemberProfileId, patientId);
    }

    @Override
    public boolean doctorCanAttendPatient(Long doctorProfileId, Long patientId) {
        if (doctorProfileId == null || patientId == null) {
            return false;
        }
        if (doctorPatientRelationRepository.existsByDoctorProfileIdAndPatientIdAndActiveTrue(
                doctorProfileId,
                patientId)) {
            return true;
        }
        return doctorCanAttendPatientFromProfiles(doctorProfileId, patientId);
    }

    private boolean synchronizePatientReferenceFromProfiles(Long patientId) {
        try {
            if (!profilesServiceClient.patientExists(patientId)) {
                return false;
            }
            synchronizePatientReference(patientId);
            return true;
        } catch (FeignException.NotFound exception) {
            return false;
        } catch (FeignException exception) {
            LOGGER.warn(
                    "Could not verify patient {} against profiles-service. status={}",
                    patientId,
                    exception.status());
            return false;
        }
    }

    private void synchronizePatientReference(Long patientId) {
        try {
            var patientProfile = profilesServiceClient.getPatientProfileById(patientId);
            var fullName = resolvePatientFullName(patientId, patientProfile);

            patientReferenceRepository.findByPatientId(patientId)
                    .ifPresentOrElse(
                            reference -> {
                                reference.reactivate(fullName);
                                patientReferenceRepository.save(reference);
                            },
                            () -> patientReferenceRepository.save(new PatientReference(patientId, fullName)));
        } catch (FeignException exception) {
            LOGGER.warn(
                    "Patient {} exists in profiles-service, but appointments-service could not refresh its local reference. status={}",
                    patientId,
                    exception.status());
        }
    }

    private boolean doctorCanAttendPatientFromProfiles(Long doctorProfileId, Long patientId) {
        try {
            return profilesServiceClient.canDoctorAttendPatient(doctorProfileId, patientId);
        } catch (FeignException.NotFound exception) {
            return false;
        } catch (FeignException exception) {
            LOGGER.warn(
                    "Could not verify doctor {} access to patient {} against profiles-service. status={}",
                    doctorProfileId,
                    patientId,
                    exception.status());
            return false;
        }
    }

    private boolean familyMemberCanAccessPatientFromProfiles(Long familyMemberProfileId, Long patientId) {
        try {
            return profilesServiceClient.canFamilyMemberVisitPatient(familyMemberProfileId, patientId);
        } catch (FeignException.NotFound exception) {
            return false;
        } catch (FeignException exception) {
            LOGGER.warn(
                    "Could not verify family member {} access to patient {} against profiles-service. status={}",
                    familyMemberProfileId,
                    patientId,
                    exception.status());
            return false;
        }
    }

    private String resolvePatientFullName(Long patientId, PatientProfileResponse patientProfile) {
        if (patientProfile == null || patientProfile.fullName() == null || patientProfile.fullName().isBlank()) {
            return "Patient " + patientId;
        }
        return patientProfile.fullName();
    }
}

