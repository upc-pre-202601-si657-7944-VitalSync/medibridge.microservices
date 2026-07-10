package pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.resources;

import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.AdministrationRoute;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.DosageUnit;

import java.math.BigDecimal;

public record ActiveMedicationSummaryResource(
        Integer id,
        String name,
        BigDecimal dosageAmount,
        DosageUnit dosageUnit,
        AdministrationRoute administrationRoute,
        Integer stockQuantity) {
}
