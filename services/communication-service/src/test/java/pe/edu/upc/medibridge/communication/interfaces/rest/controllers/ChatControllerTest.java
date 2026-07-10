package pe.edu.upc.medibridge.communication.interfaces.rest.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import pe.edu.upc.medibridge.communication.application.internal.commandservices.AuthenticatedUserContextService;
import pe.edu.upc.medibridge.communication.domain.model.documents.ChatMessage;
import pe.edu.upc.medibridge.communication.domain.model.exceptions.ChatAccessDeniedException;
import pe.edu.upc.medibridge.communication.domain.services.ChatMessageService;
import pe.edu.upc.medibridge.communication.interfaces.rest.resources.SendChatMessageResource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private AuthenticatedUserContextService authenticatedUserContextService;

    @Test
    void sendMessageUsesAuthenticatedUserAsSender() {
        var jwt = mock(Jwt.class);
        var sentAt = Instant.parse("2026-07-10T05:00:00Z");
        var saved = ChatMessage.builder()
                .id("msg-1")
                .chatId("15-20")
                .senderUserId(15L)
                .recipientUserId(20L)
                .content("Hola")
                .sentAt(sentAt)
                .build();
        when(authenticatedUserContextService.resolveAuthenticatedUserId(jwt)).thenReturn(15L);
        when(chatMessageService.save(any(ChatMessage.class))).thenReturn(saved);

        var controller = new ChatController(chatMessageService, authenticatedUserContextService);
        var response = controller.sendMessage(new SendChatMessageResource(20L, "Hola", sentAt), jwt);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().senderUserId()).isEqualTo(15L);
        assertThat(response.getBody().recipientUserId()).isEqualTo(20L);

        var messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageService).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getSenderUserId()).isEqualTo(15L);
        assertThat(messageCaptor.getValue().getRecipientUserId()).isEqualTo(20L);
    }

    @Test
    void findMessagesRejectsUsersOutsideChat() {
        var jwt = mock(Jwt.class);
        when(authenticatedUserContextService.resolveAuthenticatedUserId(jwt)).thenReturn(99L);

        var controller = new ChatController(chatMessageService, authenticatedUserContextService);

        assertThatThrownBy(() -> controller.findMessages(15L, 20L, jwt))
                .isInstanceOf(ChatAccessDeniedException.class)
                .hasMessageContaining("part of the chat");
    }
}
