package com.yourcompany.sample.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourcompany.starter.model.AuthContext;

import jakarta.servlet.http.HttpServletRequest;
/**
 * Admin endpoints - requires ADMIN role.
 * Demonstrates operation-level authorization.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(HttpServletRequest request) {
        AuthContext authContext = (AuthContext) request.getAttribute("authContext");
        
        if (authContext == null || !authContext.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<String> roles = authContext.getRoles();
        if (roles == null || !roles.contains("ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden - ADMIN role required"));
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 100);
        stats.put("activeSessions", 50);
        stats.put("requestsToday", 1000);
        stats.put("averageResponseTime", 150);

        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        AuthContext authContext = (AuthContext) request.getAttribute("authContext");
        
        if (authContext == null || !authContext.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<String> roles = authContext.getRoles();
        if (roles == null || !roles.contains("ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden - ADMIN role required"));
        }

        return ResponseEntity.ok(Map.of("message", "User " + id + " deleted successfully"));
    }
}

