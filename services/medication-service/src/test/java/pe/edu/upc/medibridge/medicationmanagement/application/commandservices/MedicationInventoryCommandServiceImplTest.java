package pe.edu.upc.medibridge.medicationmanagement.application.commandservices;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.upc.medibridge.medicationmanagement.application.outboundservices.acl.ExternalPatientContextService;
import pe.edu.upc.medibridge.medicationmanagement.application.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.aggregates.MedicationSchedule;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.CreateMedicationScheduleCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.DeactivateMedicationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.RegisterMedicationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.UpdateMedicationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.UpdateMedicationStockCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.entities.Medication;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.exceptions.MedicationNotFoundException;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.AdministrationRoute;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.DosageUnit;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.FrequencyType;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.messaging.publishers.MedicationIntegrationEventPublisher;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationRepository;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationScheduleRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationInventoryCommandServiceImplTest {

    @Mock
    private MedicationRepository medicationRepository;
    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ExternalPatientContextService externalPatientContextService;
    @Mock
    private MedicationIntegrationEventPublisher integrationEventPublisher;
    @Mock
    private AuthenticatedPatientAccessService authenticatedPatientAccessService;

    @InjectMocks
    private MedicationInventoryCommandServiceImpl service;

    @Test
    void retiresMedicationAndSchedulesWithoutDeletingClinicalHistory() {
        var medicationId = 7;
        var patientId = 13L;
        var requestedByUserId = 99L;
        var medication = new Medication(new RegisterMedicationCommand(
                patientId,
                "Panadol",
                new BigDecimal("500"),
                DosageUnit.MG,
                AdministrationRoute.ORAL,
                8,
                5,
                LocalDate.now().plusMonths(1),
                requestedByUserId));
        var schedule = new MedicationSchedule(new CreateMedicationScheduleCommand(
                medicationId,
                patientId,
                FrequencyType.DAILY,
                1,
                LocalTime.of(8, 0),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(5),
                requestedByUserId));
        var command = new DeactivateMedicationCommand(medicationId, requestedByUserId);

        when(medicationRepository.findByIdForUpdate(medicationId)).thenReturn(Optional.of(medication));
        when(medicationScheduleRepository.findByMedicationIdAndActiveTrue(medicationId))
                .thenReturn(List.of(schedule));

        service.handle(command);
        service.handle(command);

        assertThat(medication.isActive()).isFalse();
        assertThat(schedule.isActive()).isFalse();
        verify(authenticatedPatientAccessService, times(2)).requireAccess(requestedByUserId, patientId);
        verify(medicationRepository, times(2)).save(medication);
        verify(medicationScheduleRepository, times(2)).saveAll(List.of(schedule));
        verify(medicationRepository, never()).delete(medication);
        verify(medicationRepository, never()).deleteById(medicationId);
    }

    @Test
    void failsWhenMedicationDoesNotExist() {
        when(medicationRepository.findByIdForUpdate(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(new DeactivateMedicationCommand(404, 99L)))
                .isInstanceOf(MedicationNotFoundException.class);

        verify(authenticatedPatientAccessService, never()).requireAccess(99L, 13L);
    }

    @Test
    void rejectsStockUpdateForInactiveMedication() {
        var medication = org.mockito.Mockito.mock(Medication.class);
        when(medicationRepository.findByIdForUpdate(7)).thenReturn(Optional.of(medication));
        when(medication.getPatientId()).thenReturn(13L);
        when(medication.isActive()).thenReturn(false);

        assertThatThrownBy(() -> service.handle(new UpdateMedicationStockCommand(7, 10, 99L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot update stock for an inactive medication");

        verify(medication, never()).updateStock(10);
        verify(medicationRepository, never()).save(medication);
    }

    @Test
    void rejectsGeneralUpdateForInactiveMedication() {
        var medication = org.mockito.Mockito.mock(Medication.class);
        var command = new UpdateMedicationCommand(
                7,
                "Panadol",
                new BigDecimal("500"),
                DosageUnit.MG,
                AdministrationRoute.ORAL,
                8,
                5,
                LocalDate.now().plusMonths(1),
                99L);
        when(medicationRepository.findByIdForUpdate(7)).thenReturn(Optional.of(medication));
        when(medication.getPatientId()).thenReturn(13L);
        when(medication.isActive()).thenReturn(false);

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot update an inactive medication");

        verify(medication, never()).update(command);
        verify(medicationRepository, never()).save(medication);
    }
}
