package pe.edu.upc.medibridge.reportsanalytics.interfaces.rest.transform;

import org.junit.jupiter.api.Test;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.aggregates.ClinicalReport;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.commands.GenerateClinicalReportCommand;
import pe.edu.upc.medibridge.reportsanalytics.domain.model.valueobjects.ReportType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicalReportResponseFromEntityAssemblerTest {

    @Test
    void exposesGeneratedAtWithExplicitUtcOffset() {
        var beforeUtc = LocalDateTime.now(ZoneOffset.UTC);
        var report = new ClinicalReport(
                new GenerateClinicalReportCommand(
                        13L,
                        ReportType.FULL_CLINICAL,
                        LocalDate.of(2026, 7, 10),
                        LocalDate.of(2026, 7, 10),
                        63L),
                "Resumen");
        var afterUtc = LocalDateTime.now(ZoneOffset.UTC);

        var response = ClinicalReportResponseFromEntityAssembler.toResourceFromEntity(report);

        assertThat(report.getGeneratedAt()).isBetween(beforeUtc, afterUtc);
        assertThat(response.generatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(response.generatedAt().toLocalDateTime()).isEqualTo(report.getGeneratedAt());
    }
}
