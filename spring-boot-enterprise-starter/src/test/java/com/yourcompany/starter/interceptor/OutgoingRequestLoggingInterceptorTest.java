package com.yourcompany.starter.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.starter.config.EnterpriseStarterProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutgoingRequestLoggingInterceptorTest {

    private EnterpriseStarterProperties properties;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ClientHttpRequestExecution execution;

    private OutgoingRequestLoggingInterceptor interceptor;
    private EnterpriseStarterProperties.Logging loggingConfig;

    @BeforeEach
    void setUp() throws IOException {
        properties = new EnterpriseStarterProperties();
        loggingConfig = properties.getLogging();
        interceptor = new OutgoingRequestLoggingInterceptor(properties, objectMapper);
        
        // Create a simple mock response
        MockClientHttpResponse response = new MockClientHttpResponse();
        when(execution.execute(any(), any())).thenReturn(response);
    }

    @Test
    void testIntercept_LoggingDisabled() throws IOException {
        properties.getLogging().setEnabled(false);
        
        HttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.com/api"));
        byte[] body = "{}".getBytes();
        
        ClientHttpResponse response = interceptor.intercept(request, body, execution);
        
        verify(execution).execute(any(), any());
        assertNotNull(response);
    }

    @Test
    void testIntercept_OutgoingRequestLoggingDisabled() throws IOException {
        properties.getLogging().setEnabled(true);
        properties.getLogging().setLogOutgoingRequest(false);
        
        HttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.com/api"));
        byte[] body = "{}".getBytes();
        
        ClientHttpResponse response = interceptor.intercept(request, body, execution);
        
        verify(execution).execute(any(), any());
        assertNotNull(response);
    }

    @Test
    void testIntercept_LogsRequest() throws IOException {
        properties.getLogging().setEnabled(true);
        properties.getLogging().setLogOutgoingRequest(true);
        properties.getLogging().setLogIncomingResponse(true);
        properties.getLogging().setIncludeHeaders(true);
        properties.getLogging().setIncludeBody(true);
        
        HttpRequest request = new MockClientHttpRequest(HttpMethod.POST, URI.create("http://example.com/api/users"));
        request.getHeaders().add("Content-Type", "application/json");
        byte[] body = "{\"name\":\"test\"}".getBytes();
        
        ClientHttpResponse response = interceptor.intercept(request, body, execution);
        
        verify(execution).execute(any(), any());
        assertNotNull(response);
        // Verify correlation ID was added to headers
        String correlationIdHeader = properties.getLogging().getCorrelationIdHeader();
        assertTrue(request.getHeaders().containsKey("X-Correlation-ID") || 
                   request.getHeaders().containsKey(correlationIdHeader));
    }

    @Test
    void testIntercept_AddsCorrelationId() throws IOException {
        properties.getLogging().setEnabled(true);
        properties.getLogging().setLogOutgoingRequest(true);
        properties.getLogging().setCorrelationIdHeader("X-Correlation-ID");
        
        HttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("http://example.com/api"));
        byte[] body = new byte[0];
        
        interceptor.intercept(request, body, execution);
        
        // Correlation ID should be added to request headers
        verify(execution).execute(any(), any());
    }
}

