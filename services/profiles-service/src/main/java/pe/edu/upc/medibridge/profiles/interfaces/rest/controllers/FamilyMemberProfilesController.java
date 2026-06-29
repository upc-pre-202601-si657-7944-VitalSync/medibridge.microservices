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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.medibridge.profiles.application.internal.outboundservices.acl.ExternalIamContextService;
import pe.edu.upc.medibridge.profiles.domain.model.exceptions.InvalidProfileRequestException;
import pe.edu.upc.medibridge.profiles.domain.model.queries.GetFamilyMemberProfileByIdQuery;
import pe.edu.upc.medibridge.profiles.domain.services.FamilyMemberProfileCommandService;
import pe.edu.upc.medibridge.profiles.domain.services.FamilyMemberProfileQueryService;
import pe.edu.upc.medibridge.profiles.interfaces.rest.resources.CreateFamilyMemberProfileResource;
import pe.edu.upc.medibridge.profiles.interfaces.rest.resources.FamilyMemberProfileResource;
import pe.edu.upc.medibridge.profiles.interfaces.rest.transform.CreateFamilyMemberProfileCommandFromResourceAssembler;
import pe.edu.upc.medibridge.profiles.interfaces.rest.transform.FamilyMemberProfileResourceFromEntityAssembler;

@RestController
@RequestMapping(value = "/api/v1/profiles/family-members", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Family Member Profiles", description = "Family Member Profile Management Endpoints")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class FamilyMemberProfilesController {

    private final FamilyMemberProfileCommandService familyMemberProfileCommandService;
    private final FamilyMemberProfileQueryService familyMemberProfileQueryService;
    private final ExternalIamContextService externalIamContextService;

    public FamilyMemberProfilesController(
            FamilyMemberProfileCommandService familyMemberProfileCommandService,
            FamilyMemberProfileQueryService familyMemberProfileQueryService,
            ExternalIamContextService externalIamContextService) {
        this.familyMemberProfileCommandService = familyMemberProfileCommandService;
        this.familyMemberProfileQueryService = familyMemberProfileQueryService;
        this.externalIamContextService = externalIamContextService;
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FamilyMemberProfileResource> createFamilyMemberProfile(
            @RequestBody CreateFamilyMemberProfileResource resource,
            @AuthenticationPrincipal Jwt jwt) {
        var userId = resolveAuthenticatedUserId(jwt);
        var command = CreateFamilyMemberProfileCommandFromResourceAssembler.toCommandFromResource(resource, userId);
        var familyMemberProfile = familyMemberProfileCommandService.handle(command);

        if (familyMemberProfile.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var familyMemberProfileResource = FamilyMemberProfileResourceFromEntityAssembler
                .toResourceFromEntity(familyMemberProfile.get());
        return new ResponseEntity<>(familyMemberProfileResource, HttpStatus.CREATED);
    }

    @GetMapping("/{familyMemberProfileId}")
    public ResponseEntity<FamilyMemberProfileResource> getFamilyMemberProfileById(
            @PathVariable Long familyMemberProfileId) {
        var familyMemberProfile = familyMemberProfileQueryService.handle(
                new GetFamilyMemberProfileByIdQuery(familyMemberProfileId));

        if (familyMemberProfile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var familyMemberProfileResource = FamilyMemberProfileResourceFromEntityAssembler
                .toResourceFromEntity(familyMemberProfile.get());
        return ResponseEntity.ok(familyMemberProfileResource);
    }

    private Long resolveAuthenticatedUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new InvalidProfileRequestException("Authenticated user is required");
        }
        return externalIamContextService.findUserIdByUsername(jwt.getSubject())
                .orElseThrow(() -> new InvalidProfileRequestException("Authenticated user was not found"));
    }
}
