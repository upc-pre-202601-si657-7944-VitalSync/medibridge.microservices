package pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.controllers;


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
import pe.edu.upc.medibridge.healthmonitoring.application.internal.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.healthmonitoring.application.internal.queryservices.PatientHealthSummaryQueryService;
import pe.edu.upc.medibridge.healthmonitoring.application.internal.queryservices.PremiumAccessService;
import pe.edu.upc.medibridge.healthmonitoring.domain.model.queries.GetActiveClinicalAlertsByPatientQuery;
import pe.edu.upc.medibridge.healthmonitoring.domain.model.queries.GetPatientHealthObservationsQuery;
import pe.edu.upc.medibridge.healthmonitoring.domain.services.ClinicalAlertQueryService;
import pe.edu.upc.medibridge.healthmonitoring.domain.services.HealthObservationCommandService;
import pe.edu.upc.medibridge.healthmonitoring.domain.services.HealthObservationQueryService;
import pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.acl.HealthMonitoringContextFacade;
import pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.resources.ClinicalAlertResource;
import pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.resources.PatientHealthObservationResource;
import pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.resources.PatientHealthSummaryResource;
import pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.resources.RecordPatientHealthObservationResource;
import pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.transform.ClinicalAlertResourceFromEntityAssembler;
import pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.transform.PatientHealthObservationResourceFromEntityAssembler;
import pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.transform.PatientHealthSummaryResourceFromDataAssembler;
import pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.transform.RecordPatientHealthObservationCommandFromResourceAssembler;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/health-monitoring/patients/{patientId}", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Health Monitoring", description = "Patient Health Monitoring Endpoints")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class HealthMonitoringController {

    private final HealthObservationCommandService healthObservationCommandService;
    private final HealthObservationQueryService healthObservationQueryService;
    private final ClinicalAlertQueryService clinicalAlertQueryService;
    private final HealthMonitoringContextFacade healthMonitoringContextFacade;
    private final PremiumAccessService premiumAccessService;
    private final AuthenticatedPatientAccessService authenticatedPatientAccessService;
    private final PatientHealthSummaryQueryService patientHealthSummaryQueryService;

    public HealthMonitoringController(
            HealthObservationCommandService healthObservationCommandService,
            HealthObservationQueryService healthObservationQueryService,
            ClinicalAlertQueryService clinicalAlertQueryService,
            HealthMonitoringContextFacade healthMonitoringContextFacade,
            PremiumAccessService premiumAccessService,
            AuthenticatedPatientAccessService authenticatedPatientAccessService,
            PatientHealthSummaryQueryService patientHealthSummaryQueryService) {
        this.healthObservationCommandService = healthObservationCommandService;
        this.healthObservationQueryService = healthObservationQueryService;
        this.clinicalAlertQueryService = clinicalAlertQueryService;
        this.healthMonitoringContextFacade = healthMonitoringContextFacade;
        this.premiumAccessService = premiumAccessService;
        this.authenticatedPatientAccessService = authenticatedPatientAccessService;
        this.patientHealthSummaryQueryService = patientHealthSummaryQueryService;
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping(value = "/observations", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthObservationResource> recordHealthObservation(
            @PathVariable Long patientId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody RecordPatientHealthObservationResource resource) {
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var command = RecordPatientHealthObservationCommandFromResourceAssembler
                .toCommandFromResource(patientId, resource, requestedByUserId);
        var observation = healthObservationCommandService.handle(command);

        if (observation.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        var observationResource = PatientHealthObservationResourceFromEntityAssembler
                .toResourceFromEntity(observation.get());
        return new ResponseEntity<>(observationResource, HttpStatus.CREATED);
    }

    @GetMapping("/observations")
    public ResponseEntity<List<PatientHealthObservationResource>> getPatientHealthObservations(
            @PathVariable Long patientId,
            @AuthenticationPrincipal Jwt jwt) {
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var observations = healthObservationQueryService.handle(new GetPatientHealthObservationsQuery(patientId, requestedByUserId));
        var resources = observations.stream()
                .map(PatientHealthObservationResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/alerts/active")
    public ResponseEntity<List<ClinicalAlertResource>> getActiveClinicalAlerts(
            @PathVariable Long patientId,
            @AuthenticationPrincipal Jwt jwt) {
        premiumAccessService.requirePaidSubscription(jwt, "advanced clinical alerts");
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var alerts = clinicalAlertQueryService.handle(new GetActiveClinicalAlertsByPatientQuery(patientId, requestedByUserId));
        var resources = alerts.stream()
                .map(ClinicalAlertResourceFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/summary")
    public ResponseEntity<PatientHealthSummaryResource> getPatientHealthSummary(
            @PathVariable Long patientId,
            @AuthenticationPrincipal Jwt jwt) {
        premiumAccessService.requirePaidSubscription(jwt, "patient health summaries");
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var summary = patientHealthSummaryQueryService.getSummary(patientId, requestedByUserId);
        var observations = healthObservationQueryService.handle(new GetPatientHealthObservationsQuery(patientId, requestedByUserId));
        var activeAlerts = clinicalAlertQueryService.handle(new GetActiveClinicalAlertsByPatientQuery(patientId, requestedByUserId));
        return ResponseEntity.ok(PatientHealthSummaryResourceFromDataAssembler.toResourceFromData(
                patientId,
                summary,
                observations,
                activeAlerts.size()));
    }
}
