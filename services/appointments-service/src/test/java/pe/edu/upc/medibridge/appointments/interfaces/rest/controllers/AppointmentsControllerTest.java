package pe.edu.upc.medibridge.appointments.interfaces.rest.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.edu.upc.medibridge.appointments.application.internal.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.appointments.domain.model.queries.GetAppointmentByIdQuery;
import pe.edu.upc.medibridge.appointments.domain.model.queries.GetAppointmentsByPatientQuery;
import pe.edu.upc.medibridge.appointments.domain.services.AppointmentCommandService;
import pe.edu.upc.medibridge.appointments.domain.services.AppointmentQueryService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentsControllerTest {

    @Mock
    private AppointmentCommandService appointmentCommandService;

    @Mock
    private AppointmentQueryService appointmentQueryService;

    @Mock
    private AuthenticatedPatientAccessService authenticatedPatientAccessService;

    @Test
    void getAppointmentByIdReturnsNotFoundWhenAppointmentDoesNotExist() {
        var jwt = mock(Jwt.class);
        when(authenticatedPatientAccessService.resolveUserId(jwt)).thenReturn(31L);
        when(appointmentQueryService.handle(any(GetAppointmentByIdQuery.class))).thenReturn(Optional.empty());

        var controller = new AppointmentsController(
                appointmentCommandService,
                appointmentQueryService,
                authenticatedPatientAccessService);
        var response = controller.getAppointmentById(44L, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        var queryCaptor = ArgumentCaptor.forClass(GetAppointmentByIdQuery.class);
        verify(appointmentQueryService).handle(queryCaptor.capture());
        assertThat(queryCaptor.getValue().appointmentId()).isEqualTo(44L);
        assertThat(queryCaptor.getValue().requestedByUserId()).isEqualTo(31L);
    }

    @Test
    void getAppointmentsByPatientReturnsEmptyListWhenNoAppointmentsExist() {
        var jwt = mock(Jwt.class);
        when(authenticatedPatientAccessService.resolveUserId(jwt)).thenReturn(31L);
        when(appointmentQueryService.handle(any(GetAppointmentsByPatientQuery.class))).thenReturn(List.of());

        var controller = new AppointmentsController(
                appointmentCommandService,
                appointmentQueryService,
                authenticatedPatientAccessService);
        var response = controller.getAppointmentsByPatient(12L, jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}
