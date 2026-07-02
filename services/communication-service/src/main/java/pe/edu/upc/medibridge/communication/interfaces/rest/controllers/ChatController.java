package pe.edu.upc.medibridge.communication.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.medibridge.communication.application.internal.commandservices.AuthenticatedUserContextService;
import pe.edu.upc.medibridge.communication.domain.model.documents.ChatMessage;
import pe.edu.upc.medibridge.communication.domain.model.exceptions.ChatAccessDeniedException;
import pe.edu.upc.medibridge.communication.domain.services.ChatMessageService;
import pe.edu.upc.medibridge.communication.interfaces.rest.resources.ChatMessageResource;
import pe.edu.upc.medibridge.communication.interfaces.rest.resources.SendChatMessageResource;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping(value = "/api/v1/chat/messages", produces = MediaType.APPLICATION_JSON_VALUE)
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "503", description = "Communication persistence is unavailable", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class ChatController {
    private final ChatMessageService chatMessageService;
    private final AuthenticatedUserContextService authenticatedUserContextService;

    public ChatController(
            ChatMessageService chatMessageService,
            AuthenticatedUserContextService authenticatedUserContextService) {
        this.chatMessageService = chatMessageService;
        this.authenticatedUserContextService = authenticatedUserContextService;
    }

    @ApiResponse(responseCode = "201", description = "Created")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChatMessageResource> sendMessage(
            @Valid @RequestBody SendChatMessageResource resource,
            @AuthenticationPrincipal Jwt jwt) {
        var senderUserId = authenticatedUserContextService.resolveAuthenticatedUserId(jwt);
        var saved = chatMessageService.save(ChatMessage.builder()
                .senderUserId(senderUserId)
                .recipientUserId(resource.recipientUserId())
                .content(resource.content())
                .sentAt(resource.sentAt())
                .build());
        return new ResponseEntity<>(ChatMessageResource.from(saved), HttpStatus.CREATED);
    }

    @GetMapping("/{senderUserId}/{recipientUserId}")
    public ResponseEntity<List<ChatMessageResource>> findMessages(
            @PathVariable Long senderUserId,
            @PathVariable Long recipientUserId,
            @AuthenticationPrincipal Jwt jwt) {
        var authenticatedUserId = authenticatedUserContextService.resolveAuthenticatedUserId(jwt);
        if (!Objects.equals(authenticatedUserId, senderUserId) && !Objects.equals(authenticatedUserId, recipientUserId)) {
            throw new ChatAccessDeniedException("Authenticated user must be part of the chat");
        }
        var messages = chatMessageService.findChatMessages(senderUserId, recipientUserId)
                .stream()
                .map(ChatMessageResource::from)
                .toList();
        return ResponseEntity.ok(messages);
    }
}
