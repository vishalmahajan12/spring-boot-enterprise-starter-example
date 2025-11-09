package com.yourcompany.sample.controller;

import com.yourcompany.starter.model.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * User endpoints - requires authentication.
 * Demonstrates authentication context access and RBAC.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final Map<Long, User> users = new HashMap<>();

    public UserController() {
        // Initialize with sample data
        users.put(1L, new User(1L, "john.doe", "John Doe", "USER"));
        users.put(2L, new User(2L, "jane.smith", "Jane Smith", "USER"));
        users.put(3L, new User(3L, "admin", "Admin User", "ADMIN"));
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(HttpServletRequest request) {
        AuthContext authContext = (AuthContext) request.getAttribute("authContext");
        
        if (authContext == null || !authContext.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String username = authContext.getUsername();
        List<String> roles = authContext.getRoles() != null ? authContext.getRoles() : List.of();
        
        User user = users.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);

        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("name", user.getName());
        profile.put("roles", roles);
        profile.put("authenticated", true);

        return ResponseEntity.ok(profile);
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers(HttpServletRequest request) {
        AuthContext authContext = (AuthContext) request.getAttribute("authContext");
        
        if (authContext == null || !authContext.isAuthenticated()) {
            return ResponseEntity.status(401).body(List.of());
        }

        return ResponseEntity.ok(new ArrayList<>(users.values()));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id, HttpServletRequest request) {
        AuthContext authContext = (AuthContext) request.getAttribute("authContext");
        
        if (authContext == null || !authContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        User user = users.get(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(user);
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        AuthContext authContext = (AuthContext) httpRequest.getAttribute("authContext");
        
        if (authContext == null || !authContext.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        Long newId = users.keySet().stream().max(Long::compareTo).orElse(0L) + 1;
        User newUser = new User(newId, request.username, request.name, "USER");
        users.put(newId, newUser);

        return ResponseEntity.ok(newUser);
    }

    // Inner classes
    static class User {
        private Long id;
        private String username;
        private String name;
        private String role;

        public User(Long id, String username, String name, String role) {
            this.id = id;
            this.username = username;
            this.name = name;
            this.role = role;
        }

        // Getters
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getName() { return name; }
        public String getRole() { return role; }
    }

    static class CreateUserRequest {
        public String username;
        public String name;
    }
}

