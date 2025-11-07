package com.project.back_end.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * MongoDB document representing a prescription record in the Smart Clinic Management System.
 * Each prescription is tied to a specific appointment and includes medication details and optional doctor notes.
 */
@Document(collection = "prescriptions")
public class Prescription {

    // ---------------------------------------------------------
    // 1. Primary Key: MongoDB ID
    // ---------------------------------------------------------
    @Id
    private String id;  // MongoDB ObjectId stored as a string

    // ---------------------------------------------------------
    // 2. Patient Name
    // ---------------------------------------------------------
    @NotNull(message = "Patient name cannot be null")
    @Size(min = 3, max = 100, message = "Patient name must be between 3 and 100 characters")
    private String patientName;

    // ---------------------------------------------------------
    // 3. Appointment ID
    // ---------------------------------------------------------
    @NotNull(message = "Appointment ID cannot be null")
    private Long appointmentId;

    // ---------------------------------------------------------
    // 4. Medication
    // ---------------------------------------------------------
    @NotNull(message = "Medication cannot be null")
    @Size(min = 3, max = 100, message = "Medication must be between 3 and 100 characters")
    private String medication;

    // ---------------------------------------------------------
    // 5. Dosage
    // ---------------------------------------------------------
    @NotNull(message = "Dosage cannot be null")
    @Size(min = 3, max = 50, message = "Dosage must be between 3 and 50 characters")
    private String dosage;

    // ---------------------------------------------------------
    // 6. Doctor Notes
    // ---------------------------------------------------------
    @Size(max = 200, message = "Doctor notes cannot exceed 200 characters")
    private String doctorNotes;

    // ---------------------------------------------------------
    // 7. Constructors
    // ---------------------------------------------------------
    public Prescription() {}

    public Prescription(String patientName, Long appointmentId, String medication, String dosage, String doctorNotes) {
        this.patientName = patientName;
        this.appointmentId = appointmentId;
        this.medication = medication;
        this.dosage = dosage;
        this.doctorNotes = doctorNotes;
    }

    // ---------------------------------------------------------
    // 8. Getters and Setters
    // ---------------------------------------------------------
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getMedication() {
        return medication;
    }

    public void setMedication(String medication) {
        this.medication = medication;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getDoctorNotes() {
        return doctorNotes;
    }

    public void setDoctorNotes(String doctorNotes) {
        this.doctorNotes = doctorNotes;
    }

    // ---------------------------------------------------------
    // 9. toString
    // ---------------------------------------------------------
    @Override
    public String toString() {
        return "Prescription{" +
                "id='" + id + '\'' +
                ", patientName='" + patientName + '\'' +
                ", appointmentId=" + appointmentId +
                ", medication='" + medication + '\'' +
                ", dosage='" + dosage + '\'' +
                ", doctorNotes='" + doctorNotes + '\'' +
                '}';
    }
}

