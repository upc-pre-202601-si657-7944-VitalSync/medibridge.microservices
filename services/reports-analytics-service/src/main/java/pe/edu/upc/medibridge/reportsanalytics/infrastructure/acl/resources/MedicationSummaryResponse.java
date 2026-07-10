package pe.edu.upc.medibridge.reportsanalytics.infrastructure.acl.resources;

import java.util.List;

public record MedicationSummaryResponse(
        Long patientId,
        int activeMedications,
        int lowStockMedications,
        long doseAdministrations,
        List<ActiveMedicationSummaryResponse> activeMedicationDetails) {
}

