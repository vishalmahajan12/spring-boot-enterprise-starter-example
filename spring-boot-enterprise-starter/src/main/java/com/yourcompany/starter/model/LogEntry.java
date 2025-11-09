package com.yourcompany.starter.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Model class representing a log entry for requests/responses.
 * 
 * This class captures all the details needed for logging:
 * - Correlation ID for tracing requests across services
 * - Service name and consumer details
 * - Request/Response details (method, URI, headers, body)
 * - Performance metrics (duration)
 * - Error information if applicable
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogEntry {
    private String correlationId;
    private String serviceName;
    private String direction; // INCOMING, OUTGOING
    private String type; // REQUEST, RESPONSE
    private String method;
    private String uri;
    private String remoteAddress;
    private Map<String, String> headers;
    private Object body;
    private Integer statusCode;
    private Long durationMs;
    private String consumerDetails;
    private LocalDateTime timestamp;
    private String errorMessage;
    private String errorStack;

    // Constructors
    public LogEntry() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    public String getRemoteAddress() { return remoteAddress; }
    public void setRemoteAddress(String remoteAddress) { this.remoteAddress = remoteAddress; }
    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }
    public Object getBody() { return body; }
    public void setBody(Object body) { this.body = body; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getConsumerDetails() { return consumerDetails; }
    public void setConsumerDetails(String consumerDetails) { this.consumerDetails = consumerDetails; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public String getErrorStack() { return errorStack; }
    public void setErrorStack(String errorStack) { this.errorStack = errorStack; }
}

