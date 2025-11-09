package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * Entity class representing a Patient in the Smart Clinic Management System.
 * Each patient has a unique ID, name, contact information, and login credentials.
 */
@Entity
@Table(name = "patients")
public class Patient {

    // ---------------------------------------------------------
    // 1. Primary Key: ID
    // ---------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------------------------------------------------------
    // 2. Name
    // ---------------------------------------------------------
    @NotNull(message = "Patient name cannot be null")
    @Size(min = 3, max = 100, message = "Patient name must be between 3 and 100 characters")
    @Column(nullable = false)
    private String name;

    // ---------------------------------------------------------
    // 3. Email
    // ---------------------------------------------------------
    @NotNull(message = "Email cannot be null")
    @Email(message = "Invalid email format")
    @Column(nullable = false, unique = true)
    private String email;

    // ---------------------------------------------------------
    // 4. Password
    // ---------------------------------------------------------
    @NotNull(message = "Password cannot be null")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    // ---------------------------------------------------------
    // 5. Phone Number
    // ---------------------------------------------------------
    @NotNull(message = "Phone number cannot be null")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    @Column(nullable = false, unique = true)
    private String phone;

    // ---------------------------------------------------------
    // 6. Address
    // ---------------------------------------------------------
    @NotNull(message = "Address cannot be null")
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    @Column(nullable = false)
    private String address;

    // ---------------------------------------------------------
    // 7. Constructors
    // ---------------------------------------------------------
    public Patient() {}

    public Patient(String name, String email, String password, String phone, String address) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
    }

    // ---------------------------------------------------------
    // 8. Getters and Setters
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    // ---------------------------------------------------------
    // 9. toString (excluding password)
    // ---------------------------------------------------------
    @Override
    public String toString() {
        return "Patient{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}

