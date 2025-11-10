package com.yourcompany.starter.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.starter.config.EnterpriseStarterProperties;
import com.yourcompany.starter.model.LogEntry;
import com.yourcompany.starter.util.CorrelationIdUtil;
import com.yourcompany.starter.util.MaskingUtil;

import jakarta.annotation.PreDestroy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter for logging incoming HTTP requests and outgoing responses.
 * 
 * This filter:
 * 1. Intercepts all incoming requests (runs first with @Order(1))
 * 2. Wraps request/response to enable reading body multiple times
 * 3. Generates/retrieves correlation ID
 * 4. Logs request details (method, URI, headers, body)
 * 5. Logs response details (status, headers, body, duration)
 * 6. Handles masking of sensitive data
 * 7. Supports path exclusion
 * 
 * Uses ContentCachingRequestWrapper/ResponseWrapper to read body without consuming the stream.
 */
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final EnterpriseStarterProperties properties;
    private final ObjectMapper objectMapper;
    private final ExecutorService asyncLoggingExecutor;

    public RequestLoggingFilter(EnterpriseStarterProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        // Create executor service for async logging if enabled
        if (properties.getLogging().isAsyncLogging()) {
            this.asyncLoggingExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r, "async-logging-" + System.currentTimeMillis());
                    t.setDaemon(true);
                    return t;
                }
            );
        } else {
            this.asyncLoggingExecutor = null;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (asyncLoggingExecutor != null) {
            asyncLoggingExecutor.shutdown();
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        // Skip logging if disabled or path is excluded
        if (!properties.getLogging().isEnabled() || 
            isExcludedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get or generate correlation ID
        String correlationId = getOrGenerateCorrelationId(request);
        CorrelationIdUtil.setCorrelationId(correlationId);
        
        // Add to MDC for logging context
        MDC.put("correlationId", correlationId);
        MDC.put("serviceName", getServiceName());

        long startTime = System.currentTimeMillis();
        
        // Wrap request/response to enable multiple reads of body
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        // Add correlation ID to response header (set on both wrapped and original to ensure it's included)
        String correlationIdHeader = properties.getLogging().getCorrelationIdHeader();
        response.setHeader(correlationIdHeader, correlationId);
        wrappedResponse.setHeader(correlationIdHeader, correlationId);

        try {
            // Log incoming request before processing
            if (properties.getLogging().isLogIncomingRequest()) {
                logIncomingRequest(wrappedRequest, correlationId);
            }

            // Continue filter chain
            filterChain.doFilter(wrappedRequest, wrappedResponse);

            // Calculate duration
            long duration = System.currentTimeMillis() - startTime;
            
            // Log outgoing response after processing
            if (properties.getLogging().isLogOutgoingResponse()) {
                logOutgoingResponse(wrappedResponse, correlationId, duration);
            }
        } finally {
            // Copy response body and headers back to original response
            // Ensure correlation ID is still present after copy
            wrappedResponse.copyBodyToResponse();
            if (!response.containsHeader(correlationIdHeader)) {
                response.setHeader(correlationIdHeader, correlationId);
            }
            
            // Cleanup ThreadLocal and MDC
            CorrelationIdUtil.clear();
            MDC.clear();
        }
    }

    /**
     * Logs incoming request details.
     */
    private void logIncomingRequest(ContentCachingRequestWrapper request, String correlationId) {
        if (!properties.getLogging().isLogRequest()) return;

        LogEntry logEntry = new LogEntry();
        logEntry.setCorrelationId(correlationId);
        logEntry.setServiceName(getServiceName());
        logEntry.setDirection("INCOMING");
        logEntry.setType("REQUEST");
        logEntry.setMethod(request.getMethod());
        logEntry.setUri(request.getRequestURI());
        logEntry.setRemoteAddress(getClientIpAddress(request));

        // Add headers if configured
        if (properties.getLogging().isIncludeHeaders()) {
            Map<String, String> headers = getHeaders(request);
            if (properties.getLogging().isMaskSensitiveData()) {
                headers = MaskingUtil.maskHeaders(headers, properties.getLogging().getSensitiveFields());
            }
            logEntry.setHeaders(headers);
        }

        // Add body if configured
        if (properties.getLogging().isIncludeBody()) {
            String body = getRequestBody(request);
            if (properties.getLogging().isMaskSensitiveData()) {
                body = MaskingUtil.maskSensitiveData(body, properties.getLogging().getSensitiveFields());
            }
            logEntry.setBody(truncateBody(body));
        }

        // Add consumer details if configured
        if (properties.getLogging().isIncludeConsumerDetails()) {
            logEntry.setConsumerDetails(getConsumerDetails(request));
        }

        logEntry(logEntry);
    }

    /**
     * Logs outgoing response details.
     */
    private void logOutgoingResponse(ContentCachingResponseWrapper response, String correlationId, long duration) {
        if (!properties.getLogging().isLogResponse()) return;

        LogEntry logEntry = new LogEntry();
        logEntry.setCorrelationId(correlationId);
        logEntry.setServiceName(getServiceName());
        logEntry.setDirection("OUTGOING");
        logEntry.setType("RESPONSE");
        logEntry.setStatusCode(response.getStatus());
        logEntry.setDurationMs(duration);

        // Add headers if configured
        if (properties.getLogging().isIncludeHeaders()) {
            Map<String, String> headers = getResponseHeaders(response);
            if (properties.getLogging().isMaskSensitiveData()) {
                headers = MaskingUtil.maskHeaders(headers, properties.getLogging().getSensitiveFields());
            }
            logEntry.setHeaders(headers);
        }

        // Add body if configured
        if (properties.getLogging().isIncludeBody()) {
            String body = getResponseBody(response);
            if (properties.getLogging().isMaskSensitiveData()) {
                body = MaskingUtil.maskSensitiveData(body, properties.getLogging().getSensitiveFields());
            }
            logEntry.setBody(truncateBody(body));
        }

        logEntry(logEntry);
    }

    /**
     * Gets correlation ID from header or generates new one.
     */
    private String getOrGenerateCorrelationId(HttpServletRequest request) {
        String headerName = properties.getLogging().getCorrelationIdHeader();
        String correlationId = request.getHeader(headerName);
        return correlationId != null ? correlationId : CorrelationIdUtil.generateCorrelationId();
    }

    /**
     * Reads request body from cached wrapper.
     */
    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        return content.length > 0 ? new String(content, StandardCharsets.UTF_8) : null;
    }

    /**
     * Reads response body from cached wrapper.
     */
    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        return content.length > 0 ? new String(content, StandardCharsets.UTF_8) : null;
    }

    /**
     * Extracts all request headers.
     */
    private Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        return headers;
    }

    /**
     * Extracts all response headers.
     * Handles duplicate header names by combining values with comma separator.
     */
    private Map<String, String> getResponseHeaders(ContentCachingResponseWrapper response) {
        return response.getHeaderNames().stream()
                .collect(Collectors.toMap(
                    name -> name, 
                    name -> {
                        // Get all values for this header name
                        Collection<String> values = response.getHeaders(name);
                        // Combine multiple values with comma (HTTP standard)
                        return String.join(", ", values);
                    },
                    (existing, replacement) -> existing + ", " + replacement // Merge function for duplicates
                ));
    }

    /**
     * Gets client IP address, handling proxies (X-Forwarded-For).
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Builds consumer details string.
     */
    private String getConsumerDetails(HttpServletRequest request) {
        return String.format("IP: %s, User-Agent: %s", 
                getClientIpAddress(request), 
                request.getHeader("User-Agent"));
    }

    /**
     * Gets service name from system property or config.
     */
    private String getServiceName() {
        return properties.getLogging().isIncludeServiceName() 
                ? System.getProperty("spring.application.name", "unknown-service")
                : null;
    }

    /**
     * Truncates body if exceeds max length.
     */
    private String truncateBody(String body) {
        if (body == null) return null;
        int maxLength = properties.getLogging().getMaxBodyLength();
        return body.length() > maxLength ? body.substring(0, maxLength) + "...(truncated)" : body;
    }

    /**
     * Checks if path should be excluded from logging.
     */
    private boolean isExcludedPath(String path) {
        return properties.getLogging().getExcludedPaths().stream()
                .anyMatch(excluded -> path.matches(excluded.replace("**", ".*")));
    }

    /**
     * Logs the log entry as JSON.
     * Uses async logging if enabled, otherwise synchronous.
     */
    private void logEntry(LogEntry logEntry) {
        if (properties.getLogging().isAsyncLogging() && asyncLoggingExecutor != null) {
            // Capture MDC context for async logging
            Map<String, String> mdcContext = MDC.getCopyOfContextMap();
            
            asyncLoggingExecutor.submit(() -> {
                try {
                    // Restore MDC context in async thread
                    if (mdcContext != null) {
                        MDC.setContextMap(mdcContext);
                    }
                    
                    String logMessage = objectMapper.writeValueAsString(logEntry);
                    logger.info("Request/Response Log: {}", logMessage);
                } catch (Exception e) {
                    logger.error("Error logging request/response", e);
                } finally {
                    MDC.clear();
                }
            });
        } else {
            // Synchronous logging
            try {
                String logMessage = objectMapper.writeValueAsString(logEntry);
                logger.info("Request/Response Log: {}", logMessage);
            } catch (Exception e) {
                logger.error("Error logging request/response", e);
            }
        }
    }
}

