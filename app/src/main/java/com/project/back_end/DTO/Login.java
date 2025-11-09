package com.project.back_end.DTO;

/**
 * Data Transfer Object (DTO) for login requests.
 * Contains user credentials (email and password) used for authentication.
 */
public class Login {

    // ------------------------
    // 1. Fields
    // ------------------------
    private String email;
    private String password;

    // ------------------------
    // 2. Default Constructor
    // ------------------------
    public Login() {
        // Default constructor — allows frameworks like Spring to instantiate automatically
    }

    // ------------------------
    // 3. Getters and Setters
    // ------------------------
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

    // ------------------------
    // 4. Optional: toString() for logging/debugging (avoid printing password in production)
    // ------------------------
    @Override
    public String toString() {
        return "Login{" +
                "email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}

