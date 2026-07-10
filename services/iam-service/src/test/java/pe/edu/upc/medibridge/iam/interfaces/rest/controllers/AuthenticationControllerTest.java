package pe.edu.upc.medibridge.iam.interfaces.rest.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import pe.edu.upc.medibridge.iam.domain.model.aggregates.User;
import pe.edu.upc.medibridge.iam.domain.model.commands.SignUpCommand;
import pe.edu.upc.medibridge.iam.domain.model.entities.Role;
import pe.edu.upc.medibridge.iam.domain.model.valueobjects.Roles;
import pe.edu.upc.medibridge.iam.domain.services.UserCommandService;
import pe.edu.upc.medibridge.iam.interfaces.rest.resources.SignUpResource;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    @Mock
    private UserCommandService userCommandService;

    @Test
    void signUpReturnsCreatedUserResourceWhenCommandSucceeds() {
        var user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getUsername()).thenReturn("family_user");
        when(user.getRoles()).thenReturn(Set.of(new Role(Roles.ROLE_USER)));
        when(userCommandService.handle(any(SignUpCommand.class))).thenReturn(Optional.of(user));

        var controller = new AuthenticationController(userCommandService);
        var response = controller.signUp(new SignUpResource("family_user", "Test123456!", List.of("ROLE_USER")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(7L);
        assertThat(response.getBody().username()).isEqualTo("family_user");
        assertThat(response.getBody().roles()).containsExactly("ROLE_USER");

        var commandCaptor = ArgumentCaptor.forClass(SignUpCommand.class);
        verify(userCommandService).handle(commandCaptor.capture());
        assertThat(commandCaptor.getValue().username()).isEqualTo("family_user");
        assertThat(commandCaptor.getValue().roles())
                .extracting(Role::getStringName)
                .containsExactly("ROLE_USER");
    }

    @Test
    void signUpReturnsBadRequestWhenCommandFails() {
        when(userCommandService.handle(any(SignUpCommand.class))).thenReturn(Optional.empty());

        var controller = new AuthenticationController(userCommandService);
        var response = controller.signUp(new SignUpResource("bad_user", "Test123456!", List.of("ROLE_USER")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }
}
