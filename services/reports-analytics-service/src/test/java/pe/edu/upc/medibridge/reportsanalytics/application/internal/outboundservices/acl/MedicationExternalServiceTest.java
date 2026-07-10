package pe.edu.upc.medibridge.reportsanalytics.application.internal.outboundservices.acl;

import org.junit.jupiter.api.Test;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.acl.MedicationServiceClient;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.acl.resources.ActiveMedicationSummaryResponse;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.acl.resources.MedicationSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MedicationExternalServiceTest {

    private final MedicationServiceClient medicationServiceClient = mock(MedicationServiceClient.class);
    private final MedicationExternalService service = new MedicationExternalService(medicationServiceClient);

    @Test
    void includesMedicationNameDoseAndRouteAlongsideCounts() {
        var patientId = 13L;
        var startDate = LocalDate.of(2026, 7, 10);
        var endDate = LocalDate.of(2026, 7, 10);
        var details = new ActiveMedicationSummaryResponse(
                5,
                "Ibuprofeno",
                new BigDecimal("500.00"),
                "MG",
                "ORAL",
                9);
        when(medicationServiceClient.getMedicationSummary(patientId, startDate, endDate))
                .thenReturn(new MedicationSummaryResponse(patientId, 1, 0, 1, List.of(details)));

        var summary = service.getMedicationSummary(patientId, startDate, endDate);

        assertThat(summary).isEqualTo(
                "Medicamentos activos (1): Ibuprofeno 500 MG (oral). "
                        + "Stock bajo: 0 medicamento(s). Dosis administradas en el periodo: 1.");
    }

    @Test
    void keepsCountOnlyTextWhenAnOlderMedicationServiceDoesNotSendDetails() {
        var patientId = 13L;
        var startDate = LocalDate.of(2026, 7, 10);
        var endDate = LocalDate.of(2026, 7, 10);
        when(medicationServiceClient.getMedicationSummary(patientId, startDate, endDate))
                .thenReturn(new MedicationSummaryResponse(patientId, 1, 0, 1, null));

        var summary = service.getMedicationSummary(patientId, startDate, endDate);

        assertThat(summary).isEqualTo(
                "Medicacion: 1 medicamento(s) activo(s), 0 con stock bajo, "
                        + "1 dosis administrada(s) registradas en el periodo.");
    }
}
