package com.project.back_end.controllers;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.PrescriptionService;
import com.project.back_end_shared.services.Service; // <-- adjust to your actual shared Service package
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for Prescription operations.
 * Base path: ${api.path}prescription
 */
@RestController
@RequestMapping("${api.path}prescription")
@Validated
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final AppointmentService appointmentService;
    private final Service service; // shared utilities: token validation, role checks, etc.

    public PrescriptionController(PrescriptionService prescriptionService,
                                  AppointmentService appointmentService,
                                  Service service) {
        this.prescriptionService = prescriptionService;
        this.appointmentService = appointmentService;
        this.service = service;
    }

    // ---------------------------------------------------------------------
    // 3) Save a new prescription (doctor-only)
    //     POST  /{token}
    //     Body: Prescription (validated)
    // ---------------------------------------------------------------------
    @PostMapping("/{token}")
    public ResponseEntity<?> savePrescription(
            @Valid @RequestBody Prescription prescription,
            @PathVariable String token) {

        // Validate doctor token
        if (!service.isTokenValid("doctor", token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(resp(false, "Invalid or expired token for role 'doctor'."));
        }

        try {
            // Persist prescription in MongoDB
            Prescription saved = prescriptionService.save(prescription);

            // Update appointment status to reflect a prescription was added
            // Adjust to your actual service method name:
            // e.g., appointmentService.markPrescriptionAdded(saved.getAppointmentId());
            appointmentService.markPrescriptionAdded(saved.getAppointmentId());

            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("message", "Prescription saved and appointment updated.");
            body.put("prescription", saved);
            return ResponseEntity.status(HttpStatus.CREATED).body(body);

        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(resp(false, iae.getMessage()));
        } catch (Exception ex) {
            return serverError("Failed to save prescription", ex.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // 4) Get prescription by appointmentId (doctor-only)
    //     GET  /by-appointment/{appointmentId}/{token}
    // ---------------------------------------------------------------------
    @GetMapping("/by-appointment/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescription(
            @PathVariable Long appointmentId,
            @PathVariable String token) {

        // Validate doctor token
        if (!service.isTokenValid("doctor", token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(resp(false, "Invalid or expired token for role 'doctor'."));
        }

        try {
            Prescription p = prescriptionService.findByAppointmentId(appointmentId);
            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(resp(false, "No prescription found for appointmentId=" + appointmentId));
            }
            Map<String, Object> body = new HashMap<>();
            body.put("success", true);
            body.put("prescription", p);
            return ResponseEntity.ok(body);

        } catch (Exception ex) {
            return serverError("Failed to fetch prescription", ex.getMessage());
//          Optionally include appointment existence check if you have it:
//          if (!appointmentService.exists(appointmentId)) { ... }
        }
    }

    // ----------------- helpers -----------------

    private Map<String, Object> resp(boolean success, String message) {
        Map<String, Object> m = new HashMap<>();
        m.put("success", success);
        m.put("message", message);
        return m;
    }

    private ResponseEntity<Map<String, Object>> serverError(String message, String detail) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        body.put("detail", detail);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

