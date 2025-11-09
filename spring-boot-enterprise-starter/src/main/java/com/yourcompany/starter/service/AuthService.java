package com.yourcompany.starter.service;

import com.yourcompany.starter.config.EnterpriseStarterProperties;
import com.yourcompany.starter.model.AuthContext;
import com.yourcompany.starter.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Service for authentication operations.
 * 
 * Handles:
 * 1. Token validation
 * 2. Role-based access control (RBAC)
 * 3. Operation rules validation
 * 4. IP whitelist checking
 * 5. Audit logging
 */
@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final EnterpriseStarterProperties properties;
    private final JwtUtil jwtUtil;
    private final WebClient webClient;

    public AuthService(EnterpriseStarterProperties properties, JwtUtil jwtUtil) {
        this.properties = properties;
        this.jwtUtil = jwtUtil;
        this.webClient = WebClient.builder().build();
    }

    /**
     * Validates authentication based on configured auth type.
     */
    public AuthContext validateAuthentication(String token, String ipAddress, String operationName) {
        if (!properties.getAuthentication().isEnabled()) {
            AuthContext context = new AuthContext();
            context.setAuthenticated(true); // If auth disabled, consider authenticated
            return context;
        }

        AuthContext context = new AuthContext();
        context.setIpAddress(ipAddress);
        context.setToken(token);

        try {
            switch (properties.getAuthentication().getType()) {
                case JWT:
                    return validateJwtToken(context, token, ipAddress, operationName);
                case API_KEY:
                    return validateApiKey(context, token, ipAddress, operationName);
                case BASIC:
                    return validateBasicAuth(context, token, ipAddress, operationName);
                case OAUTH2:
                    return validateOAuth2(context, token, ipAddress, operationName);
                default:
                    logger.warn("Unsupported auth type: {}", properties.getAuthentication().getType());
                    context.setAuthenticated(false);
                    return context;
            }
        } catch (Exception e) {
            logger.error("Authentication validation failed", e);
            context.setAuthenticated(false);
            if (properties.getAuthentication().isEnableAuditLogging()) {
                auditLog(false, ipAddress, operationName, "Authentication error: " + e.getMessage());
            }
            return context;
        }
    }

    /**
     * Validates JWT token.
     */
    private AuthContext validateJwtToken(AuthContext context, String token, String ipAddress, String operationName) {
        if (token == null || token.isEmpty()) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "Token missing");
            return context;
        }

        String secretKey = properties.getAuthentication().getSecretKey();
        if (secretKey == null || secretKey.isEmpty()) {
            logger.error("JWT secret key not configured");
            context.setAuthenticated(false);
            return context;
        }

        // Validate token
        if (!jwtUtil.validateToken(token, secretKey)) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "Invalid or expired token");
            return context;
        }

        // Extract user info
        context.setUsername(jwtUtil.extractUsername(token, secretKey));
        context.setRoles(jwtUtil.extractRoles(token, secretKey));

        // Check IP whitelist
        if (properties.getAuthentication().isEnableIpWhitelist() && 
            !isIpWhitelisted(ipAddress)) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "IP not whitelisted");
            return context;
        }

        // Check role-based access
        if (!properties.getAuthentication().getAllowedRoles().isEmpty() &&
            !hasRequiredRole(context.getRoles())) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "Insufficient roles");
            return context;
        }

        // Check operation rules
        if (!validateOperationRules(operationName, context.getRoles())) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "Operation not allowed");
            return context;
        }

        context.setAuthenticated(true);
        auditLog(true, ipAddress, operationName, "Authentication successful");
        return context;
    }

    /**
     * Validates API key.
     * Checks API key against configured keys map and assigns roles.
     */
    private AuthContext validateApiKey(AuthContext context, String apiKey, String ipAddress, String operationName) {
        if (apiKey == null || apiKey.isEmpty()) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "API key missing");
            return context;
        }

        Map<String, List<String>> apiKeys = properties.getAuthentication().getApiKeys();
        if (apiKeys == null || apiKeys.isEmpty()) {
            logger.warn("No API keys configured for validation");
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "No API keys configured");
            return context;
        }

        List<String> roles = apiKeys.get(apiKey);
        if (roles == null) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "Invalid API key");
            return context;
        }

        // Set roles from API key configuration
        context.setRoles(roles);
        context.setUsername("api-key-user"); // Generic username for API key auth

        // Check IP whitelist
        if (properties.getAuthentication().isEnableIpWhitelist() && 
            !isIpWhitelisted(ipAddress)) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "IP not whitelisted");
            return context;
        }

        // Check role-based access
        if (!properties.getAuthentication().getAllowedRoles().isEmpty() &&
            !hasRequiredRole(context.getRoles())) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "Insufficient roles");
            return context;
        }

        // Check operation rules
        if (!validateOperationRules(operationName, context.getRoles())) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "Operation not allowed");
            return context;
        }

        context.setAuthenticated(true);
        auditLog(true, ipAddress, operationName, "API key authentication successful");
        return context;
    }

    /**
     * Validates Basic Auth credentials.
     * Supports validation against configured users map or external file.
     */
    private AuthContext validateBasicAuth(AuthContext context, String credentials, String ipAddress, String operationName) {
        if (credentials == null || credentials.isEmpty()) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "Basic Auth credentials missing");
            return context;
        }

        try {
            // Decode Base64 credentials
            String decodedCredentials = new String(Base64.getDecoder().decode(credentials), StandardCharsets.UTF_8);
            String[] parts = decodedCredentials.split(":", 2);
            
            if (parts.length != 2) {
                context.setAuthenticated(false);
                auditLog(false, ipAddress, operationName, "Invalid Basic Auth format");
                return context;
            }

            String username = parts[0];
            String password = parts[1];

            // Validate against configured users map
            Map<String, String> users = properties.getAuthentication().getBasicAuthUsers();
            if (users == null || users.isEmpty()) {
                logger.warn("No Basic Auth users configured");
                context.setAuthenticated(false);
                auditLog(false, ipAddress, operationName, "No Basic Auth users configured");
                return context;
            }

            String storedPassword = users.get(username);
            if (storedPassword == null || !storedPassword.equals(password)) {
                context.setAuthenticated(false);
                auditLog(false, ipAddress, operationName, "Invalid Basic Auth credentials");
                return context;
            }

            // Set username
            context.setUsername(username);
            context.setRoles(List.of("USER")); // Default role for Basic Auth

            // Check IP whitelist
            if (properties.getAuthentication().isEnableIpWhitelist() && 
                !isIpWhitelisted(ipAddress)) {
                context.setAuthenticated(false);
                auditLog(false, ipAddress, operationName, "IP not whitelisted");
                return context;
            }

            // Check role-based access
            if (!properties.getAuthentication().getAllowedRoles().isEmpty() &&
                !hasRequiredRole(context.getRoles())) {
                context.setAuthenticated(false);
                auditLog(false, ipAddress, operationName, "Insufficient roles");
                return context;
            }

            // Check operation rules
            if (!validateOperationRules(operationName, context.getRoles())) {
                context.setAuthenticated(false);
                auditLog(false, ipAddress, operationName, "Operation not allowed");
                return context;
            }

            context.setAuthenticated(true);
            auditLog(true, ipAddress, operationName, "Basic Auth authentication successful");
            return context;
        } catch (IllegalArgumentException e) {
            logger.error("Error decoding Basic Auth credentials", e);
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "Error decoding Basic Auth credentials");
            return context;
        }
    }

    /**
     * Validates OAuth2 token using token introspection endpoint.
     * Calls the OAuth2 authorization server's introspection endpoint to validate the token.
     */
    private AuthContext validateOAuth2(AuthContext context, String token, String ipAddress, String operationName) {
        if (token == null || token.isEmpty()) {
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "OAuth2 token missing");
            return context;
        }

        String introspectionUrl = properties.getAuthentication().getOauth2IntrospectionUrl();
        if (introspectionUrl == null || introspectionUrl.isEmpty()) {
            logger.error("OAuth2 introspection URL not configured");
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "OAuth2 introspection URL not configured");
            return context;
        }

        try {
            // Prepare introspection request
            String clientId = properties.getAuthentication().getOauth2ClientId();
            String clientSecret = properties.getAuthentication().getOauth2ClientSecret();
            
            // Create Basic Auth header for client credentials
            String clientCredentials = Base64.getEncoder()
                    .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            // Call introspection endpoint synchronously (in production, consider async)
            Map<String, Object> introspectionResponse = webClient.post()
                    .uri(introspectionUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + clientCredentials)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData("token", token))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(); // Blocking call - in production consider using reactive approach

            if (introspectionResponse == null) {
                context.setAuthenticated(false);
                auditLog(false, ipAddress, operationName, "OAuth2 introspection failed - no response");
                return context;
            }

            // Check if token is active
            Boolean active = (Boolean) introspectionResponse.get("active");
            if (active == null || !active) {
                context.setAuthenticated(false);
                auditLog(false, ipAddress, operationName, "OAuth2 token is not active");
                return context;
            }

            // Extract user information from introspection response
            String username = (String) introspectionResponse.get("username");
            if (username == null) {
                username = (String) introspectionResponse.get("sub"); // Fallback to subject
            }
            context.setUsername(username != null ? username : "oauth2-user");

            // Extract roles/scopes
            @SuppressWarnings("unchecked")
            List<String> scopes = (List<String>) introspectionResponse.get("scope");
            if (scopes == null) {
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) introspectionResponse.get("roles");
                context.setRoles(roles != null ? roles : List.of());
            } else {
                // Convert scopes to roles (you may need to adjust this based on your OAuth2 setup)
                context.setRoles(scopes);
            }

            // Check IP whitelist
            if (properties.getAuthentication().isEnableIpWhitelist() && 
                !isIpWhitelisted(ipAddress)) {
                context.setAuthenticated(false);
                auditLog(false, ipAddress, operationName, "IP not whitelisted");
                return context;
            }

            // Check role-based access
            if (!properties.getAuthentication().getAllowedRoles().isEmpty() &&
                !hasRequiredRole(context.getRoles())) {
                context.setAuthenticated(false);
                auditLog(false, ipAddress, operationName, "Insufficient roles");
                return context;
            }

            // Check operation rules
            if (!validateOperationRules(operationName, context.getRoles())) {
                context.setAuthenticated(false);
                auditLog(false, ipAddress, operationName, "Operation not allowed");
                return context;
            }

            context.setAuthenticated(true);
            auditLog(true, ipAddress, operationName, "OAuth2 authentication successful");
            return context;
        } catch (Exception e) {
            logger.error("OAuth2 token validation failed", e);
            context.setAuthenticated(false);
            auditLog(false, ipAddress, operationName, "OAuth2 validation error: " + e.getMessage());
            return context;
        }
    }

    /**
     * Checks if IP is whitelisted.
     */
    private boolean isIpWhitelisted(String ipAddress) {
        return properties.getAuthentication().getIpWhitelist().contains(ipAddress);
    }

    /**
     * Checks if user has required role.
     */
    private boolean hasRequiredRole(List<String> userRoles) {
        List<String> allowedRoles = properties.getAuthentication().getAllowedRoles();
        if (allowedRoles.isEmpty()) {
            return true; // No role restriction
        }
        return userRoles.stream().anyMatch(allowedRoles::contains);
    }

    /**
     * Validates operation rules.
     * Operation rules are configured as: operationName -> [allowed roles]
     */
    private boolean validateOperationRules(String operationName, List<String> userRoles) {
        if (operationName == null || operationName.isEmpty()) {
            return true; // No operation name, allow
        }

        var operationRules = properties.getAuthentication().getOperationRules();
        if (operationRules.isEmpty()) {
            return true; // No rules configured, allow
        }

        List<String> allowedRoles = operationRules.get(operationName);
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            return true; // No rules for this operation, allow
        }

        // Check if user has any of the allowed roles
        return userRoles.stream().anyMatch(allowedRoles::contains);
    }

    /**
     * Logs authentication attempts for audit purposes.
     */
    private void auditLog(boolean success, String ipAddress, String operationName, String details) {
        if (!properties.getAuthentication().isEnableAuditLogging()) {
            return;
        }

        String status = success ? "SUCCESS" : "FAILURE";
        logger.info("AUTH_AUDIT: status={}, ip={}, operation={}, details={}", 
                status, ipAddress, operationName, details);
    }
}

