package com.project.back_end.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Entity class representing an Admin user in the system.
 * Used for authentication and administrative actions.
 */
@Entity
@Table(name = "admins")
public class Admin {

    // ---------------------------------------------------------
    // 1. Primary Key: ID
    // ---------------------------------------------------------
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ---------------------------------------------------------
    // 2. Username
    // ---------------------------------------------------------
    @NotNull(message = "Username cannot be null")
    @Column(nullable = false, unique = true)
    private String username;

    // ---------------------------------------------------------
    // 3. Password
    // ---------------------------------------------------------
    @NotNull(message = "Password cannot be null")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    // ---------------------------------------------------------
    // 4. Constructors
    // ---------------------------------------------------------

    // Default constructor (required by JPA)
    public Admin() {}

    // Parameterized constructor
    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // ---------------------------------------------------------
    // 5. Getters and Setters
    // ---------------------------------------------------------
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // ---------------------------------------------------------
    // 6. toString (exclude password)
    // ---------------------------------------------------------
    @Override
    public String toString() {
        return "Admin{" +
                "id=" + id +
                ", username='" + username + '\'' +
                '}';
    }
}

