package com.project.back_end.controllers;

import com.project.back_end.models.Doctor;
import com.project.back_end.models.Login;
import com.project.back_end.services.DoctorService;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller managing all doctor-related endpoints:
 * - Registration, Login, Update, Delete, Filtering, and Availability.
 * - Secured via role-based token validation.
 */
@RestController
@RequestMapping("${api.path}doctor")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private Service sharedService;

    // ===============================================================
    // 1. Doctor Availability
    // ===============================================================

    /**
     * GET /api/doctor/availability/{user}/{doctorId}/{date}/{token}
     * Check if a doctor is available for a specific date.
     */
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<Map<String, Object>> getDoctorAvailability(
            @PathVariable String user,
            @PathVariable Long doctorId,
            @PathVariable String date,
            @PathVariable String token) {

        Map<String, Object> response = new HashMap<>();

        if (!sharedService.validateToken(user, token)) {
            response.put("message", "Invalid or expired token.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        boolean available = doctorService.isDoctorAvailable(doctorId, date);
        response.put("doctorId", doctorId);
        response.put("date", date);
        response.put("available", available);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // ===============================================================
    // 2. Retrieve All Doctors
    // ===============================================================

    /**
     * GET /api/doctor/all
     * Return a list of all doctors.
     */
    @GetMapping("/all")
    public ResponseEntity<Map<String, Object>> getDoctor() {
        List<Doctor> doctors = doctorService.getAllDoctors();
        Map<String, Object> response = new HashMap<>();
        response.put("doctors", doctors);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // ===============================================================
    // 3. Register a New Doctor
    // ===============================================================

    /**
     * POST /api/doctor/save/{token}
     * Register a new doctor (admin-only).
     */
    @PostMapping("/save/{token}")
    public ResponseEntity<Map<String, Object>> saveDoctor(
            @Valid @RequestBody Doctor doctor,
            @PathVariable String token) {

        Map<String, Object> response = new HashMap<>();

        if (!sharedService.validateToken("admin", token)) {
            response.put("message", "Unauthorized access — only admin can add doctors.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (doctorService.existsByEmail(doctor.getEmail())) {
            response.put("message", "Doctor with email already exists.");
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }

        doctorService.saveDoctor(doctor);
        response.put("message", "Doctor registered successfully.");
        response.put("doctor", doctor);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ===============================================================
    // 4. Doctor Login
    // ===============================================================

    /**
     * POST /api/doctor/login
     * Authenticate a doctor and return a token.
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> doctorLogin(@Valid @RequestBody Login login) {
        return doctorService.doctorLogin(login);
    }

    // ===============================================================
    // 5. Update Doctor
    // ===============================================================

    /**
     * PUT /api/doctor/update/{token}
     * Update an existing doctor's details (admin-only).
     */
    @PutMapping("/update/{token}")
    public ResponseEntity<Map<String, Object>> updateDoctor(
            @Valid @RequestBody Doctor doctor,
            @PathVariable String token) {

        Map<String, Object> response = new HashMap<>();

        if (!sharedService.validateToken("admin", token)) {
            response.put("message", "Unauthorized access — only admin can update doctors.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (!doctorService.existsById(doctor.getId())) {
            response.put("message", "Doctor not found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        doctorService.updateDoctor(doctor);
        response.put("message", "Doctor updated successfully.");
        response.put("doctor", doctor);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // ===============================================================
    // 6. Delete Doctor
    // ===============================================================

    /**
     * DELETE /api/doctor/delete/{id}/{token}
     * Remove a doctor by ID (admin-only).
     */
    @DeleteMapping("/delete/{id}/{token}")
    public ResponseEntity<Map<String, Object>> deleteDoctor(
            @PathVariable Long id,
            @PathVariable String token) {

        Map<String, Object> response = new HashMap<>();

        if (!sharedService.validateToken("admin", token)) {
            response.put("message", "Unauthorized access — only admin can delete doctors.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (!doctorService.existsById(id)) {
            response.put("message", "Doctor not found.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        doctorService.deleteDoctor(id);
        response.put("message", "Doctor deleted successfully.");
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // ===============================================================
    // 7. Filter Doctors
    // ===============================================================

    /**
     * GET /api/doctor/filter/{name}/{time}/{speciality}
     * Filter doctors by name, available time, and specialty.
     */
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<Map<String, Object>> filterDoctors(
            @PathVariable String name,
            @PathVariable String time,
            @PathVariable String speciality) {

        Map<String, Object> response = new HashMap<>();
        List<Doctor> doctors = sharedService.filterDoctors(name, time, speciality);

        if (doctors.isEmpty()) {
            response.put("message", "No doctors found with the given filters.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        response.put("filteredDoctors", doctors);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}

