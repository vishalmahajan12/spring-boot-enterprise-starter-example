package com.yourcompany.starter.filter;

import com.yourcompany.starter.config.EnterpriseStarterProperties;
import com.yourcompany.starter.model.AuthContext;
import com.yourcompany.starter.service.AuthService;
import com.yourcompany.starter.service.RateLimitingService;
import jakarta.servlet.FilterChain;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter for handling authentication.
 * 
 * This filter:
 * 1. Checks if authentication is enabled
 * 2. Extracts token from Authorization header
 * 3. Validates token using AuthService
 * 4. Checks IP whitelist if enabled
 * 5. Validates operation rules
 * 6. Sets AuthContext in request attribute for use in controllers
 * 7. Returns 401 Unauthorized if authentication fails
 * 
 * Runs after RequestLoggingFilter (@Order(2)).
 */
@Component
@Order(2)
public class AuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    private final EnterpriseStarterProperties properties;
    private final AuthService authService;
    private RateLimitingService rateLimitingService;

    public AuthenticationFilter(EnterpriseStarterProperties properties, 
                              AuthService authService) {
        this.properties = properties;
        this.authService = authService;
    }

    @Autowired(required = false)
    public void setRateLimitingService(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        // Skip if authentication disabled or path excluded
        if (!properties.getAuthentication().isEnabled() || 
            isExcludedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token/credentials based on auth type
        String token = extractTokenOrCredentials(request);
        String ipAddress = getClientIpAddress(request);
        String operationName = extractOperationName(request);

        // Check rate limiting FIRST (using IP address) to prevent brute force attacks
        // This applies to all requests before authentication validation
        if (properties.getAuthentication().isEnableRateLimiting() && rateLimitingService != null) {
            if (!rateLimitingService.isAllowed(ipAddress)) {
                response.setStatus(429); // HTTP 429 Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}");
                return;
            }
        }

        // Validate authentication after rate limiting check
        AuthContext authContext = authService.validateAuthentication(token, ipAddress, operationName);

        // Additional rate limiting check based on authenticated user (if authenticated)
        // This allows different rate limits for authenticated vs unauthenticated users
        if (authContext != null && authContext.isAuthenticated() && 
            properties.getAuthentication().isEnableRateLimiting() && rateLimitingService != null) {
            // Use user ID if available, otherwise username, otherwise IP
            String userRateLimitKey = authContext.getUserId() != null 
                    ? authContext.getUserId() 
                    : (authContext.getUsername() != null ? authContext.getUsername() : ipAddress);
            // Optionally apply stricter rate limiting for authenticated users
            // Currently using same limit, but could be configured differently
            if (!rateLimitingService.isAllowed(userRateLimitKey)) {
                response.setStatus(429); // HTTP 429 Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}");
                return;
            }
        }

        // Check authentication result
        if (authContext == null || !authContext.isAuthenticated()) {
            // Authentication failed
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Authentication failed\"}");
            return;
        }

        // Set auth context in request attribute for use in controllers
        request.setAttribute("authContext", authContext);

        // Add security headers if enabled
        if (properties.getAuthentication().isEnableSecurityHeaders()) {
            addSecurityHeaders(response);
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts token or credentials based on authentication type.
     * Supports:
     * - JWT/OAuth2: Authorization header with "Bearer <token>"
     * - API Key: X-API-Key header (or configured header)
     * - Basic Auth: Authorization header with "Basic <credentials>"
     */
    private String extractTokenOrCredentials(HttpServletRequest request) {
        EnterpriseStarterProperties.Authentication authConfig = properties.getAuthentication();
        
        switch (authConfig.getType()) {
            case API_KEY:
                String apiKeyHeader = authConfig.getApiKeyHeader();
                String apiKey = request.getHeader(apiKeyHeader);
                return apiKey != null ? apiKey : request.getHeader("X-API-Key");
                
            case BASIC:
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Basic ")) {
                    return authHeader.substring(6); // Return Base64 encoded credentials
                }
                return null;
                
            case JWT:
            case OAUTH2:
            default:
                // Extract from Authorization header
                authHeader = request.getHeader("Authorization");
                if (authHeader == null || authHeader.isEmpty()) {
                    return null;
                }
                // Remove "Bearer " or "Token " prefix if present
                if (authHeader.startsWith("Bearer ")) {
                    return authHeader.substring(7);
                } else if (authHeader.startsWith("Token ")) {
                    return authHeader.substring(6);
                }
                return authHeader;
        }
    }

    /**
     * Extracts operation name from request.
     * Can be from header, path, or request attribute.
     */
    private String extractOperationName(HttpServletRequest request) {
        // Check X-Operation-Name header
        String operationName = request.getHeader("X-Operation-Name");
        if (operationName != null && !operationName.isEmpty()) {
            return operationName;
        }

        // Could also extract from path or other sources
        // For now, return path as operation name
        return request.getRequestURI();
    }

    /**
     * Gets client IP address, handling proxies.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Checks if path should be excluded from authentication.
     */
    private boolean isExcludedPath(String path) {
        return properties.getAuthentication().getExcludedPaths().stream()
                .anyMatch(excluded -> path.matches(excluded.replace("**", ".*")));
    }

    /**
     * Adds security headers to response.
     */
    private void addSecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        response.setHeader("Content-Security-Policy", "default-src 'self'");
    }
}

