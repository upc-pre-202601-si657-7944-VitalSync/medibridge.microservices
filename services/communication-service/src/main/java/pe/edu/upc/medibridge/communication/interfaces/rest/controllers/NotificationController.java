package pe.edu.upc.medibridge.communication.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.medibridge.communication.domain.services.NotificationService;
import pe.edu.upc.medibridge.communication.interfaces.rest.resources.NotificationResource;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "503", description = "Communication persistence is unavailable", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/recipients/{recipientUserId}")
    public ResponseEntity<List<NotificationResource>> findByRecipientUserId(@PathVariable Long recipientUserId) {
        var notifications = notificationService.findByRecipientUserId(recipientUserId)
                .stream()
                .map(NotificationResource::from)
                .toList();
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/recipients/{recipientUserId}/unread")
    public ResponseEntity<List<NotificationResource>> findUnreadByRecipientUserId(@PathVariable Long recipientUserId) {
        var notifications = notificationService.findUnreadByRecipientUserId(recipientUserId)
                .stream()
                .map(NotificationResource::from)
                .toList();
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResource> markAsRead(@PathVariable String notificationId) {
        return ResponseEntity.ok(NotificationResource.from(notificationService.markAsRead(notificationId)));
    }
}
