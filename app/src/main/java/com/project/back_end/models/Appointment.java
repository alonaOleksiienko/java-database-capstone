package com.project.back_end.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity class representing an appointment in the Smart Clinic Management System.
 * Each appointment links a doctor with a patient for a specific date and time.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

    // ---------------------------------------------------------
    // 1. Primary Key: ID
    // ---------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------------------------------------------------------
    // 2. Doctor Relationship
    // ---------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    @NotNull(message = "Doctor cannot be null")
    private Doctor doctor;

    // ---------------------------------------------------------
    // 3. Patient Relationship
    // ---------------------------------------------------------
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @NotNull(message = "Patient cannot be null")
    private Patient patient;

    // ---------------------------------------------------------
    // 4. Appointment Time
    // ---------------------------------------------------------
    @Future(message = "Appointment time must be in the future")
    @Column(name = "appointment_time", nullable = false)
    private LocalDateTime appointmentTime;

    // ---------------------------------------------------------
    // 5. Status (0 = Scheduled, 1 = Completed)
    // ---------------------------------------------------------
    @NotNull(message = "Status cannot be null")
    @Column(nullable = false)
    private int status = 0;

    // ---------------------------------------------------------
    // 6. Constructors
    // ---------------------------------------------------------
    public Appointment() {}

    public Appointment(Doctor doctor, Patient patient, LocalDateTime appointmentTime, int status) {
        this.doctor = doctor;
        this.patient = patient;
        this.appointmentTime = appointmentTime;
        this.status = status;
    }

    // ---------------------------------------------------------
    // 7. Getters and Setters
    // ---------------------------------------------------------
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    // ---------------------------------------------------------
    // 8. Derived Time Getters
    // ---------------------------------------------------------

    /**
     * Calculates and returns the end time of the appointment.
     * The appointment is assumed to last for 1 hour.
     */
    @Transient
    public LocalDateTime getEndTime() {
        if (appointmentTime == null) return null;
        return appointmentTime.plusHours(1);
    }

    /**
     * Extracts and returns only the date from the appointment time.
     */
    @Transient
    public LocalDate getAppointmentDate() {
        if (appointmentTime == null) return null;
        return appointmentTime.toLocalDate();
    }

    /**
     * Extracts and returns only the time from the appointment time.
     */
    @Transient
    public LocalTime getAppointmentTimeOnly() {
        if (appointmentTime == null) return null;
        return appointmentTime.toLocalTime();
    }

    // ---------------------------------------------------------
    // 9. toString (excluding heavy relationships)
    // ---------------------------------------------------------
    @Override
    public String toString() {
        return "Appointment{" +
                "id=" + id +
                ", doctor=" + (doctor != null ? doctor.getId() : null) +
                ", patient=" + (patient != null ? patient.getId() : null) +
                ", appointmentTime=" + appointmentTime +
                ", status=" + status +
                '}';
    }
}

