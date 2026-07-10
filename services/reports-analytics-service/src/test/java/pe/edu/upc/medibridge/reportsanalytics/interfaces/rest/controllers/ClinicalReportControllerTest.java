package pe.edu.upc.medibridge.reportsanalytics.interfaces.rest.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.edu.upc.medibridge.reportsanalytics.application.internal.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.reportsanalytics.application.internal.queryservices.PremiumAccessService;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.aggregates.ClinicalReport;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.medibridge.reportsanalytics.domain.services.ClinicalReportCommandService;
import pe.edu.upc.medibridge.reportsanalytics.domain.services.ClinicalReportQueryService;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.pdf.ITextPdfReportGenerator;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalReportControllerTest {

    @Mock
    private ClinicalReportCommandService clinicalReportCommandService;

    @Mock
    private ClinicalReportQueryService clinicalReportQueryService;

    @Mock
    private ITextPdfReportGenerator pdfReportGenerator;

    @Mock
    private PremiumAccessService premiumAccessService;

    @Mock
    private AuthenticatedPatientAccessService authenticatedPatientAccessService;

    @Test
    void downloadPdfReturnsAttachmentResponseWhenReportExists() throws IOException {
        var jwt = mock(Jwt.class);
        var report = mock(ClinicalReport.class);
        var pdf = "%PDF-1.5".getBytes();
        when(report.getId()).thenReturn(9);
        when(authenticatedPatientAccessService.resolveUserId(jwt)).thenReturn(33L);
        when(clinicalReportQueryService.handle(any(GetReportByIdQuery.class))).thenReturn(Optional.of(report));
        when(pdfReportGenerator.generate(report)).thenReturn(pdf);

        var controller = new ClinicalReportController(
                clinicalReportCommandService,
                clinicalReportQueryService,
                pdfReportGenerator,
                premiumAccessService,
                authenticatedPatientAccessService);
        var response = controller.downloadPdf(9, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("attachment")
                .contains("reporte-clinico-9.pdf");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().contentLength()).isEqualTo(pdf.length);
    }

    @Test
    void downloadPdfReturnsNotFoundWhenReportDoesNotExist() {
        var jwt = mock(Jwt.class);
        when(authenticatedPatientAccessService.resolveUserId(jwt)).thenReturn(33L);
        when(clinicalReportQueryService.handle(any(GetReportByIdQuery.class))).thenReturn(Optional.empty());

        var controller = new ClinicalReportController(
                clinicalReportCommandService,
                clinicalReportQueryService,
                pdfReportGenerator,
                premiumAccessService,
                authenticatedPatientAccessService);
        var response = controller.downloadPdf(99, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
