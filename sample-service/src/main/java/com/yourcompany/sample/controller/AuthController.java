package com.yourcompany.sample.controller;

import com.yourcompany.starter.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Authentication endpoints for testing.
 * Generates JWT tokens for testing authentication.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Value("${enterprise.starter.authentication.secret-key}")
    private String secretKey;

    @Value("${enterprise.starter.authentication.token-validity-seconds:3600}")
    private long tokenValiditySeconds;

    /**
     * Generate JWT token for testing.
     * Use this token in Authorization header: Bearer <token>
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        // Validate credentials (simplified for demo)
        if (!isValidCredentials(request.username, request.password)) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "Invalid credentials",
                "hint", "Try: admin/admin123 or user/user123"
            ));
        }

        // Determine roles based on username
        List<String> roles = request.username.equals("admin") 
            ? List.of("ADMIN", "USER") 
            : List.of("USER");

        // Generate JWT token
        String token = jwtUtil.generateToken(request.username, roles, secretKey, tokenValiditySeconds);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("username", request.username);
        response.put("roles", roles);
        response.put("expiresIn", tokenValiditySeconds);
        response.put("message", "Use this token in Authorization header: Bearer <token>");

        return ResponseEntity.ok(response);
    }

    /**
     * Test endpoint to verify authentication is working.
     */
    @GetMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify() {
        return ResponseEntity.ok(Map.of(
            "message", "Authentication is required for this endpoint",
            "note", "Try accessing without token, then with token"
        ));
    }

    private boolean isValidCredentials(String username, String password) {
        // Simplified validation for demo
        return (username.equals("admin") && password.equals("admin123")) ||
               (username.equals("user") && password.equals("user123"));
    }

    static class LoginRequest {
        public String username;
        public String password;
    }
}

