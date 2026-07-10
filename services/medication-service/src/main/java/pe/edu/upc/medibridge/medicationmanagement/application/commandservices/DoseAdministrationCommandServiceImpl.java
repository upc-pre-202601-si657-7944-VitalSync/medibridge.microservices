package pe.edu.upc.medibridge.medicationmanagement.application.commandservices;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.medibridge.medicationmanagement.application.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.RecordDoseAdministrationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.SkipDoseCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.entities.ClinicalLog;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.entities.DoseAdministration;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.entities.Medication;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.events.DoseAdministeredEvent;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.events.DoseSkippedEvent;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.events.StockCriticallyLowEvent;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.exceptions.DoseAlreadyAdministeredTodayException;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.exceptions.InsufficientStockException;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.exceptions.MedicationNotFoundException;
import pe.edu.upc.medibridge.medicationmanagement.domain.services.DoseAdministrationCommandService;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.messaging.publishers.MedicationIntegrationEventPublisher;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.ClinicalLogRepository;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.DoseAdministrationRepository;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationRepository;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationScheduleRepository;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.DoseAdministrationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

@Service
public class DoseAdministrationCommandServiceImpl implements DoseAdministrationCommandService {
    private final DoseAdministrationRepository doseAdministrationRepository;
    private final MedicationRepository medicationRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final ClinicalLogRepository clinicalLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MedicationIntegrationEventPublisher integrationEventPublisher;
    private final AuthenticatedPatientAccessService authenticatedPatientAccessService;

    public DoseAdministrationCommandServiceImpl(
            DoseAdministrationRepository doseAdministrationRepository,
            MedicationRepository medicationRepository,
            MedicationScheduleRepository medicationScheduleRepository,
            ClinicalLogRepository clinicalLogRepository,
            ApplicationEventPublisher eventPublisher,
            MedicationIntegrationEventPublisher integrationEventPublisher,
            AuthenticatedPatientAccessService authenticatedPatientAccessService) {
        this.doseAdministrationRepository = doseAdministrationRepository;
        this.medicationRepository = medicationRepository;
        this.medicationScheduleRepository = medicationScheduleRepository;
        this.clinicalLogRepository = clinicalLogRepository;
        this.eventPublisher = eventPublisher;
        this.integrationEventPublisher = integrationEventPublisher;
        this.authenticatedPatientAccessService = authenticatedPatientAccessService;
    }

    @Override
    @Transactional
    public Optional<DoseAdministration> handle(RecordDoseAdministrationCommand command) {
        if (command.administeredAt() == null) {
            throw new IllegalArgumentException("Dose administration date is required");
        }
        var medication = requireActiveMedicationAndSchedule(
                command.medicationId(),
                command.scheduleId(),
                command.patientId(),
                command.administeredAt().toLocalDate(),
                command.requestedByUserId());
        ensureDoseWasNotAdministeredToday(command.scheduleId(), command.administeredAt());
        if (medication.getStockQuantity() <= 0) {
            throw new InsufficientStockException(command.medicationId());
        }
        medication.decreaseStock();
        medicationRepository.save(medication);

        var doseAdministration = doseAdministrationRepository.save(new DoseAdministration(command));
        clinicalLogRepository.save(new ClinicalLog(
                command.patientId(),
                command.medicationId(),
                "Dose administered for medication " + medication.getName()));
        eventPublisher.publishEvent(new DoseAdministeredEvent(command.medicationId(), command.scheduleId(), command.patientId()));
        integrationEventPublisher.publishDoseAdministered(command.medicationId(), command.scheduleId(), command.patientId());
        if (medication.isLowStock()) {
            eventPublisher.publishEvent(new StockCriticallyLowEvent(medication.getId(), medication.getPatientId(), medication.getStockQuantity()));
            integrationEventPublisher.publishStockLow(medication);
        }
        return Optional.of(doseAdministration);
    }

    @Override
    @Transactional
    public Optional<DoseAdministration> handle(SkipDoseCommand command) {
        if (command.skippedAt() == null) {
            throw new IllegalArgumentException("Skipped dose date is required");
        }
        requireActiveMedicationAndSchedule(
                command.medicationId(),
                command.scheduleId(),
                command.patientId(),
                command.skippedAt().toLocalDate(),
                command.requestedByUserId());
        var doseAdministration = doseAdministrationRepository.save(new DoseAdministration(command));
        clinicalLogRepository.save(new ClinicalLog(
                command.patientId(),
                command.medicationId(),
                "Dose skipped. Reason: " + command.reason()));
        eventPublisher.publishEvent(new DoseSkippedEvent(command.medicationId(), command.scheduleId(), command.patientId()));
        integrationEventPublisher.publishDoseSkipped(command.medicationId(), command.scheduleId(), command.patientId(), command.reason());
        return Optional.of(doseAdministration);
    }

    private Medication requireActiveMedicationAndSchedule(
            Integer medicationId,
            Integer scheduleId,
            Long patientId,
            LocalDate occurredOn,
            Long requestedByUserId) {
        var medication = medicationRepository.findByIdForUpdate(medicationId)
                .orElseThrow(() -> new MedicationNotFoundException(medicationId));
        authenticatedPatientAccessService.requireAccess(requestedByUserId, medication.getPatientId());
        if (!Objects.equals(medication.getPatientId(), patientId)) {
            throw new IllegalArgumentException("Medication does not belong to the requested patient");
        }
        if (!medication.isActive()) {
            throw new IllegalStateException("Cannot record a dose for an inactive medication");
        }

        var schedule = medicationScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NoSuchElementException("Medication schedule not found with id: " + scheduleId));
        if (!Objects.equals(schedule.getMedicationId(), medicationId)
                || !Objects.equals(schedule.getPatientId(), patientId)) {
            throw new IllegalArgumentException("Medication, schedule and patient do not match");
        }
        if (!schedule.isActiveOn(occurredOn)) {
            throw new IllegalStateException("Medication schedule is inactive for the dose date");
        }
        return medication;
    }

    private void ensureDoseWasNotAdministeredToday(Integer scheduleId, LocalDateTime occurredAt) {
        var date = occurredAt.toLocalDate();
        var start = LocalDateTime.of(date, LocalTime.MIN);
        var end = LocalDateTime.of(date, LocalTime.MAX);
        doseAdministrationRepository.findByScheduleIdAndStatusAndOccurredAtBetween(
                scheduleId,
                DoseAdministrationStatus.ADMINISTERED,
                start,
                end).ifPresent(existing -> {
                    throw new DoseAlreadyAdministeredTodayException(scheduleId);
                });
    }
}

