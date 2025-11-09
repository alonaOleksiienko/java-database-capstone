package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API controller for handling Appointment operations:
 * - Query by date/patient
 * - Book, update, and cancel appointments
 *
 * Base path: /appointments
 */
@RestController
@RequestMapping("/appointments")
@Validated
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final Service service; // shared utilities: token validation, common filters, etc.

    @Autowired
    public AppointmentController(AppointmentService appointmentService, Service service) {
        this.appointmentService = appointmentService;
        this.service = service;
    }

    // ------------------------------------------------------------
    // 3) GET appointments for a date (and optional patient name)
    //    Path variables: {date} {patientName} {token}
    //    - Validates token for role "doctor"
    // ------------------------------------------------------------
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<?> getAppointments(
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable String patientName,
            @PathVariable String token
    ) {
        // Validate token for doctor role
        if (!service.isTokenValid("doctor", token)) {
            return unauthorized("Invalid or expired token for role 'doctor'. Please log in again.");
        }

        // Backend may expect literal "null" to mean "no filter"
        String normalizedPatient = (patientName == null || patientName.isBlank() || "null".equalsIgnoreCase(patientName))
                ? null
                : patientName.trim();

        try {
            List<Appointment> appts = appointmentService.findByDateAndPatient(date, normalizedPatient);
            Map<String, Object> body = new HashMap<>();
            body.put("date", date.toString());
            body.put("patientName", normalizedPatient);
            body.put("appointments", appts);
            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            return serverError("Failed to fetch appointments", ex.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 4) POST book appointment
    //    Path variable: {token}, Body: Appointment (validated)
    //    - Validates token for role "patient"
    // ------------------------------------------------------------
    @PostMapping("/book/{token}")
    public ResponseEntity<?> bookAppointment(
            @PathVariable String token,
            @Valid @RequestBody Appointment appointment
    ) {
        if (!service.isTokenValid("patient", token)) {
            return unauthorized("Invalid or expired token for role 'patient'. Please log in again.");
        }

        try {
            // Let service enforce availability (doctor exists, slot free, future time, etc.)
            Map<String, Object> result = appointmentService.book(appointment);
            // Expected result keys (example): { success: true/false, message: "...", appointment: <Appointment> }
            boolean success = (boolean) result.getOrDefault("success", false);
            if (success) {
                return ResponseEntity.status(HttpStatus.CREATED).body(result);
            } else {
                // Could be 400 (validation), 404 (doctor not found), or 409 (slot conflict) — use provided code if present
                HttpStatus status = HttpStatus.valueOf((int) result.getOrDefault("status", HttpStatus.BAD_REQUEST.value()));
                return ResponseEntity.status(status).body(result);
            }
        } catch (Exception ex) {
            return serverError("Failed to book appointment", ex.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 5) PUT update appointment
    //    Path variable: {token}, Body: Appointment (validated)
    //    - Validates token for role "patient"
    // ------------------------------------------------------------
    @PutMapping("/{token}")
    public ResponseEntity<?> updateAppointment(
            @PathVariable String token,
            @Valid @RequestBody Appointment appointment
    ) {
        if (!service.isTokenValid("patient", token)) {
            return unauthorized("Invalid or expired token for role 'patient'. Please log in again.");
        }

        try {
            Map<String, Object> result = appointmentService.update(appointment);
            boolean success = (boolean) result.getOrDefault("success", false);
            HttpStatus status = success ? HttpStatus.OK
                    : HttpStatus.valueOf((int) result.getOrDefault("status", HttpStatus.BAD_REQUEST.value()));
            return ResponseEntity.status(status).body(result);
        } catch (Exception ex) {
            return serverError("Failed to update appointment", ex.getMessage());
        }
    }

    // ------------------------------------------------------------
    // 6) DELETE cancel appointment
    //    Path variables: {appointmentId} {token}
    //    - Validates token for role "patient"
    // ------------------------------------------------------------
    @DeleteMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> cancelAppointment(
            @PathVariable Long appointmentId,
            @PathVariable String token
    ) {
        if (!service.isTokenValid("patient", token)) {
            return unauthorized("Invalid or expired token for role 'patient'. Please log in again.");
        }

        try {
            Map<String, Object> result = appointmentService.cancel(appointmentId);
            boolean success = (boolean) result.getOrDefault("success", false);
            HttpStatus status = success ? HttpStatus.OK
                    : HttpStatus.valueOf((int) result.getOrDefault("status", HttpStatus.NOT_FOUND.value()));
            return ResponseEntity.status(status).body(result);
        } catch (Exception ex) {
            return serverError("Failed to cancel appointment", ex.getMessage());
        }
    }

    // ----------------- helpers -----------------

    private ResponseEntity<Map<String, Object>> unauthorized(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    private ResponseEntity<Map<String, Object>> serverError(String message, String detail) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("detail", detail);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

