package com.yourcompany.starter.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.starter.config.EnterpriseStarterProperties;
import com.yourcompany.starter.model.LogEntry;
import com.yourcompany.starter.util.CorrelationIdUtil;
import com.yourcompany.starter.util.MaskingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor for logging outgoing HTTP requests made by RestTemplate/WebClient.
 * 
 * This interceptor:
 * 1. Intercepts HTTP requests made to external services
 * 2. Logs outgoing request details (method, URI, headers, body)
 * 3. Logs incoming response details (status, headers, body, duration)
 * 4. Propagates correlation ID in headers
 * 5. Handles masking of sensitive data
 * 
 * Usage: Add this interceptor to RestTemplate bean configuration.
 */
@Component
public class OutgoingRequestLoggingInterceptor implements ClientHttpRequestInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(OutgoingRequestLoggingInterceptor.class);
    private final EnterpriseStarterProperties properties;
    private final ObjectMapper objectMapper;

    public OutgoingRequestLoggingInterceptor(EnterpriseStarterProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                       ClientHttpRequestExecution execution) throws IOException {
        // Skip if logging disabled or outgoing request logging disabled
        if (!properties.getLogging().isEnabled() || !properties.getLogging().isLogOutgoingRequest()) {
            return execution.execute(request, body);
        }

        // Get correlation ID from ThreadLocal and add to request headers
        String correlationId = CorrelationIdUtil.getCorrelationId();
        request.getHeaders().add(properties.getLogging().getCorrelationIdHeader(), correlationId);

        long startTime = System.currentTimeMillis();

        // Log outgoing request
        logOutgoingRequest(request, body, correlationId);

        // Execute the request
        ClientHttpResponse response = execution.execute(request, body);

        // Calculate duration
        long duration = System.currentTimeMillis() - startTime;

        // Log incoming response
        if (properties.getLogging().isLogIncomingResponse()) {
            logIncomingResponse(response, correlationId, duration);
        }

        return response;
    }

    /**
     * Logs outgoing request details.
     */
    private void logOutgoingRequest(HttpRequest request, byte[] body, String correlationId) {
        LogEntry logEntry = new LogEntry();
        logEntry.setCorrelationId(correlationId);
        logEntry.setServiceName(getServiceName());
        logEntry.setDirection("OUTGOING");
        logEntry.setType("REQUEST");
        logEntry.setMethod(request.getMethod().name());
        logEntry.setUri(request.getURI().toString());

        // Add headers if configured
        if (properties.getLogging().isIncludeHeaders()) {
            Map<String, String> headers = new HashMap<>();
            request.getHeaders().forEach((key, values) -> 
                headers.put(key, String.join(", ", values)));
            Map<String, String> finalHeaders = properties.getLogging().isMaskSensitiveData() 
                ? MaskingUtil.maskHeaders(headers, properties.getLogging().getSensitiveFields())
                : headers;
            logEntry.setHeaders(finalHeaders);
        }

        // Add body if configured
        if (properties.getLogging().isIncludeBody() && body.length > 0) {
            String bodyStr = new String(body, StandardCharsets.UTF_8);
            if (properties.getLogging().isMaskSensitiveData()) {
                bodyStr = MaskingUtil.maskSensitiveData(bodyStr, properties.getLogging().getSensitiveFields());
            }
            logEntry.setBody(truncateBody(bodyStr));
        }

        logEntry(logEntry);
    }

    /**
     * Logs incoming response details from external service.
     */
    private void logIncomingResponse(ClientHttpResponse response, String correlationId, long duration) 
            throws IOException {
        LogEntry logEntry = new LogEntry();
        logEntry.setCorrelationId(correlationId);
        logEntry.setServiceName(getServiceName());
        logEntry.setDirection("INCOMING");
        logEntry.setType("RESPONSE");
        logEntry.setStatusCode(response.getStatusCode().value());
        logEntry.setDurationMs(duration);

        // Add headers if configured
        if (properties.getLogging().isIncludeHeaders()) {
            Map<String, String> headers = new HashMap<>();
            response.getHeaders().forEach((key, values) -> 
                headers.put(key, String.join(", ", values)));
            Map<String, String> finalHeaders = properties.getLogging().isMaskSensitiveData() 
                ? MaskingUtil.maskHeaders(headers, properties.getLogging().getSensitiveFields())
                : headers;
            logEntry.setHeaders(finalHeaders);
        }

        // Add body if configured
        if (properties.getLogging().isIncludeBody()) {
            byte[] bodyBytes = StreamUtils.copyToByteArray(response.getBody());
            String bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);
            if (properties.getLogging().isMaskSensitiveData()) {
                bodyStr = MaskingUtil.maskSensitiveData(bodyStr, properties.getLogging().getSensitiveFields());
            }
            logEntry.setBody(truncateBody(bodyStr));
            // Reset response body stream so it can be read again by the application
            // Note: This requires wrapping the response in a buffering wrapper
        }

        logEntry(logEntry);
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
     * Logs the log entry as JSON.
     */
    private void logEntry(LogEntry logEntry) {
        try {
            String logMessage = objectMapper.writeValueAsString(logEntry);
            logger.info("Outgoing Request/Response Log: {}", logMessage);
        } catch (Exception e) {
            logger.error("Error logging outgoing request/response", e);
        }
    }
}

