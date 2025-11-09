package com.yourcompany.starter.model;

/**
 * Model class representing authentication context.
 * 
 * Stores authentication information extracted from the request:
 * - User ID
 * - Roles
 * - Token information
 * - IP address
 * 
 * This context is stored in ThreadLocal and accessible throughout the request.
 */
public class AuthContext {
    private String userId;
    private String username;
    private java.util.List<String> roles;
    private String token;
    private String ipAddress;
    private boolean authenticated;

    // Constructors
    public AuthContext() {
        this.authenticated = false;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public java.util.List<String> getRoles() { return roles; }
    public void setRoles(java.util.List<String> roles) { this.roles = roles; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public boolean isAuthenticated() { return authenticated; }
    public void setAuthenticated(boolean authenticated) { this.authenticated = authenticated; }
}

