package pe.edu.upc.medibridge.reportsanalytics.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.medibridge.reportsanalytics.application.internal.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.reportsanalytics.application.internal.queryservices.PremiumAccessService;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.aggregates.ClinicalReport;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.commands.GeneratePdfReportCommand;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.queries.GetReportsByPatientQuery;
import pe.edu.upc.medibridge.reportsanalytics.domain.services.ClinicalReportCommandService;
import pe.edu.upc.medibridge.reportsanalytics.domain.services.ClinicalReportQueryService;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.pdf.ITextPdfReportGenerator;
import pe.edu.upc.medibridge.reportsanalytics.interfaces.rest.resources.ClinicalReportResponse;
import pe.edu.upc.medibridge.reportsanalytics.interfaces.rest.resources.GenerateReportRequest;
import pe.edu.upc.medibridge.reportsanalytics.interfaces.rest.transform.ClinicalReportResponseFromEntityAssembler;
import pe.edu.upc.medibridge.reportsanalytics.interfaces.rest.transform.GenerateReportCommandFromResourceAssembler;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/clinical-reports", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Clinical Reports", description = "Clinical Report Generation Endpoints")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "502", description = "Upstream service error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class ClinicalReportController {
    private final ClinicalReportCommandService clinicalReportCommandService;
    private final ClinicalReportQueryService clinicalReportQueryService;
    private final ITextPdfReportGenerator pdfReportGenerator;
    private final PremiumAccessService premiumAccessService;
    private final AuthenticatedPatientAccessService authenticatedPatientAccessService;

    public ClinicalReportController(
            ClinicalReportCommandService clinicalReportCommandService,
            ClinicalReportQueryService clinicalReportQueryService,
            ITextPdfReportGenerator pdfReportGenerator,
            PremiumAccessService premiumAccessService,
            AuthenticatedPatientAccessService authenticatedPatientAccessService) {
        this.clinicalReportCommandService = clinicalReportCommandService;
        this.clinicalReportQueryService = clinicalReportQueryService;
        this.pdfReportGenerator = pdfReportGenerator;
        this.premiumAccessService = premiumAccessService;
        this.authenticatedPatientAccessService = authenticatedPatientAccessService;
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping
    public ResponseEntity<ClinicalReportResponse> generateReport(
            @RequestBody GenerateReportRequest resource,
            @AuthenticationPrincipal Jwt jwt) {
        premiumAccessService.requirePaidSubscription(jwt, "clinical reports");
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var command = GenerateReportCommandFromResourceAssembler.toCommandFromResource(resource, requestedByUserId);
        var report = clinicalReportCommandService.handle(command);
        return report
                .map(value -> new ResponseEntity<>(
                        ClinicalReportResponseFromEntityAssembler.toResourceFromEntity(value),
                        HttpStatus.CREATED))
                .orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @ApiResponse(responseCode = "200", description = "PDF generated", content = @Content(
            mediaType = MediaType.APPLICATION_PDF_VALUE,
            schema = @Schema(type = "string", format = "binary")))
    @PostMapping(value = "/{reportId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> generatePdf(
            @PathVariable Integer reportId,
            @AuthenticationPrincipal Jwt jwt) {
        premiumAccessService.requirePaidSubscription(jwt, "PDF report generation");
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var report = clinicalReportCommandService.handle(new GeneratePdfReportCommand(reportId, requestedByUserId));
        return report
                .map(this::buildPdfDownloadResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @ApiResponse(responseCode = "200", description = "PDF downloaded", content = @Content(
            mediaType = MediaType.APPLICATION_PDF_VALUE,
            schema = @Schema(type = "string", format = "binary")))
    @GetMapping(value = "/{reportId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<Resource> downloadPdf(
            @PathVariable Integer reportId,
            @AuthenticationPrincipal Jwt jwt) {
        premiumAccessService.requirePaidSubscription(jwt, "PDF report downloads");
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var report = clinicalReportQueryService.handle(new GetReportByIdQuery(reportId, requestedByUserId));
        if (report.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return buildPdfDownloadResponse(report.get());
    }

    @GetMapping("/{reportId}")
    public ResponseEntity<ClinicalReportResponse> getReportById(
            @PathVariable Integer reportId,
            @AuthenticationPrincipal Jwt jwt) {
        premiumAccessService.requirePaidSubscription(jwt, "clinical reports");
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var report = clinicalReportQueryService.handle(new GetReportByIdQuery(reportId, requestedByUserId));
        return report
                .map(value -> ResponseEntity.ok(ClinicalReportResponseFromEntityAssembler.toResourceFromEntity(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/patients/{patientId}")
    public ResponseEntity<List<ClinicalReportResponse>> getReportsByPatient(
            @PathVariable Long patientId,
            @AuthenticationPrincipal Jwt jwt) {
        premiumAccessService.requirePaidSubscription(jwt, "clinical reports");
        var requestedByUserId = authenticatedPatientAccessService.resolveUserId(jwt);
        var reports = clinicalReportQueryService.handle(new GetReportsByPatientQuery(patientId, requestedByUserId));
        var resources = reports.stream()
                .map(ClinicalReportResponseFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }

    private ResponseEntity<Resource> buildPdfDownloadResponse(ClinicalReport clinicalReport) {
        var pdf = pdfReportGenerator.generate(clinicalReport);
        var filename = "reporte-clinico-" + clinicalReport.getId() + ".pdf";
        var resource = new ByteArrayResource(pdf);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(resource);
    }
}
