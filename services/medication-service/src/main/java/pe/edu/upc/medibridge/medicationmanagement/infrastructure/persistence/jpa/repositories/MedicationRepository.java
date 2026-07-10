package pe.edu.upc.medibridge.medicationmanagement.infrastructure.persistence.jpa.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upc.medibridge.medicationmanagement.domain.model.entities.Medication;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Integer> {
    List<Medication> findByPatientId(Long patientId);
    List<Medication> findByPatientIdAndActiveTrue(Long patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select medication from Medication medication where medication.id = :medicationId")
    Optional<Medication> findByIdForUpdate(@Param("medicationId") Integer medicationId);
}

