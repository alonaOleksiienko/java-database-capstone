package com.project.back_end.services;

import com.project.back_end.models.Prescription;
import com.project.back_end.repositories.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * Service class for handling Prescription-related business logic
 * Manages prescription creation and retrieval for appointments
 */
@Service
public class PrescriptionService {
    
    // Logger for tracking operations and errors
    private static final Logger logger = LoggerFactory.getLogger(PrescriptionService.class);
    
    // Dependency injected via constructor
    private final PrescriptionRepository prescriptionRepository;
    
    /**
     * Constructor Injection for Dependencies
     * Best practice for dependency injection and testing
     *
     * @param prescriptionRepository - Repository for prescription data access
     */
    public PrescriptionService(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }
    
    /**
     * Saves a new prescription to the database
     * Prevents duplicate prescriptions for the same appointment
     *
     * @param prescription - Prescription object to be saved
     * @return ResponseEntity with success message or error status
     */
    public ResponseEntity<?> savePrescription(Prescription prescription) {
        try {
            logger.info("Attempting to save prescription for appointment ID: {}",
                prescription.getAppointmentId());
            
            // Validate prescription object
            if (prescription == null) {
                logger.error("Prescription object is null");
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Prescription data is required"));
            }
            
            // Validate appointment ID
            if (prescription.getAppointmentId() == null) {
                logger.error("Appointment ID is null in prescription");
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Appointment ID is required"));
            }
            
            // Check if prescription already exists for this appointment
            Optional<Prescription> existingPrescription = prescriptionRepository
                .findByAppointmentId(prescription.getAppointmentId());
            
            if (existingPrescription.isPresent()) {
                logger.warn("Prescription already exists for appointment ID: {}",
                    prescription.getAppointmentId());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(
                        "Prescription already exists for this appointment"));
            }
            
            // Validate prescription details
            if (!validatePrescriptionDetails(prescription)) {
                logger.error("Invalid prescription details for appointment ID: {}",
                    prescription.getAppointmentId());
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid prescription details"));
            }
            
            // Save the new prescription
            Prescription savedPrescription = prescriptionRepository.save(prescription);
            
            logger.info("Successfully saved prescription with ID: {} for appointment ID: {}",
                savedPrescription.getId(), savedPrescription.getAppointmentId());
            
            // Return success response with 201 Created status
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Prescription saved successfully");
            response.put("prescriptionId", savedPrescription.getId());
            response.put("appointmentId", savedPrescription.getAppointmentId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument while saving prescription: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Invalid prescription data: " + e.getMessage()));
                
        } catch (Exception e) {
            logger.error("Error occurred while saving prescription for appointment ID {}: {}",
                prescription != null ? prescription.getAppointmentId() : "null",
                e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error saving prescription: " + e.getMessage()));
        }
    }
    
    /**
     * Retrieves a prescription associated with a specific appointment
     *
     * @param appointmentId - ID of the appointment
     * @return ResponseEntity with prescription data or error status
     */
    public ResponseEntity<?> getPrescription(Long appointmentId) {
        try {
            logger.info("Retrieving prescription for appointment ID: {}", appointmentId);
            
            // Validate appointment ID
            if (appointmentId == null || appointmentId <= 0) {
                logger.error("Invalid appointment ID: {}", appointmentId);
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid appointment ID"));
            }
            
            // Fetch prescription from repository
            Optional<Prescription> prescriptionOptional = prescriptionRepository
                .findByAppointmentId(appointmentId);
            
            // Check if prescription exists
            if (!prescriptionOptional.isPresent()) {
                logger.warn("No prescription found for appointment ID: {}", appointmentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("No prescription found for this appointment"));
            }
            
            Prescription prescription = prescriptionOptional.get();
            
            logger.info("Successfully retrieved prescription ID: {} for appointment ID: {}",
                prescription.getId(), appointmentId);
            
            // Return prescription wrapped in a map with 200 OK status
            Map<String, Object> response = new HashMap<>();
            response.put("prescription", prescription);
            response.put("appointmentId", appointmentId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error occurred while fetching prescription for appointment ID {}: {}",
                appointmentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error retrieving prescription: " + e.getMessage()));
        }
    }
    
    /**
     * Retrieves all prescriptions for a specific patient
     *
     * @param patientId - ID of the patient
     * @return ResponseEntity with list of prescriptions or error status
     */
    public ResponseEntity<?> getPatientPrescriptions(Long patientId) {
        try {
            logger.info("Retrieving all prescriptions for patient ID: {}", patientId);
            
            // Validate patient ID
            if (patientId == null || patientId <= 0) {
                logger.error("Invalid patient ID: {}", patientId);
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid patient ID"));
            }
            
            // Fetch all prescriptions for the patient
            List<Prescription> prescriptions = prescriptionRepository
                .findByPatientId(patientId);
            
            logger.info("Found {} prescriptions for patient ID: {}",
                prescriptions.size(), patientId);
            
            // Return prescriptions with metadata
            Map<String, Object> response = new HashMap<>();
            response.put("prescriptions", prescriptions);
            response.put("count", prescriptions.size());
            response.put("patientId", patientId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving prescriptions for patient ID {}: {}",
                patientId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error retrieving patient prescriptions: " + e.getMessage()));
        }
    }
    
    /**
     * Retrieves all prescriptions prescribed by a specific doctor
     *
     * @param doctorId - ID of the doctor
     * @return ResponseEntity with list of prescriptions or error status
     */
    public ResponseEntity<?> getDoctorPrescriptions(Long doctorId) {
        try {
            logger.info("Retrieving all prescriptions by doctor ID: {}", doctorId);
            
            // Validate doctor ID
            if (doctorId == null || doctorId <= 0) {
                logger.error("Invalid doctor ID: {}", doctorId);
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid doctor ID"));
            }
            
            // Fetch all prescriptions by the doctor
            List<Prescription> prescriptions = prescriptionRepository
                .findByDoctorId(doctorId);
            
            logger.info("Found {} prescriptions by doctor ID: {}",
                prescriptions.size(), doctorId);
            
            // Return prescriptions with metadata
            Map<String, Object> response = new HashMap<>();
            response.put("prescriptions", prescriptions);
            response.put("count", prescriptions.size());
            response.put("doctorId", doctorId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving prescriptions for doctor ID {}: {}",
                doctorId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error retrieving doctor prescriptions: " + e.getMessage()));
        }
    }
    
    /**
     * Updates an existing prescription
     *
     * @param prescriptionId - ID of the prescription to update
     * @param updatedPrescription - Prescription object with updated information
     * @return ResponseEntity with updated prescription or error status
     */
    public ResponseEntity<?> updatePrescription(Long prescriptionId, Prescription updatedPrescription) {
        try {
            logger.info("Updating prescription with ID: {}", prescriptionId);
            
            // Validate prescription ID
            if (prescriptionId == null || prescriptionId <= 0) {
                logger.error("Invalid prescription ID: {}", prescriptionId);
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid prescription ID"));
            }
            
            // Check if prescription exists
            Optional<Prescription> existingPrescriptionOptional = prescriptionRepository
                .findById(prescriptionId);
            
            if (!existingPrescriptionOptional.isPresent()) {
                logger.warn("Prescription with ID {} not found", prescriptionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Prescription not found"));
            }
            
            Prescription existingPrescription = existingPrescriptionOptional.get();
            
            // Update fields
            if (updatedPrescription.getMedicationName() != null) {
                existingPrescription.setMedicationName(updatedPrescription.getMedicationName());
            }
            if (updatedPrescription.getDosage() != null) {
                existingPrescription.setDosage(updatedPrescription.getDosage());
            }
            if (updatedPrescription.getFrequency() != null) {
                existingPrescription.setFrequency(updatedPrescription.getFrequency());
            }
            if (updatedPrescription.getDuration() != null) {
                existingPrescription.setDuration(updatedPrescription.getDuration());
            }
            if (updatedPrescription.getInstructions() != null) {
                existingPrescription.setInstructions(updatedPrescription.getInstructions());
            }
            
            // Validate updated prescription
            if (!validatePrescriptionDetails(existingPrescription)) {
                logger.error("Invalid updated prescription details for ID: {}", prescriptionId);
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid prescription details"));
            }
            
            // Save updated prescription
            Prescription savedPrescription = prescriptionRepository.save(existingPrescription);
            
            logger.info("Successfully updated prescription with ID: {}", prescriptionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Prescription updated successfully");
            response.put("prescription", savedPrescription);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error updating prescription with ID {}: {}",
                prescriptionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error updating prescription: " + e.getMessage()));
        }
    }
    
    /**
     * Deletes a prescription by ID
     *
     * @param prescriptionId - ID of the prescription to delete
     * @return ResponseEntity with success message or error status
     */
    public ResponseEntity<?> deletePrescription(Long prescriptionId) {
        try {
            logger.info("Deleting prescription with ID: {}", prescriptionId);
            
            // Validate prescription ID
            if (prescriptionId == null || prescriptionId <= 0) {
                logger.error("Invalid prescription ID: {}", prescriptionId);
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid prescription ID"));
            }
            
            // Check if prescription exists
            if (!prescriptionRepository.existsById(prescriptionId)) {
                logger.warn("Prescription with ID {} not found", prescriptionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Prescription not found"));
            }
            
            // Delete prescription
            prescriptionRepository.deleteById(prescriptionId);
            
            logger.info("Successfully deleted prescription with ID: {}", prescriptionId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Prescription deleted successfully");
            response.put("prescriptionId", prescriptionId.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error deleting prescription with ID {}: {}",
                prescriptionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error deleting prescription: " + e.getMessage()));
        }
    }
    
    /**
     * Validates prescription details before saving or updating
     *
     * @param prescription - Prescription object to validate
     * @return true if valid, false otherwise
     */
    private boolean validatePrescriptionDetails(Prescription prescription) {
        // Check medication name
        if (prescription.getMedicationName() == null ||
            prescription.getMedicationName().trim().isEmpty()) {
            logger.error("Medication name is null or empty");
            return false;
        }
        
        // Check dosage
        if (prescription.getDosage() == null ||
            prescription.getDosage().trim().isEmpty()) {
            logger.error("Dosage is null or empty");
            return false;
        }
        
        // Check frequency
        if (prescription.getFrequency() == null ||
            prescription.getFrequency().trim().isEmpty()) {
            logger.error("Frequency is null or empty");
            return false;
        }
        
        // Check duration
        if (prescription.getDuration() == null ||
            prescription.getDuration().trim().isEmpty()) {
            logger.error("Duration is null or empty");
            return false;
        }
        
        // All validations passed
        return true;
    }
    
    /**
     * Helper method to create error response map
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
     * Retrieves prescription by ID
     *
     * @param prescriptionId - ID of the prescription
     * @return ResponseEntity with prescription or error status
     */
    public ResponseEntity<?> getPrescriptionById(Long prescriptionId) {
        try {
            logger.info("Retrieving prescription with ID: {}", prescriptionId);
            
            // Validate prescription ID
            if (prescriptionId == null || prescriptionId <= 0) {
                logger.error("Invalid prescription ID: {}", prescriptionId);
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Invalid prescription ID"));
            }
            
            // Fetch prescription
            Optional<Prescription> prescriptionOptional = prescriptionRepository
                .findById(prescriptionId);
            
            if (!prescriptionOptional.isPresent()) {
                logger.warn("Prescription with ID {} not found", prescriptionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Prescription not found"));
            }
            
            Prescription prescription = prescriptionOptional.get();
            
            logger.info("Successfully retrieved prescription with ID: {}", prescriptionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("prescription", prescription);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving prescription with ID {}: {}",
                prescriptionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error retrieving prescription: " + e.getMessage()));
        }
    }
    
    /**
     * Checks if a prescription exists for a given appointment
     *
     * @param appointmentId - ID of the appointment
     * @return true if prescription exists, false otherwise
     */
    public boolean prescriptionExists(Long appointmentId) {
        try {
            return prescriptionRepository.findByAppointmentId(appointmentId).isPresent();
        } catch (Exception e) {
            logger.error("Error checking prescription existence for appointment ID {}: {}",
                appointmentId, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Retrieves all prescriptions (admin function)
     *
     * @return ResponseEntity with list of all prescriptions or error status
     */
    public ResponseEntity<?> getAllPrescriptions() {
        try {
            logger.info("Retrieving all prescriptions");
            
            List<Prescription> prescriptions = prescriptionRepository.findAll();
            
            logger.info("Found {} total prescriptions", prescriptions.size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("prescriptions", prescriptions);
            response.put("count", prescriptions.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error retrieving all prescriptions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error retrieving prescriptions: " + e.getMessage()));
        }
    }
}
