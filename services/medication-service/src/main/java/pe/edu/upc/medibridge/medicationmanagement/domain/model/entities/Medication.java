package pe.edu.upc.medibridge.medicationmanagement.domain.model.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.RegisterMedicationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.commands.UpdateMedicationCommand;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.AdministrationRoute;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.valueobjects.DosageUnit;
import pe.edu.upc.medibridge.shared.domain.model.entities.AuditableModel;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@NoArgsConstructor
public class Medication extends AuditableModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(nullable = false)
    private Long patientId;

    @NotBlank
    @Column(nullable = false, length = 120)
    private String name;

    @Positive
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dosageAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DosageUnit dosageUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdministrationRoute administrationRoute;

    @PositiveOrZero
    @Column(nullable = false)
    private Integer stockQuantity;

    @PositiveOrZero
    @Column(nullable = false)
    private Integer lowStockThreshold;

    @Column(nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false)
    private boolean active;

    public Medication(RegisterMedicationCommand command) {
        this.patientId = command.patientId();
        this.name = command.name();
        this.dosageAmount = command.dosageAmount();
        this.dosageUnit = command.dosageUnit();
        this.administrationRoute = command.administrationRoute();
        this.stockQuantity = command.stockQuantity();
        this.lowStockThreshold = command.lowStockThreshold();
        this.expirationDate = command.expirationDate();
        this.active = true;
    }

    public void updateStock(Integer stockQuantity) {
        if (stockQuantity == null || stockQuantity < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        this.stockQuantity = stockQuantity;
    }

    public void update(UpdateMedicationCommand command) {
        if (command.name() == null || command.name().isBlank()) {
            throw new IllegalArgumentException("Medication name is required");
        }
        if (command.dosageAmount() == null || command.dosageAmount().signum() <= 0) {
            throw new IllegalArgumentException("Dosage amount must be positive");
        }
        if (command.dosageUnit() == null) {
            throw new IllegalArgumentException("Dosage unit is required");
        }
        if (command.administrationRoute() == null) {
            throw new IllegalArgumentException("Administration route is required");
        }
        if (command.stockQuantity() == null || command.stockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be negative");
        }
        if (command.lowStockThreshold() == null || command.lowStockThreshold() < 0) {
            throw new IllegalArgumentException("Low stock threshold cannot be negative");
        }
        if (command.expirationDate() == null) {
            throw new IllegalArgumentException("Expiration date is required");
        }

        this.name = command.name();
        this.dosageAmount = command.dosageAmount();
        this.dosageUnit = command.dosageUnit();
        this.administrationRoute = command.administrationRoute();
        this.stockQuantity = command.stockQuantity();
        this.lowStockThreshold = command.lowStockThreshold();
        this.expirationDate = command.expirationDate();
    }

    public void deactivate() {
        this.active = false;
    }

    public void decreaseStock() {
        if (stockQuantity <= 0) {
            throw new IllegalStateException("Medication stock is insufficient");
        }
        this.stockQuantity--;
    }

    public boolean isLowStock() {
        return stockQuantity <= lowStockThreshold;
    }

    public boolean isExpired() {
        return expirationDate.isBefore(LocalDate.now());
    }
}

