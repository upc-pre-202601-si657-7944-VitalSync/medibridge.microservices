package pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.transform;

import pe.edu.upc.medibridge.healthmonitoring.domain.model.aggregates.PatientHealthObservation;
import pe.edu.upc.medibridge.healthmonitoring.domain.model.valueobjects.EmotionalState;
import pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.resources.PatientHealthSummaryResource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PatientHealthSummaryResourceFromDataAssembler {

    private static final String TREND_ASCENDING = "ASCENDING";
    private static final String TREND_DESCENDING = "DESCENDING";
    private static final String TREND_STABLE = "STABLE";

    public static PatientHealthSummaryResource toResourceFromData(
            Long patientId,
            String summary,
            List<PatientHealthObservation> observations,
            Integer activeAlerts) {
        if (observations.isEmpty()) {
            return new PatientHealthSummaryResource(
                    patientId,
                    summary,
                    null,
                    null,
                    TREND_STABLE,
                    TREND_STABLE,
                    activeAlerts,
                    0,
                    null);
        }

        var latest = observations.getFirst();
        var previous = observations.size() > 1 ? observations.get(1) : latest;

        return new PatientHealthSummaryResource(
                patientId,
                summary,
                latest.getSystolicBloodPressure() + "/" + latest.getDiastolicBloodPressure(),
                averageTemperature(observations),
                trend(latest.getPainLevel(), previous.getPainLevel()),
                emotionalTrend(latest.getEmotionalState(), previous.getEmotionalState()),
                activeAlerts,
                observations.size(),
                latest.getRecordedAt());
    }

    private static BigDecimal averageTemperature(List<PatientHealthObservation> observations) {
        var total = observations.stream()
                .map(PatientHealthObservation::getBodyTemperature)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(observations.size()), 1, RoundingMode.HALF_UP);
    }

    private static String trend(Integer latest, Integer previous) {
        var comparison = latest.compareTo(previous);
        if (comparison > 0) {
            return TREND_ASCENDING;
        }
        if (comparison < 0) {
            return TREND_DESCENDING;
        }
        return TREND_STABLE;
    }

    private static String emotionalTrend(EmotionalState latest, EmotionalState previous) {
        return trend(emotionalScore(latest), emotionalScore(previous));
    }

    private static Integer emotionalScore(EmotionalState state) {
        return switch (state) {
            case CALM -> 0;
            case ANXIOUS, SAD -> 2;
            case IRRITABLE, APATHETIC -> 3;
            case CONFUSED -> 4;
        };
    }
}
