package pe.edu.upc.medibridge.medicationmanagement.application.commandservices;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.medibridge.medicationmanagement.application.outboundservices.acl.ExternalPatientContextService;
import pe.edu.upc.medibridge.medicationmanagement.application.queryservices.AuthenticatedPatientAccessService;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.DeactivateMedicationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.RegisterMedicationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.UpdateMedicationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.UpdateMedicationStockCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.entities.Medication;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.events.MedicationExpiredEvent;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.events.MedicationRegisteredEvent;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.events.StockCriticallyLowEvent;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.exceptions.InvalidPatientReferenceException;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.exceptions.MedicationNotFoundException;
import pe.edu.upc.medibridge.medicationmanagement.domain.services.MedicationInventoryCommandService;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.messaging.publishers.MedicationIntegrationEventPublisher;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationRepository;
import pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories.MedicationScheduleRepository;

import java.util.Optional;

@Service
public class MedicationInventoryCommandServiceImpl implements MedicationInventoryCommandService {
    private final MedicationRepository medicationRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ExternalPatientContextService externalPatientContextService;
    private final MedicationIntegrationEventPublisher integrationEventPublisher;
    private final AuthenticatedPatientAccessService authenticatedPatientAccessService;

    public MedicationInventoryCommandServiceImpl(
            MedicationRepository medicationRepository,
            MedicationScheduleRepository medicationScheduleRepository,
            ApplicationEventPublisher eventPublisher,
            ExternalPatientContextService externalPatientContextService,
            MedicationIntegrationEventPublisher integrationEventPublisher,
            AuthenticatedPatientAccessService authenticatedPatientAccessService) {
        this.medicationRepository = medicationRepository;
        this.medicationScheduleRepository = medicationScheduleRepository;
        this.eventPublisher = eventPublisher;
        this.externalPatientContextService = externalPatientContextService;
        this.integrationEventPublisher = integrationEventPublisher;
        this.authenticatedPatientAccessService = authenticatedPatientAccessService;
    }

    @Override
    public Optional<Medication> handle(RegisterMedicationCommand command) {
        if (!externalPatientContextService.patientExists(command.patientId())) {
            throw new InvalidPatientReferenceException(command.patientId());
        }
        authenticatedPatientAccessService.requireAccess(command.requestedByUserId(), command.patientId());
        var medication = medicationRepository.save(new Medication(command));
        eventPublisher.publishEvent(new MedicationRegisteredEvent(medication.getId(), medication.getPatientId()));
        integrationEventPublisher.publishMedicationRegistered(medication);
        if (medication.isLowStock()) {
            eventPublisher.publishEvent(new StockCriticallyLowEvent(medication.getId(), medication.getPatientId(), medication.getStockQuantity()));
            integrationEventPublisher.publishStockLow(medication);
        }
        if (medication.isExpired()) {
            eventPublisher.publishEvent(new MedicationExpiredEvent(medication.getId(), medication.getPatientId()));
        }
        return Optional.of(medication);
    }

    @Override
    @Transactional
    public Optional<Medication> handle(UpdateMedicationStockCommand command) {
        var medication = medicationRepository.findByIdForUpdate(command.medicationId())
                .orElseThrow(() -> new MedicationNotFoundException(command.medicationId()));
        authenticatedPatientAccessService.requireAccess(command.requestedByUserId(), medication.getPatientId());
        if (!medication.isActive()) {
            throw new IllegalStateException("Cannot update stock for an inactive medication");
        }
        medication.updateStock(command.stockQuantity());
        var updatedMedication = medicationRepository.save(medication);
        if (updatedMedication.isLowStock()) {
            eventPublisher.publishEvent(new StockCriticallyLowEvent(
                    updatedMedication.getId(),
                    updatedMedication.getPatientId(),
                    updatedMedication.getStockQuantity()));
            integrationEventPublisher.publishStockLow(updatedMedication);
        }
        return Optional.of(updatedMedication);
    }

    @Override
    @Transactional
    public Optional<Medication> handle(UpdateMedicationCommand command) {
        var medication = medicationRepository.findByIdForUpdate(command.medicationId())
                .orElseThrow(() -> new MedicationNotFoundException(command.medicationId()));
        authenticatedPatientAccessService.requireAccess(command.requestedByUserId(), medication.getPatientId());
        if (!medication.isActive()) {
            throw new IllegalStateException("Cannot update an inactive medication");
        }
        medication.update(command);
        var updatedMedication = medicationRepository.save(medication);
        if (updatedMedication.isLowStock()) {
            eventPublisher.publishEvent(new StockCriticallyLowEvent(
                    updatedMedication.getId(),
                    updatedMedication.getPatientId(),
                    updatedMedication.getStockQuantity()));
            integrationEventPublisher.publishStockLow(updatedMedication);
        }
        if (updatedMedication.isExpired()) {
            eventPublisher.publishEvent(new MedicationExpiredEvent(updatedMedication.getId(), updatedMedication.getPatientId()));
        }
        return Optional.of(updatedMedication);
    }

    @Override
    @Transactional
    public void handle(DeactivateMedicationCommand command) {
        var medication = medicationRepository.findByIdForUpdate(command.medicationId())
                .orElseThrow(() -> new MedicationNotFoundException(command.medicationId()));
        authenticatedPatientAccessService.requireAccess(command.requestedByUserId(), medication.getPatientId());

        medication.deactivate();
        var activeSchedules = medicationScheduleRepository.findByMedicationIdAndActiveTrue(command.medicationId());
        activeSchedules.forEach(schedule -> schedule.deactivate());

        medicationRepository.save(medication);
        medicationScheduleRepository.saveAll(activeSchedules);
    }
}

