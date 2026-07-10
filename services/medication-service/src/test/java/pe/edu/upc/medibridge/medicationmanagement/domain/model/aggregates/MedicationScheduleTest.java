package pe.edu.upc.medibridge.medicationmanagement.domain.model.aggregates;

import org.junit.jupiter.api.Test;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.CreateMedicationScheduleCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.FrequencyType;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicationScheduleTest {

    @Test
    void requiresFrequency() {
        assertThatThrownBy(() -> new MedicationSchedule(command(null, 1, LocalTime.NOON, today(), today())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Medication frequency is required");
    }

    @Test
    void requiresPositiveTimesPerDay() {
        assertThatThrownBy(() -> new MedicationSchedule(command(FrequencyType.DAILY, 0, LocalTime.NOON, today(), today())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Times per day must be positive");
    }

    @Test
    void requiresAdministrationTime() {
        assertThatThrownBy(() -> new MedicationSchedule(command(FrequencyType.DAILY, 1, null, today(), today())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Administration time is required");
    }

    @Test
    void requiresStartDate() {
        assertThatThrownBy(() -> new MedicationSchedule(command(FrequencyType.DAILY, 1, LocalTime.NOON, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Schedule start date is required");
    }

    @Test
    void rejectsEndDateBeforeStartDate() {
        assertThatThrownBy(() -> new MedicationSchedule(command(
                FrequencyType.DAILY,
                1,
                LocalTime.NOON,
                today(),
                today().minusDays(1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Schedule end date cannot be before start date");
    }

    private CreateMedicationScheduleCommand command(
            FrequencyType frequencyType,
            Integer timesPerDay,
            LocalTime administrationTime,
            LocalDate startDate,
            LocalDate endDate) {
        return new CreateMedicationScheduleCommand(
                7,
                13L,
                frequencyType,
                timesPerDay,
                administrationTime,
                startDate,
                endDate,
                99L);
    }

    private LocalDate today() {
        return LocalDate.of(2026, 7, 10);
    }
}
