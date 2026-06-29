package pe.edu.upc.medibridge.communication.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.medibridge.communication.domain.model.documents.ConnectedUser;
import pe.edu.upc.medibridge.communication.domain.services.ConnectedUserService;
import pe.edu.upc.medibridge.communication.interfaces.rest.resources.ConnectUserResource;
import pe.edu.upc.medibridge.communication.interfaces.rest.resources.ConnectedUserResource;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/chat/users", produces = MediaType.APPLICATION_JSON_VALUE)
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class ConnectedUserController {
    private final ConnectedUserService connectedUserService;

    public ConnectedUserController(ConnectedUserService connectedUserService) {
        this.connectedUserService = connectedUserService;
    }

    @PostMapping(value = "/connect", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectedUserResource> connect(@Valid @RequestBody ConnectUserResource resource) {
        var user = connectedUserService.connect(ConnectedUser.builder()
                .userId(resource.userId())
                .username(resource.username())
                .fullName(resource.fullName())
                .build());
        return ResponseEntity.ok(ConnectedUserResource.from(user));
    }

    @PostMapping(value = "/disconnect", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ConnectedUserResource> disconnect(@Valid @RequestBody ConnectUserResource resource) {
        return ResponseEntity.ok(ConnectedUserResource.from(connectedUserService.disconnect(resource.userId())));
    }

    @GetMapping("/connected")
    public ResponseEntity<List<ConnectedUserResource>> findConnectedUsers() {
        var users = connectedUserService.findConnectedUsers()
                .stream()
                .map(ConnectedUserResource::from)
                .toList();
        return ResponseEntity.ok(users);
    }
}
