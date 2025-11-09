package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity class representing a Doctor in the Smart Clinic Management System.
 * Each doctor has a name, specialty, contact details, and available time slots.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    // ---------------------------------------------------------
    // 1. Primary Key: ID
    // ---------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------------------------------------------------------
    // 2. Name
    // ---------------------------------------------------------
    @NotNull(message = "Doctor name cannot be null")
    @Size(min = 3, max = 100, message = "Doctor name must be between 3 and 100 characters")
    @Column(nullable = false)
    private String name;

    // ---------------------------------------------------------
    // 3. Specialty
    // ---------------------------------------------------------
    @NotNull(message = "Specialty cannot be null")
    @Size(min = 3, max = 50, message = "Specialty must be between 3 and 50 characters")
    @Column(nullable = false)
    private String specialty;

    // ---------------------------------------------------------
    // 4. Email
    // ---------------------------------------------------------
    @NotNull(message = "Email cannot be null")
    @Email(message = "Invalid email format")
    @Column(nullable = false, unique = true)
    private String email;

    // ---------------------------------------------------------
    // 5. Password
    // ---------------------------------------------------------
    @NotNull(message = "Password cannot be null")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    // ---------------------------------------------------------
    // 6. Phone Number
    // ---------------------------------------------------------
    @NotNull(message = "Phone number cannot be null")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    @Column(nullable = false, unique = true)
    private String phone;

    // ---------------------------------------------------------
    // 7. Available Times
    // ---------------------------------------------------------
    @ElementCollection
    @CollectionTable(name = "doctor_available_times", joinColumns = @JoinColumn(name = "doctor_id"))
    @Column(name = "time_slot")
    private List<String> availableTimes = new ArrayList<>();

    // ---------------------------------------------------------
    // 8. Constructors
    // ---------------------------------------------------------
    public Doctor() {}

    public Doctor(String name, String specialty, String email, String password, String phone, List<String> availableTimes) {
        this.name = name;
        this.specialty = specialty;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.availableTimes = availableTimes;
    }

    // ---------------------------------------------------------
    // 9. Getters and Setters
    // ---------------------------------------------------------
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<String> getAvailableTimes() {
        return availableTimes;
    }

    public void setAvailableTimes(List<String> availableTimes) {
        this.availableTimes = availableTimes;
    }

    // ---------------------------------------------------------
    // 10. toString (hide password for security)
    // ---------------------------------------------------------
    @Override
    public String toString() {
        return "Doctor{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", specialty='" + specialty + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", availableTimes=" + availableTimes +
                '}';
    }
}

