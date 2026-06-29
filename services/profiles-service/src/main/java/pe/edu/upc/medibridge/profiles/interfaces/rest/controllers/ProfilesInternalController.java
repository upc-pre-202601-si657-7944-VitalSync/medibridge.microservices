package pe.edu.upc.medibridge.profiles.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.ProfileNotFoundException;
import pe.edu.upc.medibridge.profiles.domain.model.queries.CanDoctorAttendPatientQuery;
import pe.edu.upc.medibridge.profiles.domain.model.queries.CanFamilyMemberAccessPatientQuery;
import pe.edu.upc.medibridge.profiles.domain.model.queries.GetCareTeamMembersByPatientIdQuery;
import pe.edu.upc.medibridge.profiles.domain.model.queries.GetPatientProfileByIdQuery;
import pe.edu.upc.medibridge.profiles.domain.services.CareRelationshipQueryService;
import pe.edu.upc.medibridge.profiles.domain.services.PatientProfileQueryService;
import pe.edu.upc.medibridge.profiles.interfaces.rest.resources.CareTeamMembersResource;
import pe.edu.upc.medibridge.profiles.interfaces.rest.resources.PatientProfileResource;
import pe.edu.upc.medibridge.profiles.interfaces.rest.transform.CareTeamMembersResourceFromValueObjectAssembler;
import pe.edu.upc.medibridge.profiles.interfaces.rest.transform.PatientProfileResourceFromEntityAssembler;

@RestController
@RequestMapping("/api/v1/internal/profiles")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class ProfilesInternalController {
    private final PatientProfileQueryService patientProfileQueryService;
    private final CareRelationshipQueryService careRelationshipQueryService;

    public ProfilesInternalController(
            PatientProfileQueryService patientProfileQueryService,
            CareRelationshipQueryService careRelationshipQueryService) {
        this.patientProfileQueryService = patientProfileQueryService;
        this.careRelationshipQueryService = careRelationshipQueryService;
    }

    @GetMapping("/patients/{patientId}/exists")
    public boolean existsPatientById(@PathVariable Long patientId) {
        return patientId != null && patientProfileQueryService
                .handle(new GetPatientProfileByIdQuery(patientId))
                .isPresent();
    }

    @GetMapping("/patients/{patientId}")
    public PatientProfileResource getPatientProfileById(@PathVariable Long patientId) {
        return patientProfileQueryService.handle(new GetPatientProfileByIdQuery(patientId))
                .map(PatientProfileResourceFromEntityAssembler::toResourceFromEntity)
                .orElseThrow(() -> new ProfileNotFoundException("Patient profile", patientId));
    }

    @GetMapping("/patients/{patientId}/care-team-members")
    public CareTeamMembersResource getCareTeamMembersByPatientId(@PathVariable Long patientId) {
        if (patientId == null || patientProfileQueryService.handle(new GetPatientProfileByIdQuery(patientId)).isEmpty()) {
            throw new ProfileNotFoundException("Patient profile", patientId);
        }

        var members = careRelationshipQueryService.handle(new GetCareTeamMembersByPatientIdQuery(patientId));
        return CareTeamMembersResourceFromValueObjectAssembler.toResourceFromValueObject(members);
    }

    @GetMapping("/doctors/{doctorId}/can-attend/{patientId}")
    public boolean canDoctorAttendPatient(
            @PathVariable Long doctorId,
            @PathVariable Long patientId) {
        return careRelationshipQueryService.handle(new CanDoctorAttendPatientQuery(doctorId, patientId));
    }

    @GetMapping("/family-members/{familyMemberId}/can-visit/{patientId}")
    public boolean canFamilyMemberVisitPatient(
            @PathVariable Long familyMemberId,
            @PathVariable Long patientId) {
        return careRelationshipQueryService.handle(new CanFamilyMemberAccessPatientQuery(familyMemberId, patientId));
    }

    @GetMapping("/users/{userId}/can-access/{patientId}")
    public boolean canUserAccessPatient(
            @PathVariable Long userId,
            @PathVariable Long patientId) {
        if (userId == null || patientId == null) {
            return false;
        }
        var members = careRelationshipQueryService.handle(new GetCareTeamMembersByPatientIdQuery(patientId));
        return members.careTeamUserIds().contains(userId);
    }
}
