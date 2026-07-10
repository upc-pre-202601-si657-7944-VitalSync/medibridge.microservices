package pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.entities.Medication;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.AdministrationRoute;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.DosageUnit;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.DoseAdministrationStatus;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.DoseAdministrationRepository;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationInternalControllerTest {

    @Mock
    private MedicationRepository medicationRepository;

    @Mock
    private DoseAdministrationRepository doseAdministrationRepository;

    @InjectMocks
    private MedicationInternalController controller;

    @Test
    void returnsStructuredDetailsForEveryActiveMedication() {
        var patientId = 13L;
        var startDate = LocalDate.of(2026, 7, 10);
        var endDate = LocalDate.of(2026, 7, 10);
        var medication = org.mockito.Mockito.mock(Medication.class);
        when(medication.getId()).thenReturn(5);
        when(medication.getName()).thenReturn("Ibuprofeno");
        when(medication.getDosageAmount()).thenReturn(new BigDecimal("500.00"));
        when(medication.getDosageUnit()).thenReturn(DosageUnit.MG);
        when(medication.getAdministrationRoute()).thenReturn(AdministrationRoute.ORAL);
        when(medication.getStockQuantity()).thenReturn(9);
        when(medication.getLowStockThreshold()).thenReturn(5);
        when(medicationRepository.findByPatientIdAndActiveTrue(patientId)).thenReturn(List.of(medication));
        when(doseAdministrationRepository.countByPatientIdAndStatusAndOccurredAtBetween(
                patientId,
                DoseAdministrationStatus.ADMINISTERED,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay())).thenReturn(1L);

        var response = controller.getMedicationSummary(patientId, startDate, endDate);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().activeMedications()).isEqualTo(1);
        assertThat(response.getBody().lowStockMedications()).isZero();
        assertThat(response.getBody().doseAdministrations()).isEqualTo(1);
        assertThat(response.getBody().activeMedicationDetails()).singleElement().satisfies(detail -> {
            assertThat(detail.id()).isEqualTo(5);
            assertThat(detail.name()).isEqualTo("Ibuprofeno");
            assertThat(detail.dosageAmount()).isEqualByComparingTo("500.00");
            assertThat(detail.dosageUnit()).isEqualTo(DosageUnit.MG);
            assertThat(detail.administrationRoute()).isEqualTo(AdministrationRoute.ORAL);
            assertThat(detail.stockQuantity()).isEqualTo(9);
        });
    }
}
