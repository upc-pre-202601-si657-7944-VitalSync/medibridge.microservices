package pe.edu.upc.medibridge.medicationmanagement.application.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.aggregates.MedicationSchedule;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.queries.GetActiveMedicationSchedulesQuery;
import pe.edu.upc.medibridge.medicationmanagement.domain.services.MedicationScheduleQueryService;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationScheduleRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class MedicationScheduleQueryServiceImpl implements MedicationScheduleQueryService {
    private static final ZoneId CLINICAL_ZONE = ZoneId.of("America/Lima");

    private final MedicationScheduleRepository medicationScheduleRepository;
    private final AuthenticatedPatientAccessService authenticatedPatientAccessService;

    public MedicationScheduleQueryServiceImpl(
            MedicationScheduleRepository medicationScheduleRepository,
            AuthenticatedPatientAccessService authenticatedPatientAccessService) {
        this.medicationScheduleRepository = medicationScheduleRepository;
        this.authenticatedPatientAccessService = authenticatedPatientAccessService;
    }

    @Override
    public List<MedicationSchedule> handle(GetActiveMedicationSchedulesQuery query) {
        authenticatedPatientAccessService.requireAccess(query.requestedByUserId(), query.patientId());
        var today = LocalDate.now(CLINICAL_ZONE);
        return medicationScheduleRepository.findByPatientIdAndActiveTrue(query.patientId()).stream()
                .filter(schedule -> schedule.isActiveOn(today))
                .toList();
    }
}

