package pe.edu.upc.medibridge.reportsanalytics.application.internal.outboundservices.acl;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Service;
import pe.edu.upc.medibridge.reportsanalytics.infrastructure.acl.HealthMonitoringServiceClient;

import java.time.LocalDate;

@Service
public class HealthMonitoringExternalService implements ExternalHealthMonitoringService {
    private final HealthMonitoringServiceClient healthMonitoringServiceClient;

    public HealthMonitoringExternalService(HealthMonitoringServiceClient healthMonitoringServiceClient) {
        this.healthMonitoringServiceClient = healthMonitoringServiceClient;
    }

    @Override
    @CircuitBreaker(name = "healthMonitoringService", fallbackMethod = "getPatientClinicalSummaryFallback")
    public String getPatientClinicalSummary(Long patientId, LocalDate startDate, LocalDate endDate) {
        return translateSummary(healthMonitoringServiceClient.getPatientHealthSummary(patientId, startDate, endDate));
    }

    private String getPatientClinicalSummaryFallback(Long patientId, LocalDate startDate, LocalDate endDate, Throwable exception) {
        return "El resumen de signos vitales y monitoreo no esta disponible temporalmente.";
    }

    private String translateSummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return "No hay informacion de monitoreo registrada para este paciente.";
        }
        return summary
                .replace("No health monitoring observations registered for this patient in the report period.",
                        "No hay registros de monitoreo para este paciente en el periodo del reporte.")
                .replace("No health monitoring observations registered for this patient.",
                        "No hay registros de monitoreo para este paciente.")
                .replace("Latest health observation in report period recorded at ",
                        "Ultimo registro de salud en el periodo: ")
                .replace("Latest health observation recorded at ",
                        "Ultimo registro de salud: ")
                .replace(": blood pressure ", ": presion arterial ")
                .replace(", body temperature ", ", temperatura ")
                .replace(", pain level ", ", dolor ")
                .replace(", emotional state ", ", estado emocional ")
                .replace("Recent observations in report period: ", "Registros recientes del periodo: ")
                .replace("Recent observations: ", "Registros recientes: ")
                .replace(" BP ", " PA ")
                .replace(", temp ", ", temp. ")
                .replace(", pain ", ", dolor ")
                .replace(", mood ", ", animo ")
                .replace("No active clinical alerts in the report period.", "Sin alertas clinicas activas en el periodo.")
                .replace("No active clinical alerts.", "Sin alertas clinicas activas.")
                .replace("Active clinical alerts in report period: ", "Alertas clinicas activas en el periodo: ")
                .replace("Active clinical alerts: ", "Alertas clinicas activas: ")
                .replace("CALM", "calmado")
                .replace("ANXIOUS", "ansioso")
                .replace("SAD", "triste")
                .replace("IRRITABLE", "irritable")
                .replace("CONFUSED", "confundido")
                .replace("APATHETIC", "apatico")
                .replace("MEDIUM", "media")
                .replace("HIGH", "alta");
    }
}

