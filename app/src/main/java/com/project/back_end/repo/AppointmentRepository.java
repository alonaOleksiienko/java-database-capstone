package com.project.back_end.repo;

import com.project.back_end.models.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Appointment entity.
 * Provides CRUD operations, filtering, and custom update/delete queries for appointment management.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /**
     * Finds all appointments for a given doctor within a specified time range.
     *
     * @param doctorId The doctor's ID.
     * @param start    The start of the time range.
     * @param end      The end of the time range.
     * @return A list of appointments.
     */
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);

    /**
     * Finds all appointments for a doctor filtered by patient name (case-insensitive)
     * within a specified date range.
     *
     * @param doctorId    The doctor's ID.
     * @param patientName The patient's name (partial match).
     * @param start       The start of the time range.
     * @param end         The end of the time range.
     * @return A list of appointments matching the filters.
     */
    List<Appointment> findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
            Long doctorId, String patientName, LocalDateTime start, LocalDateTime end
    );

    /**
     * Deletes all appointments associated with a given doctor.
     *
     * @param doctorId The doctor's ID.
     */
    @Transactional
    @Modifying
    void deleteAllByDoctorId(Long doctorId);

    /**
     * Finds all appointments for a given patient.
     *
     * @param patientId The patient's ID.
     * @return A list of appointments.
     */
    List<Appointment> findByPatientId(Long patientId);

    /**
     * Finds all appointments for a patient with a specific status,
     * ordered by appointment time ascending.
     *
     * @param patientId The patient's ID.
     * @param status    The appointment status.
     * @return A list of appointments sorted by appointment time.
     */
    List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(Long patientId, int status);

    /**
     * Filters appointments by doctor's name (LIKE query) and patient ID.
     *
     * @param doctorName The doctor's name or partial name.
     * @param patientId  The patient's ID.
     * @return A list of appointments matching the filter.
     */
    @Query("SELECT a FROM Appointment a WHERE LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%')) AND a.patient.id = :patientId")
    List<Appointment> filterByDoctorNameAndPatientId(String doctorName, Long patientId);

    /**
     * Filters appointments by doctor's name (LIKE query), patient ID, and appointment status.
     *
     * @param doctorName The doctor's name or partial name.
     * @param patientId  The patient's ID.
     * @param status     The appointment status.
     * @return A list of appointments matching the filters.
     */
    @Query("SELECT a FROM Appointment a WHERE LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%')) " +
            "AND a.patient.id = :patientId AND a.status = :status")
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(String doctorName, Long patientId, int status);

    /**
     * Updates the status of a specific appointment.
     *
     * @param status The new status.
     * @param id     The appointment ID.
     */
    @Transactional
    @Modifying
    @Query("UPDATE Appointment a SET a.status = :status WHERE a.id = :id")
    void updateStatus(int status, long id);
}

