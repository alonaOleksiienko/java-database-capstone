package com.project.back_end.repo;

import com.project.back_end.models.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Patient entity.
 * Provides CRUD operations and custom query methods for patient management.
 */
@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    /**
     * Finds a patient by their email address.
     *
     * @param email The patient's email.
     * @return The Patient entity if found, otherwise null.
     */
    Patient findByEmail(String email);

    /**
     * Finds a patient by their email or phone number.
     * Useful for login or duplicate record checks.
     *
     * @param email The patient's email.
     * @param phone The patient's phone number.
     * @return The Patient entity if found, otherwise null.
     */
    Patient findByEmailOrPhone(String email, String phone);
}


