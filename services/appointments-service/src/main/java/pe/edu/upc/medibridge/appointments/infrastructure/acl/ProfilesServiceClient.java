package pe.edu.upc.medibridge.appointments.infrastructure.acl;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import pe.edu.upc.medibridge.appointments.infrastructure.acl.resources.PatientProfileResponse;

@FeignClient(name = "profiles-service", url = "${services.profiles.url}")
public interface ProfilesServiceClient {
    @GetMapping("/api/v1/internal/profiles/patients/{patientId}/exists")
    boolean patientExists(@PathVariable("patientId") Long patientId);

    @GetMapping("/api/v1/internal/profiles/patients/{patientId}")
    PatientProfileResponse getPatientProfileById(@PathVariable("patientId") Long patientId);

    @GetMapping("/api/v1/internal/profiles/doctors/{doctorId}/can-attend/{patientId}")
    boolean canDoctorAttendPatient(
            @PathVariable("doctorId") Long doctorId,
            @PathVariable("patientId") Long patientId);

    @GetMapping("/api/v1/internal/profiles/family-members/{familyMemberId}/can-visit/{patientId}")
    boolean canFamilyMemberVisitPatient(
            @PathVariable("familyMemberId") Long familyMemberId,
            @PathVariable("patientId") Long patientId);

    @GetMapping("/api/v1/internal/profiles/users/{userId}/can-access/{patientId}")
    boolean canUserAccessPatient(
            @PathVariable("userId") Long userId,
            @PathVariable("patientId") Long patientId);
}

