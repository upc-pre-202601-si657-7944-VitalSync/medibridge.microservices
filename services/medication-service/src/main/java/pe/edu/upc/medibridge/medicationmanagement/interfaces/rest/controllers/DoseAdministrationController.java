package pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.controllers;


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
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.medibridge.medicationmanagement.application.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.exceptions.MedicationNotFoundException;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.queries.GetDoseAdministrationHistoryQuery;
import pe.edu.upc.medibridge.medicationmanagement.domain.services.DoseAdministrationCommandService;
import pe.edu.upc.medibridge.medicationmanagement.domain.services.DoseAdministrationQueryService;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationRepository;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.resources.DoseAdministrationResponse;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.resources.RecordDoseAdministrationRequest;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.resources.SkipDoseRequest;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.transform.DoseAdministrationResponseFromEntityAssembler;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.transform.RecordDoseAdministrationCommandFromResourceAssembler;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.transform.SkipDoseCommandFromResourceAssembler;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/dose-administrations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Dose Administrations", description = "Dose Administration Management Endpoints")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class DoseAdministrationController {
    private final DoseAdministrationCommandService doseAdministrationCommandService;
    private final DoseAdministrationQueryService doseAdministrationQueryService;
    private final AuthenticatedPatientAccessService authenticatedPatientAccessService;
    private final MedicationRepository medicationRepository;

    public DoseAdministrationController(
            DoseAdministrationCommandService doseAdministrationCommandService,
            DoseAdministrationQueryService doseAdministrationQueryService,
            AuthenticatedPatientAccessService authenticatedPatientAccessService,
            MedicationRepository medicationRepository) {
        this.doseAdministrationCommandService = doseAdministrationCommandService;
        this.doseAdministrationQueryService = doseAdministrationQueryService;
        this.authenticatedPatientAccessService = authenticatedPatientAccessService;
        this.medicationRepository = medicationRepository;
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping
    public ResponseEntity<DoseAdministrationResponse> recordDoseAdministration(
            @RequestBody RecordDoseAdministrationRequest resource,
            @AuthenticationPrincipal Jwt jwt) {
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var command = RecordDoseAdministrationCommandFromResourceAssembler.toCommandFromResource(resource, requestedByUserId);
        var doseAdministration = doseAdministrationCommandService.handle(command);
        return doseAdministration
                .map(value -> new ResponseEntity<>(
                        DoseAdministrationResponseFromEntityAssembler.toResourceFromEntity(value),
                        HttpStatus.CREATED))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping("/skip")
    public ResponseEntity<DoseAdministrationResponse> skipDose(
            @RequestBody SkipDoseRequest resource,
            @AuthenticationPrincipal Jwt jwt) {
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var command = SkipDoseCommandFromResourceAssembler.toCommandFromResource(resource, requestedByUserId);
        var doseAdministration = doseAdministrationCommandService.handle(command);
        return doseAdministration
                .map(value -> new ResponseEntity<>(
                        DoseAdministrationResponseFromEntityAssembler.toResourceFromEntity(value),
                        HttpStatus.CREATED))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping("/medications/{medicationId}")
    public ResponseEntity<List<DoseAdministrationResponse>> getDoseAdministrationHistory(
            @PathVariable Integer medicationId,
            @AuthenticationPrincipal Jwt jwt) {
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var doseAdministrations = doseAdministrationQueryService.handle(
                new GetDoseAdministrationHistoryQuery(medicationId, requestedByUserId));
        var resources = doseAdministrations.stream()
                .map(DoseAdministrationResponseFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
