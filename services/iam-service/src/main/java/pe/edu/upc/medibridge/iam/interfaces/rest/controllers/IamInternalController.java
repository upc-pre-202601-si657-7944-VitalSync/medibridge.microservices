package pe.edu.upc.medibridge.iam.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.medibridge.iam.domain.model.queries.GetUserByIdQuery;
import pe.edu.upc.medibridge.iam.domain.model.queries.GetUserByUsernameQuery;
import pe.edu.upc.medibridge.iam.domain.model.exceptions.UserNotFoundException;
import pe.edu.upc.medibridge.iam.domain.services.UserQueryService;
import pe.edu.upc.medibridge.iam.interfaces.rest.resources.UserResource;
import pe.edu.upc.medibridge.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;

@RestController
@RequestMapping("/api/v1/internal/users")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "503", description = "Messaging infrastructure unavailable", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class IamInternalController {
    private final UserQueryService userQueryService;

    public IamInternalController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @GetMapping("/{userId}/exists")
    public boolean existsUserById(@PathVariable Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId)).isPresent();
    }

    @GetMapping("/{userId}")
    public UserResource getUserById(@PathVariable Long userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
    }

    @GetMapping("/by-username/{username}")
    public UserResource getUserByUsername(@PathVariable String username) {
        return userQueryService.handle(new GetUserByUsernameQuery(username))
                .map(UserResourceFromEntityAssembler::toResourceFromEntity)
                .orElseThrow(() -> new UserNotFoundException(username));
    }
}
