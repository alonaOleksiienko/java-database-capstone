package com.project.back_end.repo;

import com.project.back_end.models.Prescription;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Prescription documents in MongoDB.
 * Provides CRUD operations and custom queries for prescription management.
 */
@Repository
public interface PrescriptionRepository extends MongoRepository<Prescription, String> {

    /**
     * Finds prescriptions by their associated appointment ID.
     *
     * @param appointmentId The ID of the appointment linked to the prescription.
     * @return A list of prescriptions related to the given appointment ID.
     */
    List<Prescription> findByAppointmentId(Long appointmentId);
}

