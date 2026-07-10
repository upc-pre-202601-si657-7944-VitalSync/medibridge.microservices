package pe.edu.upc.medibridge.reportsanalytics.application.internal.outboundservices.acl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.acl.MedicationServiceClient;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.acl.resources.ActiveMedicationSummaryResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class MedicationExternalService implements ExternalMedicationService {
    private final MedicationServiceClient medicationServiceClient;

    public MedicationExternalService(MedicationServiceClient medicationServiceClient) {
        this.medicationServiceClient = medicationServiceClient;
    }

    @Override
    @CircuitBreaker(name = "medicationService", fallbackMethod = "getMedicationSummaryFallback")
    public String getMedicationSummary(Long patientId, LocalDate startDate, LocalDate endDate) {
        var summary = medicationServiceClient.getMedicationSummary(patientId, startDate, endDate);
        if (summary.activeMedications() == 0) {
            return "No hay medicacion activa registrada para este paciente.";
        }
        var activeMedicationDetails = summary.activeMedicationDetails() == null
                ? List.<ActiveMedicationSummaryResponse>of()
                : summary.activeMedicationDetails();
        if (!activeMedicationDetails.isEmpty()) {
            var medications = activeMedicationDetails.stream()
                    .map(this::describeMedication)
                    .toList();
            return "Medicamentos activos (" + summary.activeMedications() + "): "
                    + String.join(", ", medications) + ". "
                    + "Stock bajo: " + summary.lowStockMedications() + " medicamento(s). "
                    + "Dosis administradas en el periodo: " + summary.doseAdministrations() + ".";
        }
        return "Medicacion: " + summary.activeMedications() + " medicamento(s) activo(s), "
                + summary.lowStockMedications() + " con stock bajo, "
                + summary.doseAdministrations() + " dosis administrada(s) registradas en el periodo.";
    }

    private String getMedicationSummaryFallback(Long patientId, LocalDate startDate, LocalDate endDate, Throwable exception) {
        return "El resumen de medicacion no esta disponible temporalmente.";
    }

    private String describeMedication(ActiveMedicationSummaryResponse medication) {
        var description = new StringBuilder(valueOrDefault(medication.name(), "Medicamento sin nombre"));
        if (medication.dosageAmount() != null) {
            description.append(' ').append(medication.dosageAmount().stripTrailingZeros().toPlainString());
            if (medication.dosageUnit() != null && !medication.dosageUnit().isBlank()) {
                description.append(' ').append(medication.dosageUnit());
            }
        }
        if (medication.administrationRoute() != null && !medication.administrationRoute().isBlank()) {
            description.append(" (")
                    .append(medication.administrationRoute().toLowerCase(Locale.ROOT).replace('_', ' '))
                    .append(')');
        }
        return description.toString();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

