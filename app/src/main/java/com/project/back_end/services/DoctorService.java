package com.project.back_end.services;

import com.project.back_end.models.Doctor;
import com.project.back_end.models.Appointment;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.AppointmentRepository;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic for doctors: availability, CRUD, filtering, and login validation.
 *
 * NOTE on @Service vs your own "Service" helper:
 * If you also have a class literally named `Service` elsewhere, we use the fully-qualified
 * Spring annotation here to avoid any name collision.
 */
@org.springframework.stereotype.Service
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    private static final DateTimeFormatter SLOT_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public DoctorService(DoctorRepository doctorRepository,
                         AppointmentRepository appointmentRepository,
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // ---------------------------------------------------------------------
    // 1) AVAILABILITY
    // ---------------------------------------------------------------------
    /**
     * Returns available time-slot strings (e.g., "09:00-10:00") for a doctor on a given date,
     * removing slots that are already booked.
     */
    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        if (doctorId == null || date == null) return Collections.emptyList();

        Doctor doctor = doctorRepository.findById(doctorId).orElse(null);
        if (doctor == null) return Collections.emptyList();

        // Ensure availableTimes is initialized (ElementCollection can be LAZY).
        List<String> allSlots = doctor.getAvailableTimes() == null
                ? Collections.emptyList()
                : new ArrayList<>(doctor.getAvailableTimes());

        if (allSlots.isEmpty()) return allSlots;

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        // Fetch booked appts today and convert to "HH:mm-HH:mm" labels
        List<Appointment> booked = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);
        Set<String> bookedLabels = booked.stream()
                .map(ap -> slotLabel(ap.getAppointmentTime(), ap.getAppointmentTime().plusHours(1)))
                .collect(Collectors.toSet());

        // Remove booked slots (string match)
        return allSlots.stream()
                .filter(s -> !bookedLabels.contains(normalizeSlot(s)))
                .sorted(Comparator.comparing(DoctorService::slotStartAsLocalTime))
                .collect(Collectors.toList());
    }

    private static String slotLabel(LocalDateTime start, LocalDateTime end) {
        return start.toLocalTime().format(SLOT_FMT) + "-" + end.toLocalTime().format(SLOT_FMT);
    }

    private static String normalizeSlot(String raw) {
        // Ensures slot string looks like "HH:mm-HH:mm"
        String s = raw == null ? "" : raw.trim();
        String[] parts = s.split("-");
        if (parts.length != 2) return s;
        return toHHmm(parts[0]) + "-" + toHHmm(parts[1]);
    }

    private static String toHHmm(String t) {
        String s = t.trim();
        // accept "9:00" or "09:00"
        if (s.length() == 4) s = "0" + s;
        return s;
    }

    private static LocalTime slotStartAsLocalTime(String slot) {
        try {
            String start = normalizeSlot(slot).split("-")[0];
            return LocalTime.parse(start, SLOT_FMT);
        } catch (Exception e) {
            return LocalTime.MIDNIGHT;
        }
    }

    // ---------------------------------------------------------------------
    // 2) SAVE
    // ---------------------------------------------------------------------
    /**
     * Saves a new doctor if email is unique.
     * @return 1 = success, -1 = conflict (email exists), 0 = error
     */
    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctor == null || doctor.getEmail() == null) return 0;
            Doctor existing = doctorRepository.findByEmail(doctor.getEmail());
            if (existing != null) return -1;
            doctorRepository.save(doctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ---------------------------------------------------------------------
    // 3) UPDATE
    // ---------------------------------------------------------------------
    /**
     * Updates an existing doctor.
     * @return 1 = updated, -1 = not found, 0 = error
     */
    @Transactional
    public int updateDoctor(Doctor incoming) {
        if (incoming == null || incoming.getId() == null) return 0;

        Optional<Doctor> dbOpt = doctorRepository.findById(incoming.getId());
        if (dbOpt.isEmpty()) return -1;

        try {
            Doctor db = dbOpt.get();
            // Apply fields if provided (simple overwrite policy; adjust as needed)
            if (incoming.getName() != null) db.setName(incoming.getName());
            if (incoming.getSpecialty() != null) db.setSpecialty(incoming.getSpecialty());
            if (incoming.getEmail() != null) db.setEmail(incoming.getEmail());
            if (incoming.getPassword() != null) db.setPassword(incoming.getPassword());
            if (incoming.getPhone() != null) db.setPhone(incoming.getPhone());
            if (incoming.getAvailableTimes() != null) {
                db.setAvailableTimes(new ArrayList<>(incoming.getAvailableTimes()));
            }
            doctorRepository.save(db);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ---------------------------------------------------------------------
    // 4) GET ALL
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        List<Doctor> list = doctorRepository.findAll();
        // Touch availableTimes for LAZY init safety
        list.forEach(d -> {
            if (d.getAvailableTimes() != null) d.getAvailableTimes().size();
        });
        return list;
    }

    // ---------------------------------------------------------------------
    // 5) DELETE
    // ---------------------------------------------------------------------
    /**
     * Deletes a doctor and all appointments linked to that doctor.
     * @return 1 = deleted, -1 = not found, 0 = error
     */
    @Transactional
    public int deleteDoctor(Long doctorId) {
        if (doctorId == null) return 0;

        Optional<Doctor> dbOpt = doctorRepository.findById(doctorId);
        if (dbOpt.isEmpty()) return -1;

        try {
            appointmentRepository.deleteAllByDoctorId(doctorId);
            doctorRepository.deleteById(doctorId);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // ---------------------------------------------------------------------
    // 6) LOGIN VALIDATION
    // ---------------------------------------------------------------------
    /**
     * Validates doctor login and returns token on success.
     * Response map keys: success (boolean), message (String), token (String), doctorId (Long)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validateDoctor(String email, String password) {
        Map<String, Object> resp = new HashMap<>();
        if (email == null || password == null) {
            resp.put("success", false);
            resp.put("message", "Email and password are required.");
            return resp;
        }

        Doctor doc = doctorRepository.findByEmail(email);
        if (doc == null) {
            resp.put("success", false);
            resp.put("message", "Invalid credentials.");
            return resp;
        }

        // Plain-text compare here; replace with hashing in production
        if (!password.equals(doc.getPassword())) {
            resp.put("success", false);
            resp.put("message", "Invalid credentials.");
            return resp;
        }

        String token = tokenService.generateToken("doctor", doc.getId());
        resp.put("success", true);
        resp.put("message", "Login successful.");
        resp.put("token", token);
        resp.put("doctorId", doc.getId());
        return resp;
    }

    // ---------------------------------------------------------------------
    // 7) FIND BY NAME (partial)
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Doctor> findDoctorByName(String name) {
        String q = (name == null) ? "" : name.trim();
        if (q.isEmpty()) return getDoctors();
        // Uses repository method defined earlier
        List<Doctor> list = doctorRepository.findByNameLike("%" + q + "%");
        list.forEach(d -> { if (d.getAvailableTimes() != null) d.getAvailableTimes().size(); });
        return list;
    }

    // ---------------------------------------------------------------------
    // 8) FILTER: name + specialty + time (AM/PM)
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorsByNameSpecilityandTime(String name, String specialty, String timePeriod) {
        String nm = name == null ? "" : name.trim();
        String sp = specialty == null ? "" : specialty.trim();

        List<Doctor> base = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(nm, sp);

        return base.stream()
                .filter(d -> matchesPeriod(d.getAvailableTimes(), timePeriod))
                .peek(d -> { if (d.getAvailableTimes() != null) d.getAvailableTimes().size(); })
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    // 9) FILTER: by time over a provided set of doctors
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorByTime(List<Doctor> doctors, String timePeriod) {
        if (doctors == null) return Collections.emptyList();
        return doctors.stream()
                .filter(d -> matchesPeriod(d.getAvailableTimes(), timePeriod))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    // 10) FILTER: name + time
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorByNameAndTime(String name, String timePeriod) {
        List<Doctor> byName = findDoctorByName(name);
        return byName.stream()
                .filter(d -> matchesPeriod(d.getAvailableTimes(), timePeriod))
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    // 11) FILTER: name + specialty
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorByNameAndSpecility(String name, String specialty) {
        String nm = name == null ? "" : name.trim();
        String sp = specialty == null ? "" : specialty.trim();
        List<Doctor> list = doctorRepository
                .findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(nm, sp);
        list.forEach(d -> { if (d.getAvailableTimes() != null) d.getAvailableTimes().size(); });
        return list;
    }

    // ---------------------------------------------------------------------
    // 12) FILTER: time + specialty
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorByTimeAndSpecility(String specialty, String timePeriod) {
        String sp = specialty == null ? "" : specialty.trim();
        List<Doctor> list = doctorRepository.findBySpecialtyIgnoreCase(sp);
        return list.stream()
                .filter(d -> matchesPeriod(d.getAvailableTimes(), timePeriod))
                .peek(d -> { if (d.getAvailableTimes() != null) d.getAvailableTimes().size(); })
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------------
    // 13) FILTER: by time across all doctors
    // ---------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorsByTime(String timePeriod) {
        List<Doctor> all = getDoctors();
        return all.stream()
                .filter(d -> matchesPeriod(d.getAvailableTimes(), timePeriod))
                .collect(Collectors.toList());
    }

    // ======================= Helpers =======================

    private static boolean matchesPeriod(List<String> slots, String timePeriod) {
        if (slots == null || slots.isEmpty() || timePeriod == null) return false;

        String p = timePeriod.trim().toUpperCase(Locale.ROOT);
        boolean wantAM = "AM".equals(p);
        boolean wantPM = "PM".equals(p);
        if (!wantAM && !wantPM) return false;

        for (String s : slots) {
            LocalTime start = slotStartAsLocalTime(s);
            if (wantAM && start.isBefore(LocalTime.NOON)) return true;
            if (wantPM && (start.equals(LocalTime.NOON) || start.isAfter(LocalTime.NOON))) return true;
        }
        return false;
    }
}

