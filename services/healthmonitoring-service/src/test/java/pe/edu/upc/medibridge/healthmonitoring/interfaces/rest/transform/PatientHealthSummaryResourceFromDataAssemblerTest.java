package pe.edu.upc.medibridge.healthmonitoring.interfaces.rest.transform;

import org.junit.jupiter.api.Test;
import pe.edu.upc.medibridge.healthmonitoring.domain.model.aggregates.PatientHealthObservation;
import pe.edu.upc.medibridge.healthmonitoring.domain.model.valueobjects.EmotionalState;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PatientHealthSummaryResourceFromDataAssemblerTest {

    @Test
    void mapsEmptyObservationSetToSafeDefaults() {
        var resource = PatientHealthSummaryResourceFromDataAssembler.toResourceFromData(
                22L,
                "Sin observaciones",
                List.of(),
                0);

        assertThat(resource.patientId()).isEqualTo(22L);
        assertThat(resource.latestBloodPressure()).isNull();
        assertThat(resource.averageTemperature()).isNull();
        assertThat(resource.painTrend()).isEqualTo("STABLE");
        assertThat(resource.emotionalTrend()).isEqualTo("STABLE");
        assertThat(resource.activeAlerts()).isZero();
        assertThat(resource.observationsCount()).isZero();
        assertThat(resource.lastObservation()).isNull();
    }

    @Test
    void mapsObservationsToLatestVitalsAndTrends() {
        var latest = observation(130, 85, "37.2", 6, EmotionalState.ANXIOUS, "2026-07-10T09:00:00");
        var previous = observation(120, 80, "36.8", 4, EmotionalState.CALM, "2026-07-09T09:00:00");

        var resource = PatientHealthSummaryResourceFromDataAssembler.toResourceFromData(
                22L,
                "Con observaciones",
                List.of(latest, previous),
                2);

        assertThat(resource.latestBloodPressure()).isEqualTo("130/85");
        assertThat(resource.averageTemperature()).isEqualByComparingTo("37.0");
        assertThat(resource.painTrend()).isEqualTo("ASCENDING");
        assertThat(resource.emotionalTrend()).isEqualTo("ASCENDING");
        assertThat(resource.activeAlerts()).isEqualTo(2);
        assertThat(resource.observationsCount()).isEqualTo(2);
        assertThat(resource.lastObservation()).isEqualTo(LocalDateTime.parse("2026-07-10T09:00:00"));
    }

    private PatientHealthObservation observation(
            int systolic,
            int diastolic,
            String temperature,
            int painLevel,
            EmotionalState emotionalState,
            String recordedAt) {
        var observation = mock(PatientHealthObservation.class);
        when(observation.getSystolicBloodPressure()).thenReturn(systolic);
        when(observation.getDiastolicBloodPressure()).thenReturn(diastolic);
        when(observation.getBodyTemperature()).thenReturn(new BigDecimal(temperature));
        when(observation.getPainLevel()).thenReturn(painLevel);
        when(observation.getEmotionalState()).thenReturn(emotionalState);
        when(observation.getRecordedAt()).thenReturn(LocalDateTime.parse(recordedAt));
        return observation;
    }
}
