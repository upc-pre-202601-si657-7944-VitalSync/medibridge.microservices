package pe.edu.upc.medibridge.profiles.interfaces.rest.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.medibridge.profiles.domain.model.aggregates.PatientProfile;
import pe.edu.upc.medibridge.profiles.domain.model.queries.GetPatientProfileByIdQuery;
import pe.edu.upc.medibridge.profiles.domain.services.CareRelationshipQueryService;
import pe.edu.upc.medibridge.profiles.domain.services.PatientProfileQueryService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfilesInternalControllerTest {

    @Mock
    private PatientProfileQueryService patientProfileQueryService;

    @Mock
    private CareRelationshipQueryService careRelationshipQueryService;

    @Test
    void existsPatientByIdReturnsTrueWhenPatientExists() {
        when(patientProfileQueryService.handle(argThat(query -> query.patientId().equals(12L))))
                .thenReturn(Optional.of(mock(PatientProfile.class)));

        var controller = new ProfilesInternalController(patientProfileQueryService, careRelationshipQueryService);

        assertThat(controller.existsPatientById(12L)).isTrue();
    }

    @Test
    void existsPatientByIdReturnsFalseWhenPatientIdIsNull() {
        var controller = new ProfilesInternalController(patientProfileQueryService, careRelationshipQueryService);

        assertThat(controller.existsPatientById(null)).isFalse();
    }

    @Test
    void existsPatientByIdReturnsFalseWhenPatientDoesNotExist() {
        when(patientProfileQueryService.handle(argThat((GetPatientProfileByIdQuery query) -> query.patientId().equals(99L))))
                .thenReturn(Optional.empty());

        var controller = new ProfilesInternalController(patientProfileQueryService, careRelationshipQueryService);

        assertThat(controller.existsPatientById(99L)).isFalse();
    }
}
