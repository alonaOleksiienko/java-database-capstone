package com.project.back_end.mvc;

import com.project.back_end.services.Service;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Controller
public class DashboardController {

    private final Service service;

    // Constructor injection for the shared Service component
    public DashboardController(Service service) {
        this.service = service;
    }

    /**
     * Admin Dashboard page (Thymeleaf view)
     * Example: GET /adminDashboard/{token}
     * Validates the admin token and forwards to "admin/adminDashboard" if valid,
     * otherwise redirects to root ("/").
     */
    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable String token) {
        Map<String, Object> result = service.validateToken("admin", token);
        if (result != null && result.containsKey("error")) {
            return "redirect:/";
        }
        return "admin/adminDashboard";
    }

    /**
     * Doctor Dashboard page (Thymeleaf view)
     * Example: GET /doctorDashboard/{token}
     * Validates the doctor token and forwards to "doctor/doctorDashboard" if valid,
     * otherwise redirects to root ("/").
     */
    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable String token) {
        Map<String, Object> result = service.validateToken("doctor", token);
        if (result != null && result.containsKey("error")) {
            return "redirect:/";
        }
        return "doctor/doctorDashboard";
    }
}

