package com.project.back_end.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Data Transfer Object (DTO) for Appointment entities.
 * Provides simplified doctor/patient details and derived date/time fields.
 */
public class AppointmentDTO {

    // ------------------------
    // 1. Core Fields
    // ------------------------
    private Long id;
    private Long doctorId;
    private String doctorName;
    private Long patientId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;
    private String patientAddress;
    private LocalDateTime appointmentTime;
    private int status; // 0: Scheduled, 1: Completed, etc.

    // ------------------------
    // 2. Derived Fields (custom getters)
    // ------------------------
    private LocalDate appointmentDate;
    private LocalTime appointmentTimeOnly;
    private LocalDateTime endTime;

    // ------------------------
    // 3. Constructor
    // ------------------------
    public AppointmentDTO(Long id,
                          Long doctorId,
                          String doctorName,
                          Long patientId,
                          String patientName,
                          String patientEmail,
                          String patientPhone,
                          String patientAddress,
                          LocalDateTime appointmentTime,
                          int status) {
        this.id = id;
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.patientId = patientId;
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.patientPhone = patientPhone;
        this.patientAddress = patientAddress;
        this.appointmentTime = appointmentTime;
        this.status = status;

        // Derived fields
        if (appointmentTime != null) {
            this.appointmentDate = appointmentTime.toLocalDate();
            this.appointmentTimeOnly = appointmentTime.toLocalTime();
            this.endTime = appointmentTime.plusHours(1);
        }
    }

    // ------------------------
    // 4. Getters
    // ------------------------
    public Long getId() {
        return id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public String getPatientAddress() {
        return patientAddress;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public int getStatus() {
        return status;
    }

    public LocalDate getAppointmentDate() {
        if (appointmentTime != null) {
            return appointmentTime.toLocalDate();
        }
        return appointmentDate;
    }

    public LocalTime getAppointmentTimeOnly() {
        if (appointmentTime != null) {
            return appointmentTime.toLocalTime();
        }
        return appointmentTimeOnly;
    }

    public LocalDateTime getEndTime() {
        if (appointmentTime != null) {
            return appointmentTime.plusHours(1);
        }
        return endTime;
    }

    // ------------------------
    // 5. Optional: toString() for easy logging/debugging
    // ------------------------
    @Override
    public String toString() {
        return "AppointmentDTO{" +
                "id=" + id +
                ", doctorId=" + doctorId +
                ", doctorName='" + doctorName + '\'' +
                ", patientId=" + patientId +
                ", patientName='" + patientName + '\'' +
                ", patientEmail='" + patientEmail + '\'' +
                ", patientPhone='" + patientPhone + '\'' +
                ", patientAddress='" + patientAddress + '\'' +
                ", appointmentTime=" + appointmentTime +
                ", status=" + status +
                ", appointmentDate=" + getAppointmentDate() +
                ", appointmentTimeOnly=" + getAppointmentTimeOnly() +
                ", endTime=" + getEndTime() +
                '}';
    }
}

