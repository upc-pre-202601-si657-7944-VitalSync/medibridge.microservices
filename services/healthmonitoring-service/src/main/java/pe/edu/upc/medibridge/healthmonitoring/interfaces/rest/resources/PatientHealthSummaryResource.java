package pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.resources;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PatientHealthSummaryResource(
        Long patientId,
        String summary,
        String latestBloodPressure,
        BigDecimal averageTemperature,
        String painTrend,
        String emotionalTrend,
        Integer activeAlerts,
        Integer observationsCount,
        LocalDateTime lastObservation) {
}

