package pe.edu.upc.medibridge.medicationmanagement.application.commandservices;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.medibridge.medicationmanagement.application.outboundservices.acl.ExternalPatientContextService;
import pe.edu.upc.medibridge.medicationmanagement.application.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.CreateMedicationScheduleCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.entities.Medication;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.FrequencyType;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationRepository;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationScheduleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationScheduleCommandServiceImplTest {

    @Mock
    private MedicationScheduleRepository medicationScheduleRepository;
    @Mock
    private MedicationRepository medicationRepository;
    @Mock
    private ExternalPatientContextService externalPatientContextService;
    @Mock
    private AuthenticatedPatientAccessService authenticatedPatientAccessService;

    @InjectMocks
    private MedicationScheduleCommandServiceImpl service;

    @Test
    void rejectsMedicationOwnedByAnotherPatient() {
        var command = commandFor(7, 13L);
        var medication = mock(Medication.class);
        when(externalPatientContextService.patientExists(13L)).thenReturn(true);
        when(medicationRepository.findByIdForUpdate(7)).thenReturn(Optional.of(medication));
        when(medication.getPatientId()).thenReturn(20L);

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Medication does not belong to the requested patient");

        verify(medicationScheduleRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsInactiveMedication() {
        var command = commandFor(7, 13L);
        var medication = mock(Medication.class);
        when(externalPatientContextService.patientExists(13L)).thenReturn(true);
        when(medicationRepository.findByIdForUpdate(7)).thenReturn(Optional.of(medication));
        when(medication.getPatientId()).thenReturn(13L);
        when(medication.isActive()).thenReturn(false);

        assertThatThrownBy(() -> service.handle(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot schedule an inactive medication");

        verify(medicationScheduleRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private CreateMedicationScheduleCommand commandFor(Integer medicationId, Long patientId) {
        return new CreateMedicationScheduleCommand(
                medicationId,
                patientId,
                FrequencyType.DAILY,
                1,
                LocalTime.of(8, 0),
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                99L);
    }
}
