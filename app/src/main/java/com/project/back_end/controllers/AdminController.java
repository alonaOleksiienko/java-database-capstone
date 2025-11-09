package com.project.back_end.controllers;

import com.project.back_end.models.Admin;
import com.project.back_end.services.Service;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for handling Admin authentication and management.
 *
 * Base path: /api/admin (configurable via api.path property)
 *
 * Responsibilities:
 *  - Process admin login requests
 *  - Delegate authentication and token generation to the Service layer
 */
@RestController
@RequestMapping("${api.path}admin")
public class AdminController {

    private final Service service;

    /**
     * Constructor-based injection for the shared Service dependency.
     */
    @Autowired
    public AdminController(Service service) {
        this.service = service;
    }

    // ===============================================================
    // 1. Admin Login Endpoint
    // ===============================================================

    /**
     * POST /api/admin/login
     *
     * Handles admin login requests.
     * Validates credentials and returns authentication status and token.
     *
     * @param admin - contains username/email and password
     * @return ResponseEntity with authentication result (token or error)
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> adminLogin(@Valid @RequestBody Admin admin) {
        return service.validateAdmin(admin);
    }
}

