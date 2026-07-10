package pe.edu.upc.medibridge.reportsanalytics.infrastructure.pdf;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ITextPdfReportGeneratorTest {

    @Test
    void formatsPersistedUtcTimestampInPeruTime() {
        var generatedAtUtc = LocalDateTime.of(2026, 7, 10, 5, 15);

        var formatted = ITextPdfReportGenerator.formatDateTime(generatedAtUtc);

        assertThat(formatted).isEqualTo("10/07/2026 00:15");
    }
}
