package pe.edu.upc.medibridge.medicationmanagement.interfaces.rest.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.edu.upc.medibridge.medicationmanagement.application.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.DeactivateMedicationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.services.MedicationInventoryCommandService;
import pe.edu.upc.medibridge.medicationmanagement.domain.services.MedicationInventoryQueryService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationInventoryControllerTest {

    @Mock
    private MedicationInventoryCommandService medicationInventoryCommandService;
    @Mock
    private MedicationInventoryQueryService medicationInventoryQueryService;
    @Mock
    private AuthenticatedPatientAccessService authenticatedPatientAccessService;
    @Mock
    private Jwt jwt;

    @InjectMocks
    private MedicationInventoryController controller;

    @Test
    void deleteRetiresMedicationAndReturnsNoContent() {
        when(authenticatedPatientAccessService.resolveUserId(jwt)).thenReturn(99L);

        var response = controller.deactivateMedication(7, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(medicationInventoryCommandService).handle(new DeactivateMedicationCommand(7, 99L));
    }
}
