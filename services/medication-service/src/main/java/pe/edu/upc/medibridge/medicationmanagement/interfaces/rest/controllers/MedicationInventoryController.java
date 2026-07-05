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
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.UpdateMedicationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.UpdateMedicationStockCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.queries.GetLowStockMedicationsQuery;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.queries.GetMedicationByIdQuery;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.queries.GetMedicationsByPatientQuery;
import pe.edu.upc.medibridge.medicationmanagement.domain.services.MedicationInventoryCommandService;
import pe.edu.upc.medibridge.medicationmanagement.domain.services.MedicationInventoryQueryService;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.resources.LowStockAlertResponse;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.resources.MedicationResponse;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.resources.RegisterMedicationRequest;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.resources.UpdateMedicationRequest;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.resources.UpdateMedicationStockRequest;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.transform.MedicationResponseFromEntityAssembler;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.transform.RegisterMedicationCommandFromResourceAssembler;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/medications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Medication Inventory", description = "Medication Inventory Management Endpoints")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class MedicationInventoryController {
    private final MedicationInventoryCommandService medicationInventoryCommandService;
    private final MedicationInventoryQueryService medicationInventoryQueryService;
    private final AuthenticatedPatientAccessService authenticatedPatientAccessService;

    public MedicationInventoryController(
            MedicationInventoryCommandService medicationInventoryCommandService,
            MedicationInventoryQueryService medicationInventoryQueryService,
            AuthenticatedPatientAccessService authenticatedPatientAccessService) {
        this.medicationInventoryCommandService = medicationInventoryCommandService;
        this.medicationInventoryQueryService = medicationInventoryQueryService;
        this.authenticatedPatientAccessService = authenticatedPatientAccessService;
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping
    public ResponseEntity<MedicationResponse> registerMedication(
            @RequestBody RegisterMedicationRequest resource,
            @AuthenticationPrincipal Jwt jwt) {
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var command = RegisterMedicationCommandFromResourceAssembler.toCommandFromResource(resource, requestedByUserId);
        var medication = medicationInventoryCommandService.handle(command);
        return medication
                .map(value -> new ResponseEntity<>(
                        MedicationResponseFromEntityAssembler.toResourceFromEntity(value),
                        HttpStatus.CREATED))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping("/{medicationId}")
    public ResponseEntity<MedicationResponse> getMedicationById(
            @PathVariable Integer medicationId,
            @AuthenticationPrincipal Jwt jwt) {
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var medication = medicationInventoryQueryService.handle(new GetMedicationByIdQuery(medicationId, requestedByUserId));
        return medication
                .map(value -> ResponseEntity.ok(MedicationResponseFromEntityAssembler.toResourceFromEntity(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/patients/{patientId}")
    public ResponseEntity<List<MedicationResponse>> getMedicationsByPatient(
            @PathVariable Long patientId,
            @AuthenticationPrincipal Jwt jwt) {
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var medications = medicationInventoryQueryService.handle(new GetMedicationsByPatientQuery(patientId, requestedByUserId));
        var resources = medications.stream()
                .map(MedicationResponseFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @PatchMapping("/{medicationId}/stock")
    public ResponseEntity<MedicationResponse> updateMedicationStock(
            @PathVariable Integer medicationId,
            @RequestBody UpdateMedicationStockRequest resource,
            @AuthenticationPrincipal Jwt jwt) {
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var medication = medicationInventoryCommandService.handle(
                new UpdateMedicationStockCommand(medicationId, resource.stockQuantity(), requestedByUserId));
        return medication
                .map(value -> ResponseEntity.ok(MedicationResponseFromEntityAssembler.toResourceFromEntity(value)))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PatchMapping("/{medicationId}")
    public ResponseEntity<MedicationResponse> updateMedication(
            @PathVariable Integer medicationId,
            @RequestBody UpdateMedicationRequest resource,
            @AuthenticationPrincipal Jwt jwt) {
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var medication = medicationInventoryCommandService.handle(new UpdateMedicationCommand(
                medicationId,
                resource.name(),
                resource.dosageAmount(),
                resource.dosageUnit(),
                resource.administrationRoute(),
                resource.stockQuantity(),
                resource.lowStockThreshold(),
                resource.expirationDate(),
                requestedByUserId));
        return medication
                .map(value -> ResponseEntity.ok(MedicationResponseFromEntityAssembler.toResourceFromEntity(value)))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @GetMapping("/patients/{patientId}/low-stock")
    public ResponseEntity<List<LowStockAlertResponse>> getLowStockMedications(
            @PathVariable Long patientId,
            @AuthenticationPrincipal Jwt jwt) {
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var medications = medicationInventoryQueryService.handle(new GetLowStockMedicationsQuery(patientId, requestedByUserId));
        var resources = medications.stream()
                .map(MedicationResponseFromEntityAssembler::toLowStockResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
