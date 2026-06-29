package pe.edu.upc.medibridge.appointments.interfaces.rest.controllers;


import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import pe.edu.upc.medibridge.shared.interfaces.rest.resources.ErrorResponseResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upc.medibridge.appointments.interfaces.rest.acl.AppointmentContextFacade;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/internal/appointments")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "409", description = "Conflict", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class))),
        @ApiResponse(responseCode = "500", description = "Unexpected server error", content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
})
public class AppointmentsInternalController {
    private final AppointmentContextFacade appointmentContextFacade;

    public AppointmentsInternalController(AppointmentContextFacade appointmentContextFacade) {
        this.appointmentContextFacade = appointmentContextFacade;
    }

    @GetMapping("/patients/{patientId}/summary")
    public String getAppointmentSummaryByPatientId(
            @PathVariable Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate != null && endDate != null) {
            return appointmentContextFacade.fetchAppointmentSummaryByPatientIdAndPeriod(patientId, startDate, endDate);
        }
        return appointmentContextFacade.fetchAppointmentSummaryByPatientId(patientId);
    }
}
