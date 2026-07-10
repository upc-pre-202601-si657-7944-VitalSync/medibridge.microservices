package pe.edu.upc.medibridge.reportsanalytics.infrastructure.acl.resources;

import java.math.BigDecimal;

public record ActiveMedicationSummaryResponse(
        Integer id,
        String name,
        BigDecimal dosageAmount,
        String dosageUnit,
        String administrationRoute,
        Integer stockQuantity) {
}
