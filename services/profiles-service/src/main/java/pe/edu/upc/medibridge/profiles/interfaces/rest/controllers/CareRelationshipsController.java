package pe.edu.upc.medibridge.profiles.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.medibridge.profiles.application.internal.outboundservices.acl.ExternalIamContextService;
import pe.edu.upc.medibridge.profiles.domain.model.commands.AssignDoctorToPatientCommand;
import pe.edu.upc.medibridge.profiles.domain.model.commands.LinkFamilyMemberToPatientCommand;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.InvalidProfileRequestException;
import pe.edu.upc.medibridge.profiles.domain.model.queries.GetCareTeamMembersByPatientIdQuery;
import pe.edu.upc.medibridge.profiles.domain.services.CareRelationshipCommandService;
import pe.edu.upc.medibridge.profiles.domain.services.CareRelationshipQueryService;
import pe.edu.upc.medibridge.profiles.interfaces.rest.resources.CareTeamMembersResource;
import pe.edu.upc.medibridge.profiles.interfaces.rest.resources.DoctorPatientAssignmentResource;
import pe.edu.upc.medibridge.profiles.interfaces.rest.resources.FamilyPatientLinkResource;
import pe.edu.upc.medibridge.profiles.interfaces.rest.transform.CareTeamMembersResourceFromValueObjectAssembler;
import pe.edu.upc.medibridge.profiles.interfaces.rest.transform.DoctorPatientAssignmentResourceFromEntityAssembler;
import pe.edu.upc.medibridge.profiles.interfaces.rest.transform.FamilyPatientLinkResourceFromEntityAssembler;

@RestController
@RequestMapping(value = "/api/v1/profiles/patients/{patientId}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Care Relationships", description = "Patient Care Relationship Endpoints")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class CareRelationshipsController {

    private final CareRelationshipCommandService careRelationshipCommandService;
    private final CareRelationshipQueryService careRelationshipQueryService;
    private final ExternalIamContextService externalIamContextService;

    public CareRelationshipsController(
            CareRelationshipCommandService careRelationshipCommandService,
            CareRelationshipQueryService careRelationshipQueryService,
            ExternalIamContextService externalIamContextService) {
        this.careRelationshipCommandService = careRelationshipCommandService;
        this.careRelationshipQueryService = careRelationshipQueryService;
        this.externalIamContextService = externalIamContextService;
    }

    @GetMapping("/care-team-members")
    public ResponseEntity<CareTeamMembersResource> getCareTeamMembers(
            @PathVariable Long patientId,
            @AuthenticationPrincipal Jwt jwt) {
        var userId = resolveAuthenticatedUserId(jwt);
        var members = careRelationshipQueryService.handle(new GetCareTeamMembersByPatientIdQuery(patientId));

        if (!members.careTeamUserIds().contains(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(CareTeamMembersResourceFromValueObjectAssembler.toResourceFromValueObject(members));
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping("/doctors/{doctorProfileId}")
    public ResponseEntity<DoctorPatientAssignmentResource> assignDoctorToPatient(
            @PathVariable Long patientId,
            @PathVariable Long doctorProfileId,
            @AuthenticationPrincipal Jwt jwt) {
        var userId = resolveAuthenticatedUserId(jwt);
        var assignment = careRelationshipCommandService.handle(
                new AssignDoctorToPatientCommand(doctorProfileId, patientId, userId));

        if (assignment.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var resource = DoctorPatientAssignmentResourceFromEntityAssembler
                .toResourceFromEntity(assignment.get());
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping("/family-members/{familyMemberProfileId}")
    public ResponseEntity<FamilyPatientLinkResource> linkFamilyMemberToPatient(
            @PathVariable Long patientId,
            @PathVariable Long familyMemberProfileId,
            @AuthenticationPrincipal Jwt jwt) {
        var userId = resolveAuthenticatedUserId(jwt);
        var link = careRelationshipCommandService.handle(
                new LinkFamilyMemberToPatientCommand(familyMemberProfileId, patientId, userId));

        if (link.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var resource = FamilyPatientLinkResourceFromEntityAssembler.toResourceFromEntity(link.get());
        return new ResponseEntity<>(resource, HttpStatus.CREATED);
    }

    private Long resolveAuthenticatedUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new InvalidProfileRequestException("Authenticated user is required");
        }
        return externalIamContextService.findUserIdByUsername(jwt.getSubject())
                .orElseThrow(() -> new InvalidProfileRequestException("Authenticated user was not found"));
    }
}
