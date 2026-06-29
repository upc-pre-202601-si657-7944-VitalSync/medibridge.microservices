package pe.edu.upc.medibridge.payments.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.upc.medibridge.payments.domain.model.queries.GetInvoiceHistoryQuery;
import pe.edu.upc.medibridge.payments.domain.services.InvoiceQueryService;
import pe.edu.upc.medibridge.payments.interfaces.rest.resources.InvoiceResponse;
import pe.edu.upc.medibridge.payments.interfaces.rest.transform.InvoiceResponseFromEntityAssembler;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/invoices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Invoices", description = "Invoice Query Endpoints")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class InvoiceController {
    private final InvoiceQueryService invoiceQueryService;

    public InvoiceController(InvoiceQueryService invoiceQueryService) {
        this.invoiceQueryService = invoiceQueryService;
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoiceHistory(@PathVariable Long userId) {
        var invoices = invoiceQueryService.handle(new GetInvoiceHistoryQuery(userId));
        var resources = invoices.stream()
                .map(InvoiceResponseFromEntityAssembler::toResourceFromEntity)
                .toList();
        return ResponseEntity.ok(resources);
    }
}
