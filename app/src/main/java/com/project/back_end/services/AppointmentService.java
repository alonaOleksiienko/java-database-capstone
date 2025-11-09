package com.project.back_end.services;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Handles appointment business logic: booking, updating, canceling, retrieving, and status changes.
 *
 * NOTE on @Service vs your own Service:
 * Your codebase also has a class named `Service` used for shared token/validation helpers.
 * To avoid name collisions with Spring's @Service annotation, this class uses the fully qualified
 * annotation name: @org.springframework.stereotype.Service.
 */
@org.springframework.stereotype.Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final com.project.back_end.services.Service sharedService; // your shared helper "Service"
    private final TokenService tokenService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              com.project.back_end.services.Service sharedService,
                              TokenService tokenService,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.sharedService = sharedService;
        this.tokenService = tokenService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    // -------------------------------------------------------------------------
    // BOOK
    // -------------------------------------------------------------------------
    /**
     * Books a new appointment.
     * @param appt Appointment to save (must include doctor, patient, appointmentTime)
     * @return 1 if success, 0 if failure (validation or persistence error)
     */
    @Transactional
    public int bookAppointment(Appointment appt) {
        try {
            // Basic null/field checks
            if (appt == null || appt.getDoctor() == null || appt.getPatient() == null || appt.getAppointmentTime() == null) {
                return 0;
            }

            // Ensure doctor & patient actually exist
            Optional<Doctor> doctorOpt = doctorRepository.findById(
                    appt.getDoctor().getId() == null ? -1L : appt.getDoctor().getId());
            Optional<Patient> patientOpt = patientRepository.findById(
                    appt.getPatient().getId() == null ? -1L : appt.getPatient().getId());

            if (doctorOpt.isEmpty() || patientOpt.isEmpty()) {
                return 0;
            }

            // Validate appointment is in the future
            if (!appt.getAppointmentTime().isAfter(LocalDateTime.now())) {
                return 0;
            }

            // Check slot overlap for this doctor (1-hour slot assumed)
            LocalDateTime start = appt.getAppointmentTime();
            LocalDateTime end = start.plusHours(1);

            List<Appointment> overlaps = appointmentRepository
                    .findByDoctorIdAndAppointmentTimeBetween(doctorOpt.get().getId(), start, end.minusNanos(1));
            if (!overlaps.isEmpty()) {
                return 0; // slot taken
            }

            // Persist
            appointmentRepository.save(appt);
            return 1;
        } catch (Exception ex) {
            // log if you have logger
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------
    /**
     * Updates an existing appointment. Validates ownership by patient and slot availability.
     * @param incoming updated appointment (must contain id, patient, doctor, appointmentTime)
     * @param expectedPatientId patient that attempts update
     * @return map with result message
     */
    @Transactional
    public Map<String, Object> updateAppointment(Appointment incoming, Long expectedPatientId) {
        Map<String, Object> resp = new HashMap<>();

        if (incoming == null || incoming.getId() == null) {
            resp.put("status", "error");
            resp.put("message", "Invalid appointment payload.");
            return resp;
        }

        Optional<Appointment> existingOpt = appointmentRepository.findById(incoming.getId());
        if (existingOpt.isEmpty()) {
            resp.put("status", "error");
            resp.put("message", "Appointment not found.");
            return resp;
        }

        Appointment existing = existingOpt.get();

        // Ensure patient owns the appointment
        if (existing.getPatient() == null ||
            existing.getPatient().getId() == null ||
            !Objects.equals(existing.getPatient().getId(), expectedPatientId)) {
            resp.put("status", "error");
            resp.put("message", "Unauthorized: appointment does not belong to the patient.");
            return resp;
        }

        // Validate new doctor/patient records exist
        Long doctorId = (incoming.getDoctor() != null) ? incoming.getDoctor().getId() : null;
        Long patientId = (incoming.getPatient() != null) ? incoming.getPatient().getId() : null;
        if (doctorId == null || patientId == null) {
            resp.put("status", "error");
            resp.put("message", "Doctor or Patient information missing.");
            return resp;
        }

        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        Optional<Patient> patientOpt = patientRepository.findById(patientId);
        if (doctorOpt.isEmpty() || patientOpt.isEmpty()) {
            resp.put("status", "error");
            resp.put("message", "Doctor or Patient not found.");
            return resp;
        }

        // Validate time
        if (incoming.getAppointmentTime() == null || !incoming.getAppointmentTime().isAfter(LocalDateTime.now())) {
            resp.put("status", "error");
            resp.put("message", "Appointment time must be in the future.");
            return resp;
        }

        // Check slot clash excluding this appointment id
        LocalDateTime newStart = incoming.getAppointmentTime();
        LocalDateTime newEnd = newStart.plusHours(1);
        List<Appointment> sameSlot = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, newStart, newEnd.minusNanos(1));
        boolean takenByAnother = sameSlot.stream().anyMatch(a -> !a.getId().equals(existing.getId()));
        if (takenByAnother) {
            resp.put("status", "error");
            resp.put("message", "Requested time slot is not available.");
            return resp;
        }

        // Apply changes
        existing.setDoctor(doctorOpt.get());
        existing.setPatient(patientOpt.get());
        existing.setAppointmentTime(incoming.getAppointmentTime());
        existing.setStatus(incoming.getStatus());

        appointmentRepository.save(existing);

        resp.put("status", "success");
        resp.put("message", "Appointment updated.");
        resp.put("appointmentId", existing.getId());
        return resp;
    }

    // -------------------------------------------------------------------------
    // CANCEL
    // -------------------------------------------------------------------------
    /**
     * Cancels an appointment if it belongs to the given patient.
     * @param apptId appointment id
     * @param expectedPatientId patient performing cancellation
     * @return map result
     */
    @Transactional
    public Map<String, Object> cancelAppointment(Long apptId, Long expectedPatientId) {
        Map<String, Object> resp = new HashMap<>();

        if (apptId == null) {
            resp.put("status", "error");
            resp.put("message", "Appointment id required.");
            return resp;
        }

        Optional<Appointment> apptOpt = appointmentRepository.findById(apptId);
        if (apptOpt.isEmpty()) {
            resp.put("status", "error");
            resp.put("message", "Appointment not found.");
            return resp;
        }

        Appointment appt = apptOpt.get();
        if (appt.getPatient() == null || appt.getPatient().getId() == null ||
            !Objects.equals(appt.getPatient().getId(), expectedPatientId)) {
            resp.put("status", "error");
            resp.put("message", "Unauthorized: appointment does not belong to the patient.");
            return resp;
        }

        try {
            appointmentRepository.delete(appt);
            resp.put("status", "success");
            resp.put("message", "Appointment canceled.");
        } catch (Exception ex) {
            resp.put("status", "error");
            resp.put("message", "Failed to cancel appointment.");
        }

        return resp;
    }

    // -------------------------------------------------------------------------
    // GET (for doctor/day with optional patient name filter)
    // -------------------------------------------------------------------------
    /**
     * Returns appointments for a doctor on a given date. If patientName is provided, filters by it (case-insensitive).
     * @param doctorId doctor id
     * @param day LocalDate day
     * @param patientName optional patient name (trimmed). If null/blank, ignored.
     */
    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsForDoctorByDay(Long doctorId, LocalDate day, String patientName) {
        if (doctorId == null || day == null) return Collections.emptyList();

        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.atTime(LocalTime.MAX);

        if (patientName != null && !patientName.trim().isEmpty()) {
            return appointmentRepository.findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
                    doctorId, patientName.trim(), start, end);
        }

        return appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end);
    }

    // -------------------------------------------------------------------------
    // STATUS
    // -------------------------------------------------------------------------
    /**
     * Changes status (e.g., 0->scheduled, 1->completed, etc.).
     */
    @Transactional
    public void changeStatus(long appointmentId, int status) {
        appointmentRepository.updateStatus(status, appointmentId);
    }

    // -------------------------------------------------------------------------
    // OPTIONAL helpers you might call from controllers
    // -------------------------------------------------------------------------
    public boolean isPatientTokenValid(String token) {
        Map<String, Object> res = sharedService.validateToken(token, "patient");
        return !"true".equals(res.get("isError")); // adapt to your sharedService contract
    }

    public boolean isDoctorTokenValid(String token) {
        Map<String, Object> res = sharedService.validateToken(token, "doctor");
        return !"true".equals(res.get("isError"));
    }
}

