package pe.edu.upc.medibridge.reportsanalytics.application.internal.outboundservices.acl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.acl.AppointmentsServiceClient;

import java.time.LocalDate;

@Service
public class AppointmentExternalService implements ExternalAppointmentService {
    private final AppointmentsServiceClient appointmentsServiceClient;

    public AppointmentExternalService(AppointmentsServiceClient appointmentsServiceClient) {
        this.appointmentsServiceClient = appointmentsServiceClient;
    }

    @Override
    @CircuitBreaker(name = "appointmentsService", fallbackMethod = "getAppointmentSummaryFallback")
    public String getAppointmentSummary(Long patientId, LocalDate startDate, LocalDate endDate) {
        return translateSummary(appointmentsServiceClient.getAppointmentSummaryByPatientId(patientId, startDate, endDate));
    }

    private String getAppointmentSummaryFallback(Long patientId, LocalDate startDate, LocalDate endDate, Throwable exception) {
        return "El resumen de citas medicas no esta disponible temporalmente.";
    }

    private String translateSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return "No hay citas medicas registradas para este paciente.";
        }
        return summary
                .replace("No appointments registered for this patient in the report period.",
                        "No hay citas medicas registradas para este paciente en el periodo del reporte.")
                .replace("No appointments registered for this patient.",
                        "No hay citas medicas registradas para este paciente.")
                .replace("assigned doctor", "medico asignado")
                .replace("family member", "familiar")
                .replace("No reason registered", "Sin motivo registrado")
                .replace(" appointment with ", " con ")
                .replace(" from ", " desde ")
                .replace(" to ", " hasta ")
                .replace(". Status: ", ". Estado: ")
                .replace(". Reason: ", ". Motivo: ")
                .replace("MEDICAL", "Cita medica")
                .replace("FAMILY_VISIT", "Visita familiar")
                .replace("SCHEDULED", "programada")
                .replace("CONFIRMED", "confirmada")
                .replace("COMPLETED", "completada")
                .replace("CANCELLED", "cancelada");
    }
}

