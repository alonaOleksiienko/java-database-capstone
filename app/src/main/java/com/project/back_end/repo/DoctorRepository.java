package com.project.back_end.repo;

import com.project.back_end.models.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Doctor entity.
 * Provides CRUD operations, custom queries, and search functionality for managing doctor data.
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Finds a doctor by email.
     *
     * @param email The email of the doctor.
     * @return The Doctor entity if found, otherwise null.
     */
    Doctor findByEmail(String email);

    /**
     * Finds doctors whose names partially match the provided value (case-sensitive).
     *
     * @param name The name or partial name of the doctor.
     * @return A list of doctors with matching names.
     */
    List<Doctor> findByNameLike(String name);

    /**
     * Finds doctors whose names contain the provided value (case-insensitive)
     * and whose specialty matches exactly (case-insensitive).
     *
     * @param name      The name or partial name of the doctor (case-insensitive).
     * @param specialty The specialty to filter by (case-insensitive).
     * @return A list of doctors matching both filters.
     */
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(String name, String specialty);

    /**
     * Finds doctors by specialty (case-insensitive).
     *
     * @param specialty The specialty to search for.
     * @return A list of doctors matching the specialty.
     */
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);
}

