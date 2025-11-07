package com.project.back_end.controllers;

import com.project.back_end.models.Patient;
import com.project.back_end.models.dto.Login;
import com.project.back_end.services.PatientService;
import com.project_back_end_shared.services.Service; // <-- If your shared service is in a different package, adjust this import.
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API controller for Patient-related operations.
 * Base path: /patient
 */
@RestController
@RequestMapping("/patient")
@Validated
public class PatientController {

    private final PatientService patientService;
    private final Service service; // shared utilities: token validation, login validation, lookups

    @Autowired
    public PatientController(PatientService patientService, Service service) {
        this.patientService = patientService;
        this.service = service;
    }

    // ------------------------------------------------------------
    // 3) GET current patient by token
    //    - Validates token for role "patient"
    //    - Returns patient info if valid
    // ------------------------------------------------------------
    @GetMapping("/{token}")
    public ResponseEntity<?> getPatient(@PathVariable String token) {
        if (!service.isTokenValid("patient", token)) {
            return unauthorized("Invalid or expired token for role 'patient'. Please log in again.");
        }

        try {
            // If your shared service exposes a way to derive patient by token, use it.
            // Otherwise, adapt to your implementation (e.g., patientService.findByToken(token)).
            Patient patient = service.getPatientFromToken(token);
            if (patient == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(mapOf(false, "Patient not found for provided token"));
            }
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("patient", patient);
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            return serverError("Failed to retrieve patient", ex.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 4) POST register patient
    //    - Validates input
    //    - Checks if patient exists (shared service)
    // ------------------------------------------------------------
    @PostMapping("/register")
    public ResponseEntity<?> createPatient(@Valid @RequestBody Patient patient) {
        try {
            if (service.isPatientExists(patient.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(mapOf(false, "Patient already exists with this email"));
            }

            Patient saved = patientService.create(patient);
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("message", "Patient registered successfully");
            body.put("patient", saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(body);
        } catch (Exception ex) {
            return serverError("Failed to register patient", ex.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 5) POST patient login
    //    - Delegates to shared service for authentication
    // ------------------------------------------------------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody Login login) {
        try {
            Map<String, Object> result = service.validatePatientLogin(login);
            // expected: { success: boolean, message: String, token: String, patient: Patient, status?: int }
            boolean success = (boolean) result.getOrDefault("success", false);
            HttpStatus status = HttpStatus.valueOf((int) result.getOrDefault("status",
                    success ? HttpStatus.OK.value() : HttpStatus.UNAUTHORIZED.value()));
            return ResponseEntity.status(status).body(result);
        } catch (Exception ex) {
            return serverError("Login failed", ex.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 6) GET appointments for a patient
    //    Path: /{patientId}/appointments/{role}/{token}
    //    - Validates token (via shared service) based on provided role
    // ------------------------------------------------------------
    @GetMapping("/{patientId}/appointments/{role}/{token}")
    public ResponseEntity<?> getPatientAppointment(
            @PathVariable Long patientId,
            @PathVariable String role,
            @PathVariable String token
    ) {
        if (!service.isTokenValid(role, token)) {
            return unauthorized("Invalid or expired token for role '" + role + "'.");
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("appointments", patientService.getAppointmentsForPatient(patientId));
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            return serverError("Failed to fetch patient appointments", ex.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 7) GET filter a patient's appointments
    //    Path: /appointments/filter/{condition}/{name}/{token}
    //    - Token must be valid for role "patient"
    //    - Uses shared service to resolve patientId from token (adjust as needed)
    // ------------------------------------------------------------
    @GetMapping("/appointments/filter/{condition}/{name}/{token}")
    public ResponseEntity<?> filterPatientAppointment(
            @PathVariable String condition,
            @PathVariable String name,
            @PathVariable String token
    ) {
        if (!service.isTokenValid("patient", token)) {
            return unauthorized("Invalid or expired token for role 'patient'.");
        }

        try {
            Long patientId = service.getPatientIdFromToken(token); // adjust if you expose different method
            String normalizedName = (name == null || name.isBlank() || "null".equalsIgnoreCase(name)) ? null : name.trim();
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("condition", condition);
            body.put("name", normalizedName);
            body.put("appointments", patientService.filterAppointments(patientId, condition, normalizedName));
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            return serverError("Failed to filter patient appointments", ex.getMessage());
        }
    }

    // ----------------- helpers -----------------

    private Map<String, Object> mapOf(boolean success, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", success);
        m.put("message", message);
        return m;
    }

    private ResponseEntity<Map<String, Object>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(mapOf(false, message));
    }

    private ResponseEntity<Map<String, Object>> serverError(String message, String detail) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("detail", detail);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

