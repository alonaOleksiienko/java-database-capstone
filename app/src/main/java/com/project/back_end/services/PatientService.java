package com.project.back_end.services;

import com.project.back_end.models.Patient;
import com.project.back_end.models.Appointment;
import com.project.back_end.repositories.PatientRepository;
import com.project.back_end.repositories.AppointmentRepository;
import com.project.back_end.dto.AppointmentDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Service class for handling Patient-related business logic
 * Manages patient creation, appointment retrieval, and filtering operations
 */
@Service
public class PatientService {
    
    // Logger for tracking operations and errors
    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);
    
    // Dependencies injected via constructor
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;
    
    /**
     * Constructor Injection for Dependencies
     * Best practice for dependency injection and testing
     *
     * @param patientRepository - Repository for patient data access
     * @param appointmentRepository - Repository for appointment data access
     * @param tokenService - Service for token extraction and validation
     */
    public PatientService(PatientRepository patientRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }
    
    /**
     * Creates a new patient in the database
     *
     * @param patient - Patient object to be created
     * @return 1 if patient created successfully, 0 if failed
     */
    public int createPatient(Patient patient) {
        try {
            logger.info("Creating new patient with email: {}", patient.getEmail());
            
            // Validate patient data before saving
            if (patient == null || patient.getEmail() == null || patient.getEmail().isEmpty()) {
                logger.error("Invalid patient data: patient or email is null/empty");
                return 0;
            }
            
            // Check if patient already exists
            Optional<Patient> existingPatient = patientRepository.findByEmail(patient.getEmail());
            if (existingPatient.isPresent()) {
                logger.warn("Patient with email {} already exists", patient.getEmail());
                return 0;
            }
            
            // Save patient to database
            Patient savedPatient = patientRepository.save(patient);
            
            if (savedPatient != null && savedPatient.getId() != null) {
                logger.info("Patient created successfully with ID: {}", savedPatient.getId());
                return 1;
            } else {
                logger.error("Failed to create patient - saved patient is null or has no ID");
                return 0;
            }
            
        } catch (Exception e) {
            logger.error("Error occurred while creating patient: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Retrieves all appointments for a specific patient
     * Converts appointments to DTOs for API response
     *
     * @param patientId - ID of the patient
     * @return ResponseEntity with list of AppointmentDTOs or error status
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPatientAppointment(Long patientId) {
        try {
            logger.info("Retrieving appointments for patient ID: {}", patientId);
            
            // Validate patient ID
            if (patientId == null || patientId <= 0) {
                logger.error("Invalid patient ID: {}", patientId);
                return ResponseEntity.badRequest()
                    .body("Invalid patient ID");
            }
            
            // Check if patient exists
            Optional<Patient> patient = patientRepository.findById(patientId);
            if (!patient.isPresent()) {
                logger.warn("Patient with ID {} not found", patientId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Patient not found");
            }
            
            // Retrieve appointments for the patient
            List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
            
            // Convert appointments to DTOs
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            logger.info("Retrieved {} appointments for patient ID: {}", appointmentDTOs.size(), patientId);
            return ResponseEntity.ok(appointmentDTOs);
            
        } catch (Exception e) {
            logger.error("Error retrieving appointments for patient ID {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving appointments: " + e.getMessage());
        }
    }
    
    /**
     * Filters appointments by condition (past or future)
     *
     * @param patientId - ID of the patient
     * @param condition - "past" or "future"
     * @return ResponseEntity with filtered appointments or error status
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> filterByCondition(Long patientId, String condition) {
        try {
            logger.info("Filtering appointments for patient ID: {} by condition: {}", patientId, condition);
            
            // Validate inputs
            if (patientId == null || patientId <= 0) {
                logger.error("Invalid patient ID: {}", patientId);
                return ResponseEntity.badRequest()
                    .body("Invalid patient ID");
            }
            
            if (condition == null || condition.isEmpty()) {
                logger.error("Condition parameter is null or empty");
                return ResponseEntity.badRequest()
                    .body("Condition parameter is required");
            }
            
            // Determine status based on condition
            Integer status;
            if ("past".equalsIgnoreCase(condition)) {
                status = 1; // Past appointments
            } else if ("future".equalsIgnoreCase(condition)) {
                status = 0; // Future appointments
            } else {
                logger.error("Invalid condition value: {}", condition);
                return ResponseEntity.badRequest()
                    .body("Invalid condition. Use 'past' or 'future'");
            }
            
            // Retrieve filtered appointments
            List<Appointment> appointments = appointmentRepository
                .findByPatientIdAndStatus(patientId, status);
            
            // Convert to DTOs
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            logger.info("Found {} {} appointments for patient ID: {}",
                appointmentDTOs.size(), condition, patientId);
            return ResponseEntity.ok(appointmentDTOs);
            
        } catch (Exception e) {
            logger.error("Error filtering appointments by condition for patient ID {}: {}",
                patientId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error filtering appointments: " + e.getMessage());
        }
    }
    
    /**
     * Filters appointments by doctor's name
     *
     * @param patientId - ID of the patient
     * @param doctorName - Name of the doctor
     * @return ResponseEntity with filtered appointments or error status
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> filterByDoctor(Long patientId, String doctorName) {
        try {
            logger.info("Filtering appointments for patient ID: {} by doctor: {}", patientId, doctorName);
            
            // Validate inputs
            if (patientId == null || patientId <= 0) {
                logger.error("Invalid patient ID: {}", patientId);
                return ResponseEntity.badRequest()
                    .body("Invalid patient ID");
            }
            
            if (doctorName == null || doctorName.trim().isEmpty()) {
                logger.error("Doctor name is null or empty");
                return ResponseEntity.badRequest()
                    .body("Doctor name is required");
            }
            
            // Retrieve appointments filtered by patient ID and doctor name
            List<Appointment> appointments = appointmentRepository
                .findByPatientIdAndDoctorName(patientId, doctorName.trim());
            
            // Convert to DTOs
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            logger.info("Found {} appointments for patient ID: {} with doctor: {}",
                appointmentDTOs.size(), patientId, doctorName);
            return ResponseEntity.ok(appointmentDTOs);
            
        } catch (Exception e) {
            logger.error("Error filtering appointments by doctor for patient ID {}: {}",
                patientId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error filtering appointments by doctor: " + e.getMessage());
        }
    }
    
    /**
     * Filters appointments by both doctor name and condition (past/future)
     *
     * @param patientId - ID of the patient
     * @param doctorName - Name of the doctor
     * @param condition - "past" or "future"
     * @return ResponseEntity with filtered appointments or error status
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> filterByDoctorAndCondition(Long patientId, String doctorName, String condition) {
        try {
            logger.info("Filtering appointments for patient ID: {} by doctor: {} and condition: {}",
                patientId, doctorName, condition);
            
            // Validate inputs
            if (patientId == null || patientId <= 0) {
                logger.error("Invalid patient ID: {}", patientId);
                return ResponseEntity.badRequest()
                    .body("Invalid patient ID");
            }
            
            if (doctorName == null || doctorName.trim().isEmpty()) {
                logger.error("Doctor name is null or empty");
                return ResponseEntity.badRequest()
                    .body("Doctor name is required");
            }
            
            if (condition == null || condition.isEmpty()) {
                logger.error("Condition parameter is null or empty");
                return ResponseEntity.badRequest()
                    .body("Condition parameter is required");
            }
            
            // Determine status based on condition
            Integer status;
            if ("past".equalsIgnoreCase(condition)) {
                status = 1; // Past appointments
            } else if ("future".equalsIgnoreCase(condition)) {
                status = 0; // Future appointments
            } else {
                logger.error("Invalid condition value: {}", condition);
                return ResponseEntity.badRequest()
                    .body("Invalid condition. Use 'past' or 'future'");
            }
            
            // Retrieve appointments filtered by patient ID, doctor name, and status
            List<Appointment> appointments = appointmentRepository
                .findByPatientIdAndDoctorNameAndStatus(patientId, doctorName.trim(), status);
            
            // Convert to DTOs
            List<AppointmentDTO> appointmentDTOs = appointments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
            
            logger.info("Found {} {} appointments for patient ID: {} with doctor: {}",
                appointmentDTOs.size(), condition, patientId, doctorName);
            return ResponseEntity.ok(appointmentDTOs);
            
        } catch (Exception e) {
            logger.error("Error filtering appointments by doctor and condition for patient ID {}: {}",
                patientId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error filtering appointments: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves patient details based on authentication token
     * Extracts email from token and fetches patient information
     *
     * @param token - JWT token containing patient email
     * @return ResponseEntity with patient details or error status
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPatientDetails(String token) {
        try {
            logger.info("Retrieving patient details from token");
            
            // Validate token
            if (token == null || token.trim().isEmpty()) {
                logger.error("Token is null or empty");
                return ResponseEntity.badRequest()
                    .body("Authentication token is required");
            }
            
            // Extract email from token
            String email = tokenService.extractEmail(token);
            
            if (email == null || email.trim().isEmpty()) {
                logger.error("Failed to extract email from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired token");
            }
            
            logger.info("Extracted email from token: {}", email);
            
            // Fetch patient by email
            Optional<Patient> patientOptional = patientRepository.findByEmail(email);
            
            if (!patientOptional.isPresent()) {
                logger.warn("Patient with email {} not found", email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Patient not found");
            }
            
            Patient patient = patientOptional.get();
            
            // Remove sensitive information before returning
            patient.setPassword(null); // Don't expose password
            
            logger.info("Successfully retrieved patient details for email: {}", email);
            return ResponseEntity.ok(patient);
            
        } catch (Exception e) {
            logger.error("Error retrieving patient details: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving patient details: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to convert Appointment entity to AppointmentDTO
     * Ensures only necessary data is exposed to the client
     *
     * @param appointment - Appointment entity
     * @return AppointmentDTO with relevant appointment information
     */
    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());
        dto.setPatientId(appointment.getPatient().getId());
        dto.setPatientName(appointment.getPatient().getFirstName() + " " +
                          appointment.getPatient().getLastName());
        dto.setDoctorId(appointment.getDoctor().getId());
        dto.setDoctorName(appointment.getDoctor().getFirstName() + " " +
                         appointment.getDoctor().getLastName());
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setAppointmentTime(appointment.getAppointmentTime());
        dto.setStatus(appointment.getStatus());
        dto.setReason(appointment.getReason());
        dto.setNotes(appointment.getNotes());
        
        return dto;
    }
    
    /**
     * Additional helper method: Check if appointment is in the past
     * Can be used for business logic validation
     *
     * @param appointment - Appointment to check
     * @return true if appointment is in the past, false otherwise
     */
    private boolean isAppointmentPast(Appointment appointment) {
        LocalDateTime appointmentDateTime = LocalDateTime.of(
            appointment.getAppointmentDate(),
            appointment.getAppointmentTime()
        );
        return appointmentDateTime.isBefore(LocalDateTime.now());
    }
    
    /**
     * Additional helper method: Validate patient exists
     *
     * @param patientId - ID of patient to validate
     * @return true if patient exists, false otherwise
     */
    public boolean validatePatientExists(Long patientId) {
        try {
            return patientRepository.existsById(patientId);
        } catch (Exception e) {
            logger.error("Error validating patient existence: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Additional method: Get patient by ID
     *
     * @param patientId - ID of the patient
     * @return ResponseEntity with patient or error status
     */
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPatientById(Long patientId) {
        try {
            logger.info("Retrieving patient with ID: {}", patientId);
            
            if (patientId == null || patientId <= 0) {
                logger.error("Invalid patient ID: {}", patientId);
                return ResponseEntity.badRequest()
                    .body("Invalid patient ID");
            }
            
            Optional<Patient> patientOptional = patientRepository.findById(patientId);
            
            if (!patientOptional.isPresent()) {
                logger.warn("Patient with ID {} not found", patientId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Patient not found");
            }
            
            Patient patient = patientOptional.get();
            patient.setPassword(null); // Remove sensitive data
            
            logger.info("Successfully retrieved patient with ID: {}", patientId);
            return ResponseEntity.ok(patient);
            
        } catch (Exception e) {
            logger.error("Error retrieving patient by ID {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving patient: " + e.getMessage());
        }
    }
    
    /**
     * Additional method: Update patient information
     *
     * @param patientId - ID of patient to update
     * @param updatedPatient - Patient object with updated information
     * @return ResponseEntity with updated patient or error status
     */
    @Transactional
    public ResponseEntity<?> updatePatient(Long patientId, Patient updatedPatient) {
        try {
            logger.info("Updating patient with ID: {}", patientId);
            
            if (patientId == null || patientId <= 0) {
                logger.error("Invalid patient ID: {}", patientId);
                return ResponseEntity.badRequest()
                    .body("Invalid patient ID");
            }
            
            Optional<Patient> existingPatientOptional = patientRepository.findById(patientId);
            
            if (!existingPatientOptional.isPresent()) {
                logger.warn("Patient with ID {} not found", patientId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Patient not found");
            }
            
            Patient existingPatient = existingPatientOptional.get();
            
            // Update fields (preserve ID and email)
            if (updatedPatient.getFirstName() != null) {
                existingPatient.setFirstName(updatedPatient.getFirstName());
            }
            if (updatedPatient.getLastName() != null) {
                existingPatient.setLastName(updatedPatient.getLastName());
            }
            if (updatedPatient.getPhoneNumber() != null) {
                existingPatient.setPhoneNumber(updatedPatient.getPhoneNumber());
            }
            if (updatedPatient.getAddress() != null) {
                existingPatient.setAddress(updatedPatient.getAddress());
            }
            if (updatedPatient.getDateOfBirth() != null) {
                existingPatient.setDateOfBirth(updatedPatient.getDateOfBirth());
            }
            
            Patient savedPatient = patientRepository.save(existingPatient);
            savedPatient.setPassword(null); // Remove sensitive data
            
            logger.info("Successfully updated patient with ID: {}", patientId);
            return ResponseEntity.ok(savedPatient);
            
        } catch (Exception e) {
            logger.error("Error updating patient with ID {}: {}", patientId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating patient: " + e.getMessage());
        }
    }
}
