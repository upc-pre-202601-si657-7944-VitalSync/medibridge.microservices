package pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.DoseAdministrationRepository;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationRepository;
import pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.resources.MedicationSummaryResource;

import java.time.LocalDate;

@RestController
@RequestMapping(value = "/api/v1/internal/medications", produces = MediaType.APPLICATION_JSON_VALUE)
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class MedicationInternalController {
    private final MedicationRepository medicationRepository;
    private final DoseAdministrationRepository doseAdministrationRepository;

    public MedicationInternalController(
            MedicationRepository medicationRepository,
            DoseAdministrationRepository doseAdministrationRepository) {
        this.medicationRepository = medicationRepository;
        this.doseAdministrationRepository = doseAdministrationRepository;
    }

    @GetMapping("/patients/{patientId}/summary")
    public ResponseEntity<MedicationSummaryResource> getMedicationSummary(
            @PathVariable Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        var activeMedications = medicationRepository.findByPatientIdAndActiveTrue(patientId);
        var lowStockMedications = activeMedications.stream()
                .filter(medication -> medication.getStockQuantity() <= medication.getLowStockThreshold())
                .toList();
        var doseAdministrations = startDate != null && endDate != null
                ? doseAdministrationRepository.countByPatientIdAndOccurredAtBetween(
                patientId,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay())
                : doseAdministrationRepository.countByPatientId(patientId);

        return ResponseEntity.ok(new MedicationSummaryResource(
                patientId,
                activeMedications.size(),
                lowStockMedications.size(),
                doseAdministrations));
    }
}
