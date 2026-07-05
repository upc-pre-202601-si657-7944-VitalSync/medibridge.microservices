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
import pe.edu.upc.medibridge.profiles.domain.model.queries.GetPatientProfileByIdQuery;
import pe.edu.upc.medibridge.profiles.domain.services.PatientProfileCommandService;
import pe.edu.upc.medibridge.profiles.domain.services.PatientProfileQueryService;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.DoctorPatientAssignmentRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.DoctorProfileRepository;
import pe.edu.upc.medibridge.profiles.infrastructure.persistence.jpa.repositories.PatientProfileRepository;
import pe.edu.upc.medibridge.profiles.interfaces.rest.resources.CreatePatientProfileResource;
import pe.edu.upc.medibridge.profiles.interfaces.rest.resources.PatientProfileResource;
import pe.edu.upc.medibridge.profiles.interfaces.rest.transform.CreatePatientProfileCommandFromResourceAssembler;
import pe.edu.upc.medibridge.profiles.interfaces.rest.transform.PatientProfileResourceFromEntityAssembler;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/profiles/patients", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Patient Profiles", description = "Patient Profile Management Endpoints")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class PatientProfilesController {

    private final PatientProfileCommandService patientProfileCommandService;
    private final PatientProfileQueryService patientProfileQueryService;
    private final ExternalIamContextService externalIamContextService;
    private final DoctorProfileRepository doctorProfileRepository;
    private final DoctorPatientAssignmentRepository doctorPatientAssignmentRepository;
    private final PatientProfileRepository patientProfileRepository;

    public PatientProfilesController(
            PatientProfileCommandService patientProfileCommandService,
            PatientProfileQueryService patientProfileQueryService,
            ExternalIamContextService externalIamContextService,
            DoctorProfileRepository doctorProfileRepository,
            DoctorPatientAssignmentRepository doctorPatientAssignmentRepository,
            PatientProfileRepository patientProfileRepository) {
        this.patientProfileCommandService = patientProfileCommandService;
        this.patientProfileQueryService = patientProfileQueryService;
        this.externalIamContextService = externalIamContextService;
        this.doctorProfileRepository = doctorProfileRepository;
        this.doctorPatientAssignmentRepository = doctorPatientAssignmentRepository;
        this.patientProfileRepository = patientProfileRepository;
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientProfileResource> createPatientProfile(
            @RequestBody CreatePatientProfileResource resource) {
        var command = CreatePatientProfileCommandFromResourceAssembler.toCommandFromResource(resource);
        var patientProfile = patientProfileCommandService.handle(command);

        if (patientProfile.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var patientProfileResource = PatientProfileResourceFromEntityAssembler
                .toResourceFromEntity(patientProfile.get());
        return new ResponseEntity<>(patientProfileResource, HttpStatus.CREATED);
    }

    @GetMapping("/my-care-team")
    public ResponseEntity<List<PatientProfileResource>> getAuthenticatedDoctorPatients(@AuthenticationPrincipal Jwt jwt) {
        var userId = resolveAuthenticatedUserId(jwt);
        var doctorProfile = doctorProfileRepository.findByUserId(userId);
        if (doctorProfile.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        var patientIds = doctorPatientAssignmentRepository
                .findAllByDoctorProfileIdAndActiveTrue(doctorProfile.get().getId())
                .stream()
                .map(assignment -> assignment.getPatientId())
                .distinct()
                .toList();

        var resources = patientProfileRepository.findAllById(patientIds)
                .stream()
                .map(PatientProfileResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<PatientProfileResource> getPatientProfileById(@PathVariable Long patientId) {
        var patientProfile = patientProfileQueryService.handle(new GetPatientProfileByIdQuery(patientId));

        if (patientProfile.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var patientProfileResource = PatientProfileResourceFromEntityAssembler
                .toResourceFromEntity(patientProfile.get());
        return ResponseEntity.ok(patientProfileResource);
    }

    private Long resolveAuthenticatedUserId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new InvalidProfileRequestException("Authenticated user is required");
        }
        return externalIamContextService.findUserIdByUsername(jwt.getSubject())
                .orElseThrow(() -> new InvalidProfileRequestException("Authenticated user was not found"));
    }
}
