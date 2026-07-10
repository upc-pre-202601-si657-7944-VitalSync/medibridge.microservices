package pe.edu.upc.medibridge.reportsanalytics.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.medibridge.reportsanalytics.application.internal.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.reportsanalytics.application.internal.outboundservices.acl.ExternalAppointmentService;
import pe.edu.upc.medibridge.reportsanalytics.application.internal.outboundservices.acl.ExternalHealthMonitoringService;
import pe.edu.upc.medibridge.reportsanalytics.application.internal.outboundservices.acl.ExternalMedicationService;
import pe.edu.upc.medibridge.reportsanalytics.application.internal.outboundservices.acl.ExternalPatientProfileService;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.aggregates.ClinicalReport;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.commands.GenerateClinicalReportCommand;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.commands.GeneratePdfReportCommand;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.entities.ReportSection;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.exceptions.InvalidPatientReferenceException;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.exceptions.ReportNotFoundException;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.valueobjects.ReportType;
import pe.edu.upc.medibridge.reportsanalytics.domain.services.ClinicalReportCommandService;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.messaging.publishers.ReportsAnalyticsIntegrationEventPublisher;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.pdf.ITextPdfReportGenerator;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.pdf.PdfReportStorage;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.persistence.jpa.repositories.ClinicalReportRepository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class ClinicalReportCommandServiceImpl implements ClinicalReportCommandService {
    private static final DateTimeFormatter REPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ClinicalReportRepository clinicalReportRepository;
    private final ITextPdfReportGenerator pdfReportGenerator;
    private final PdfReportStorage pdfReportStorage;
    private final ExternalHealthMonitoringService externalHealthMonitoringService;
    private final ExternalMedicationService externalMedicationService;
    private final ExternalAppointmentService externalAppointmentService;
    private final ExternalPatientProfileService externalPatientProfileService;
    private final ReportsAnalyticsIntegrationEventPublisher eventPublisher;
    private final AuthenticatedPatientAccessService authenticatedPatientAccessService;

    public ClinicalReportCommandServiceImpl(
            ClinicalReportRepository clinicalReportRepository,
            ITextPdfReportGenerator pdfReportGenerator,
            PdfReportStorage pdfReportStorage,
            ExternalHealthMonitoringService externalHealthMonitoringService,
            ExternalMedicationService externalMedicationService,
            ExternalAppointmentService externalAppointmentService,
            ExternalPatientProfileService externalPatientProfileService,
            ReportsAnalyticsIntegrationEventPublisher eventPublisher,
            AuthenticatedPatientAccessService authenticatedPatientAccessService) {
        this.clinicalReportRepository = clinicalReportRepository;
        this.pdfReportGenerator = pdfReportGenerator;
        this.pdfReportStorage = pdfReportStorage;
        this.externalHealthMonitoringService = externalHealthMonitoringService;
        this.externalMedicationService = externalMedicationService;
        this.externalAppointmentService = externalAppointmentService;
        this.externalPatientProfileService = externalPatientProfileService;
        this.eventPublisher = eventPublisher;
        this.authenticatedPatientAccessService = authenticatedPatientAccessService;
    }

    @Override
    public Optional<ClinicalReport> handle(GenerateClinicalReportCommand command) {
        if (!externalPatientProfileService.patientExists(command.patientId())) {
            throw new InvalidPatientReferenceException(command.patientId());
        }
        authenticatedPatientAccessService.requireAccess(command.requestedByUserId(), command.patientId());

        var patientName = externalPatientProfileService.getPatientFullName(command.patientId())
                .orElse("Paciente registrado");
        var summary = "Reporte clinico de " + patientName
                + ", correspondiente al periodo del " + formatDate(command.startDate())
                + " al " + formatDate(command.endDate())
                + ".";
        var report = new ClinicalReport(command, summary);
        report.addSection(new ReportSection(
                "Datos del paciente",
                "Paciente: " + patientName + ". Tipo de reporte: " + describeReportType(command.reportType())
                        + ". Periodo evaluado: " + formatDate(command.startDate()) + " al " + formatDate(command.endDate()) + ".",
                1));
        report.addSection(new ReportSection("Signos vitales y monitoreo", externalHealthMonitoringService.getPatientClinicalSummary(
                command.patientId(),
                command.startDate(),
                command.endDate()), 2));
        report.addSection(new ReportSection("Medicacion", externalMedicationService.getMedicationSummary(
                command.patientId(),
                command.startDate(),
                command.endDate()), 3));
        report.addSection(new ReportSection(
                "Citas medicas",
                externalAppointmentService.getAppointmentSummary(
                        command.patientId(),
                        command.startDate(),
                        command.endDate()),
                4));
        var savedReport = clinicalReportRepository.save(report);
        eventPublisher.publishClinicalReportGenerated(savedReport.getId(), savedReport.getPatientId());
        return Optional.of(savedReport);
    }

    @Override
    public Optional<ClinicalReport> handle(GeneratePdfReportCommand command) {
        var report = clinicalReportRepository.findById(command.reportId())
                .orElseThrow(() -> new ReportNotFoundException(command.reportId()));
        authenticatedPatientAccessService.requireAccess(command.requestedByUserId(), report.getPatientId());
        var pdf = pdfReportGenerator.generate(report);
        var pdfPath = pdfReportStorage.savePdf(report.getId(), pdf);
        report.attachPdf(pdfPath);
        return Optional.of(clinicalReportRepository.save(report));
    }

    private String formatDate(LocalDate date) {
        return date == null ? "No registrado" : date.format(REPORT_DATE_FORMAT);
    }

    private String describeReportType(ReportType reportType) {
        return switch (reportType) {
            case VITAL_SIGNS -> "Signos vitales";
            case MEDICATION -> "Medicacion";
            case FULL_CLINICAL -> "Clinico completo";
        };
    }
}

