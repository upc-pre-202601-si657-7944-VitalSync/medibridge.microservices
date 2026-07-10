package pe.edu.upc.medibridge.medicationmanagement.application.queryservices;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.aggregates.MedicationSchedule;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.CreateMedicationScheduleCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.queries.GetActiveMedicationSchedulesQuery;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.FrequencyType;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationScheduleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationScheduleQueryServiceImplTest {

    private static final ZoneId CLINICAL_ZONE = ZoneId.of("America/Lima");

    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;
    @Mock
    private AuthenticatedPatientAccessService authenticatedPatientAccessService;

    @InjectMocks
    private MedicationScheduleQueryServiceImpl service;

    @Test
    void returnsOnlySchedulesWhoseDateRangeIsCurrent() {
        var patientId = 13L;
        var userId = 99L;
        var today = LocalDate.now(CLINICAL_ZONE);
        var expired = schedule(patientId, today.minusDays(10), today.minusDays(1));
        var future = schedule(patientId, today.plusDays(1), today.plusDays(10));
        var endingToday = schedule(patientId, today.minusDays(5), today);
        var withoutEndDate = schedule(patientId, today.minusDays(5), null);
        when(medicationScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(expired, future, endingToday, withoutEndDate));

        var result = service.handle(new GetActiveMedicationSchedulesQuery(patientId, userId));

        assertThat(result).containsExactly(endingToday, withoutEndDate);
        verify(authenticatedPatientAccessService).requireAccess(userId, patientId);
    }

    private MedicationSchedule schedule(Long patientId, LocalDate startDate, LocalDate endDate) {
        return new MedicationSchedule(new CreateMedicationScheduleCommand(
                7,
                patientId,
                FrequencyType.DAILY,
                1,
                LocalTime.of(8, 0),
                startDate,
                endDate,
                99L));
    }
}
