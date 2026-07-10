package pe.edu.upc.medibridge.medicationmanagement.application.commandservices;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.upc.medibridge.medicationmanagement.application.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.aggregates.MedicationSchedule;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.RecordDoseAdministrationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.SkipDoseCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.entities.Medication;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.messaging.publishers.MedicationIntegrationEventPublisher;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.ClinicalLogRepository;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.DoseAdministrationRepository;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationRepository;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationScheduleRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoseAdministrationCommandServiceImplTest {

    @Mock
    private DoseAdministrationRepository doseAdministrationRepository;
    @Mock
    private MedicationRepository medicationRepository;
    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;
    @Mock
    private ClinicalLogRepository clinicalLogRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private MedicationIntegrationEventPublisher integrationEventPublisher;
    @Mock
    private AuthenticatedPatientAccessService authenticatedPatientAccessService;

    @InjectMocks
    private DoseAdministrationCommandServiceImpl service;

    @Test
    void rejectsAdministrationForInactiveMedication() {
        var medication = mock(Medication.class);
        when(medicationRepository.findByIdForUpdate(7)).thenReturn(Optional.of(medication));
        when(medication.getPatientId()).thenReturn(13L);
        when(medication.isActive()).thenReturn(false);

        assertThatThrownBy(() -> service.handle(recordCommand(7, 5, 13L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot record a dose for an inactive medication");

        verify(medicationScheduleRepository, never()).findById(5);
        verify(doseAdministrationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsAdministrationWhenMedicationDoesNotBelongToPatient() {
        var medication = mock(Medication.class);
        when(medicationRepository.findByIdForUpdate(7)).thenReturn(Optional.of(medication));
        when(medication.getPatientId()).thenReturn(20L);

        assertThatThrownBy(() -> service.handle(recordCommand(7, 5, 13L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Medication does not belong to the requested patient");

        verify(authenticatedPatientAccessService).requireAccess(99L, 20L);
        verify(medicationScheduleRepository, never()).findById(5);
    }

    @Test
    void rejectsSkippedDoseWhenScheduleDoesNotMatchMedication() {
        var medication = activeMedication(13L);
        var schedule = mock(MedicationSchedule.class);
        when(medicationRepository.findByIdForUpdate(7)).thenReturn(Optional.of(medication));
        when(medicationScheduleRepository.findById(5)).thenReturn(Optional.of(schedule));
        when(schedule.getMedicationId()).thenReturn(8);

        assertThatThrownBy(() -> service.handle(skipCommand(7, 5, 13L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Medication, schedule and patient do not match");

        verify(doseAdministrationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsSkippedDoseOutsideActiveScheduleRange() {
        var medication = activeMedication(13L);
        var schedule = mock(MedicationSchedule.class);
        var command = skipCommand(7, 5, 13L);
        when(medicationRepository.findByIdForUpdate(7)).thenReturn(Optional.of(medication));
        when(medicationScheduleRepository.findById(5)).thenReturn(Optional.of(schedule));
        when(schedule.getMedicationId()).thenReturn(7);
        when(schedule.getPatientId()).thenReturn(13L);
        when(schedule.isActiveOn(command.skippedAt().toLocalDate())).thenReturn(false);

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Medication schedule is inactive for the dose date");

        verify(doseAdministrationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private Medication activeMedication(Long patientId) {
        var medication = mock(Medication.class);
        when(medication.getPatientId()).thenReturn(patientId);
        when(medication.isActive()).thenReturn(true);
        return medication;
    }

    private RecordDoseAdministrationCommand recordCommand(Integer medicationId, Integer scheduleId, Long patientId) {
        return new RecordDoseAdministrationCommand(
                medicationId,
                scheduleId,
                patientId,
                LocalDateTime.now(),
                "",
                99L);
    }

    private SkipDoseCommand skipCommand(Integer medicationId, Integer scheduleId, Long patientId) {
        return new SkipDoseCommand(
                medicationId,
                scheduleId,
                patientId,
                LocalDateTime.now(),
                "Patient declined",
                99L);
    }
}
