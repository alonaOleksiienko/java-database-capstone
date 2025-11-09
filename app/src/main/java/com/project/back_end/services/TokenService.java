package com.project.back_end.services;

import com.project.back_end.repositories.AdminRepository;
import com.project.back_end.repositories.DoctorRepository;
import com.project.back_end.repositories.PatientRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public TokenService(
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository
    ) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    /** Build the HMAC-SHA key from the configured secret. */
    private SecretKey getSigningKey() {
        // Secret should be at least 256 bits (32+ chars) for HS256
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /** Generate a JWT for the given email, valid for 7 days. */
    public String generateToken(String email) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 7L * 24 * 60 * 60 * 1000); // 7 days

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /** Extract the email (subject) from a JWT after verifying its signature. */
    public String extractEmail(String token) {
        token = sanitizeToken(token);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * Validate that the token is valid for a specific role by confirming
     * the email from the token exists in the corresponding repository.
     *
     * @param token the JWT (with or without "Bearer " prefix)
     * @param role  one of: "admin", "doctor", "patient"
     * @return true if valid and user exists for the role; otherwise false
     */
    public boolean validateToken(String token, String role) {
        try {
            String email = extractEmail(token);
            if (email == null || email.isBlank()) return false;

            switch (role == null ? "" : role.trim().toLowerCase()) {
                case "admin":
                    return adminRepository.existsByEmail(email);
                case "doctor":
                    return doctorRepository.existsByEmail(email);
                case "patient":
                    return patientRepository.existsByEmail(email);
                default:
                    return false;
            }
        } catch (Exception ex) {
            // Signature/format/expiration errors all result in invalid token
            return false;
        }
    }

    /** Optional convenience: accept Authorization headers with "Bearer " prefix. */
    private String sanitizeToken(String token) {
        if (token == null) return "";
        String trimmed = token.trim();
        if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return trimmed.substring(7).trim();
        }
        return trimmed;
    }
}

