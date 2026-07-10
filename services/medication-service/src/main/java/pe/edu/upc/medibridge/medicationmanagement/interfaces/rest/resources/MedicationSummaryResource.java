package pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.resources;

import java.util.List;

public record MedicationSummaryResource(
        Long patientId,
        int activeMedications,
        int lowStockMedications,
        long doseAdministrations,
        List<ActiveMedicationSummaryResource> activeMedicationDetails) {
}

