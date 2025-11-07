package com.project.back_end.services;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Patient;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.TimeSlot;
import com.project.back_end.repositories.AdminRepository;
import com.project.back_end.repositories.PatientRepository;
import com.project.back_end.repositories.DoctorRepository;
import com.project.back_end.repositories.TimeSlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main Service class for handling common business logic across the application
 * Manages authentication, validation, and filtering operations
 */
@Service
public class ServiceClass {
    
    // Logger for tracking operations and errors
    private static final Logger logger = LoggerFactory.getLogger(ServiceClass.class);
    
    // Dependencies injected via constructor
    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final PatientService patientService;
    
    /**
     * Constructor Injection for Dependencies
     * Promotes loose coupling, improves testability, and ensures all required dependencies
     * are provided at object creation time
     *
     * @param tokenService - Service for JWT token operations
     * @param adminRepository - Repository for admin data access
     * @param patientRepository - Repository for patient data access
     * @param doctorRepository - Repository for doctor data access
     * @param timeSlotRepository - Repository for time slot data access
     * @param patientService - Service for patient-specific operations
     */
    public ServiceClass(TokenService tokenService,
                       AdminRepository adminRepository,
                       PatientRepository patientRepository,
                       DoctorRepository doctorRepository,
                       TimeSlotRepository timeSlotRepository,
                       PatientService patientService) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.patientService = patientService;
    }
    
    /**
     * Validates if the provided JWT token is valid for a specific user
     * Ensures security by preventing unauthorized access to protected resources
     *
     * @param token - JWT token to validate
     * @param username - Username to validate against the token
     * @return ResponseEntity with validation result or error status
     */
    public ResponseEntity<?> validateToken(String token, String username) {
        try {
            logger.info("Validating token for username: {}", username);
            
            // Validate inputs
            if (token == null || token.trim().isEmpty()) {
                logger.error("Token is null or empty");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Token is required"));
            }
            
            if (username == null || username.trim().isEmpty()) {
                logger.error("Username is null or empty");
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Username is required"));
            }
            
            // Validate token using TokenService
            boolean isValid = tokenService.validateToken(token, username);
            
            if (isValid) {
                logger.info("Token is valid for username: {}", username);
                Map<String, Object> response = new HashMap<>();
                response.put("valid", true);
                response.put("message", "Token is valid");
                response.put("username", username);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Invalid or expired token for username: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid or expired token"));
            }
            
        } catch (Exception e) {
            logger.error("Error validating token for username {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error validating token: " + e.getMessage()));
        }
    }
    
    /**
     * Validates admin login credentials and generates JWT token
     * Ensures only valid admin users can access secured parts of the system
     *
     * @param username - Admin username
     * @param password - Admin password
     * @return ResponseEntity with JWT token or error status
     */
    public ResponseEntity<?> validateAdmin(String username, String password) {
        try {
            logger.info("Validating admin login for username: {}", username);
            
            // Validate inputs
            if (username == null || username.trim().isEmpty()) {
                logger.error("Username is null or empty");
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Username is required"));
            }
            
            if (password == null || password.trim().isEmpty()) {
                logger.error("Password is null or empty");
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Password is required"));
            }
            
            // Search for admin by username
            Optional<Admin> adminOptional = adminRepository.findByUsername(username);
            
            if (!adminOptional.isPresent()) {
                logger.warn("Admin not found with username: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid credentials"));
            }
            
            Admin admin = adminOptional.get();
            
            // Validate password
            if (!password.equals(admin.getPassword())) {
                // In production, use BCrypt password encoder: passwordEncoder.matches(password, admin.getPassword())
                logger.warn("Invalid password for admin username: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid credentials"));
            }
            
            // Generate JWT token
            String token = tokenService.generateToken(admin.getUsername());
            
            logger.info("Admin login successful for username: {}", username);
            
            // Return token with success response
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", admin.getUsername());
            response.put("role", "ADMIN");
            response.put("message", "Login successful");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error validating admin login for username {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error during admin login: " + e.getMessage()));
        }
    }
    
    /**
     * Filters doctors based on name, specialty, and available time slots
     * Provides flexible filtering mechanism for searching doctors
     *
     * @param name - Doctor name (optional)
     * @param specialty - Doctor specialty (optional)
     * @param availableTime - Available time slot (optional)
     * @return ResponseEntity with filtered doctors or error status
     */
    public ResponseEntity<?> filterDoctor(String name, String specialty, LocalTime availableTime) {
        try {
            logger.info("Filtering doctors - Name: {}, Specialty: {}, Available Time: {}",
                name, specialty, availableTime);
            
            List<Doctor> doctors;
            
            // Case 1: All three filters provided
            if (name != null && !name.trim().isEmpty() &&
                specialty != null && !specialty.trim().isEmpty() &&
                availableTime != null) {
                
                logger.debug("Filtering by name, specialty, and available time");
                doctors = doctorRepository.findByNameAndSpecialtyAndAvailableTime(
                    name.trim(), specialty.trim(), availableTime);
            }
            // Case 2: Name and specialty provided
            else if (name != null && !name.trim().isEmpty() &&
                     specialty != null && !specialty.trim().isEmpty()) {
                
                logger.debug("Filtering by name and specialty");
                doctors = doctorRepository.findByNameAndSpecialty(name.trim(), specialty.trim());
            }
            // Case 3: Name and available time provided
            else if (name != null && !name.trim().isEmpty() && availableTime != null) {
                
                logger.debug("Filtering by name and available time");
                doctors = doctorRepository.findByNameAndAvailableTime(name.trim(), availableTime);
            }
            // Case 4: Specialty and available time provided
            else if (specialty != null && !specialty.trim().isEmpty() && availableTime != null) {
                
                logger.debug("Filtering by specialty and available time");
                doctors = doctorRepository.findBySpecialtyAndAvailableTime(specialty.trim(), availableTime);
            }
            // Case 5: Only name provided
            else if (name != null && !name.trim().isEmpty()) {
                
                logger.debug("Filtering by name only");
                doctors = doctorRepository.findByNameContainingIgnoreCase(name.trim());
            }
            // Case 6: Only specialty provided
            else if (specialty != null && !specialty.trim().isEmpty()) {
                
                logger.debug("Filtering by specialty only");
                doctors = doctorRepository.findBySpecialty(specialty.trim());
            }
            // Case 7: Only available time provided
            else if (availableTime != null) {
                
                logger.debug("Filtering by available time only");
                doctors = doctorRepository.findByAvailableTime(availableTime);
            }
            // Case 8: No filters - return all doctors
            else {
                logger.debug("No filters provided - returning all doctors");
                doctors = doctorRepository.findAll();
            }
            
            logger.info("Found {} doctors matching the criteria", doctors.size());
            
            // Remove sensitive information
            doctors.forEach(doctor -> doctor.setPassword(null));
            
            Map<String, Object> response = new HashMap<>();
            response.put("doctors", doctors);
            response.put("count", doctors.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error filtering doctors: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error filtering doctors: " + e.getMessage()));
        }
    }
    
    /**
     * Validates if the requested appointment time for a doctor is available
     * Prevents overlapping or invalid appointment bookings
     *
     * @param doctorId - ID of the doctor
     * @param appointmentDate - Date of the appointment
     * @param appointmentTime - Time of the appointment
     * @return 1 if valid, 0 if invalid time, -1 if doctor doesn't exist
     */
    public int validateAppointment(Long doctorId, LocalDate appointmentDate, LocalTime appointmentTime) {
        try {
            logger.info("Validating appointment for doctor ID: {} on {} at {}",
                doctorId, appointmentDate, appointmentTime);
            
            // Validate inputs
            if (doctorId == null || doctorId <= 0) {
                logger.error("Invalid doctor ID: {}", doctorId);
                return -1;
            }
            
            if (appointmentDate == null || appointmentTime == null) {
                logger.error("Appointment date or time is null");
                return 0;
            }
            
            // Check if doctor exists
            Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);
            
            if (!doctorOptional.isPresent()) {
                logger.warn("Doctor with ID {} not found", doctorId);
                return -1;
            }
            
            // Retrieve available time slots for the doctor on the specified date
            List<TimeSlot> timeSlots = timeSlotRepository
                .findByDoctorIdAndDate(doctorId, appointmentDate);
            
            if (timeSlots.isEmpty()) {
                logger.warn("No time slots found for doctor ID: {} on date: {}",
                    doctorId, appointmentDate);
                return 0;
            }
            
            // Check if the requested appointment time matches any available slot
            boolean timeSlotFound = timeSlots.stream()
                .anyMatch(slot -> slot.getStartTime().equals(appointmentTime) && slot.isAvailable());
            
            if (timeSlotFound) {
                logger.info("Valid appointment time found for doctor ID: {} on {} at {}",
                    doctorId, appointmentDate, appointmentTime);
                return 1;
            } else {
                logger.warn("No matching available time slot for doctor ID: {} on {} at {}",
                    doctorId, appointmentDate, appointmentTime);
                return 0;
            }
            
        } catch (Exception e) {
            logger.error("Error validating appointment for doctor ID {}: {}",
                doctorId, e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Validates if a patient with the same email or phone number already exists
     * Helps enforce uniqueness constraints and prevent duplicate entries
     *
     * @param email - Patient email
     * @param phoneNumber - Patient phone number
     * @return true if valid (no duplicates), false if duplicate found
     */
    public boolean validatePatient(String email, String phoneNumber) {
        try {
            logger.info("Validating patient with email: {} and phone: {}", email, phoneNumber);
            
            // Validate inputs
            if (email == null || email.trim().isEmpty()) {
                logger.error("Email is null or empty");
                return false;
            }
            
            // Check if patient with same email exists
            Optional<Patient> patientByEmail = patientRepository.findByEmail(email.trim());
            
            if (patientByEmail.isPresent()) {
                logger.warn("Patient with email {} already exists", email);
                return false;
            }
            
            // Check if patient with same phone number exists (if provided)
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                Optional<Patient> patientByPhone = patientRepository.findByPhoneNumber(phoneNumber.trim());
                
                if (patientByPhone.isPresent()) {
                    logger.warn("Patient with phone number {} already exists", phoneNumber);
                    return false;
                }
            }
            
            logger.info("Patient validation successful - no duplicates found");
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating patient: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Validates patient login credentials and generates JWT token
     * Ensures only legitimate patients can log in and access their data securely
     *
     * @param email - Patient email
     * @param password - Patient password
     * @return ResponseEntity with JWT token or error status
     */
    public ResponseEntity<?> validatePatientLogin(String email, String password) {
        try {
            logger.info("Validating patient login for email: {}", email);
            
            // Validate inputs
            if (email == null || email.trim().isEmpty()) {
                logger.error("Email is null or empty");
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Email is required"));
            }
            
            if (password == null || password.trim().isEmpty()) {
                logger.error("Password is null or empty");
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Password is required"));
            }
            
            // Look up patient by email
            Optional<Patient> patientOptional = patientRepository.findByEmail(email.trim());
            
            if (!patientOptional.isPresent()) {
                logger.warn("Patient not found with email: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid credentials"));
            }
            
            Patient patient = patientOptional.get();
            
            // Validate password
            if (!password.equals(patient.getPassword())) {
                // In production, use BCrypt: passwordEncoder.matches(password, patient.getPassword())
                logger.warn("Invalid password for patient email: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid credentials"));
            }
            
            // Generate JWT token
            String token = tokenService.generateToken(patient.getEmail());
            
            logger.info("Patient login successful for email: {}", email);
            
            // Return token with patient info (excluding password)
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("patientId", patient.getId());
            response.put("email", patient.getEmail());
            response.put("firstName", patient.getFirstName());
            response.put("lastName", patient.getLastName());
            response.put("role", "PATIENT");
            response.put("message", "Login successful");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error validating patient login for email {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error during patient login: " + e.getMessage()));
        }
    }
    
    /**
     * Filters a patient's appointment history based on condition and doctor name
     * Supports patient-specific querying and enhances user experience
     *
     * @param token - JWT token to identify the patient
     * @param condition - "past" or "future" (optional)
     * @param doctorName - Name of the doctor (optional)
     * @return ResponseEntity with filtered appointments or error status
     */
    public ResponseEntity<?> filterPatient(String token, String condition, String doctorName) {
        try {
            logger.info("Filtering patient appointments - Condition: {}, Doctor: {}",
                condition, doctorName);
            
            // Validate token
            if (token == null || token.trim().isEmpty()) {
                logger.error("Token is null or empty");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Authentication token is required"));
            }
            
            // Extract email from token
            String email = tokenService.extractEmail(token);
            
            if (email == null || email.trim().isEmpty()) {
                logger.error("Failed to extract email from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("Invalid or expired token"));
            }
            
            logger.info("Extracted email from token: {}", email);
            
            // Get patient by email
            Optional<Patient> patientOptional = patientRepository.findByEmail(email);
            
            if (!patientOptional.isPresent()) {
                logger.warn("Patient with email {} not found", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Patient not found"));
            }
            
            Patient patient = patientOptional.get();
            Long patientId = patient.getId();
            
            // Delegate filtering to PatientService based on provided filters
            
            // Case 1: Both condition and doctor name provided
            if (condition != null && !condition.trim().isEmpty() &&
                doctorName != null && !doctorName.trim().isEmpty()) {
                
                logger.debug("Filtering by condition and doctor name");
                return patientService.filterByDoctorAndCondition(patientId, doctorName.trim(), condition.trim());
            }
            // Case 2: Only condition provided
            else if (condition != null && !condition.trim().isEmpty()) {
                
                logger.debug("Filtering by condition only");
                return patientService.filterByCondition(patientId, condition.trim());
            }
            // Case 3: Only doctor name provided
            else if (doctorName != null && !doctorName.trim().isEmpty()) {
                
                logger.debug("Filtering by doctor name only");
                return patientService.filterByDoctor(patientId, doctorName.trim());
            }
            // Case 4: No filters - return all appointments
            else {
                logger.debug("No filters provided - returning all appointments");
                return patientService.getPatientAppointment(patientId);
            }
            
        } catch (Exception e) {
            logger.error("Error filtering patient appointments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error filtering appointments: " + e.getMessage()));
        }
    }
    
    /**
     * Helper method to create standardized error response
     *
     * @param errorMessage - Error message to include
     * @return Map containing error details
     */
    private Map<String, String> createErrorResponse(String errorMessage) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", errorMessage);
        errorResponse.put("timestamp", java.time.LocalDateTime.now().toString());
        return errorResponse;
    }
    
    /**
     * Additional helper method: Check if user is admin
     *
     * @param token - JWT token
     * @return true if user is admin, false otherwise
     */
    public boolean isAdmin(String token) {
        try {
            String username = tokenService.extractUsername(token);
            return adminRepository.findByUsername(username).isPresent();
        } catch (Exception e) {
            logger.error("Error checking admin status: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Additional helper method: Get all available specialties
     * Useful for frontend dropdown lists
     *
     * @return ResponseEntity with list of specialties
     */
    public ResponseEntity<?> getAllSpecialties() {
        try {
            logger.info("Retrieving all available specialties");
            
            List<String> specialties = doctorRepository.findAllDistinctSpecialties();
            
            logger.info("Found {} distinct specialties", specialties.size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("specialties", specialties);
            response.put("count", specialties.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving specialties: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error retrieving specialties: " + e.getMessage()));
        }
    }
}
