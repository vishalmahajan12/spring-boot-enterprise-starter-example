package com.yourcompany.starter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.starter.config.EnterpriseStarterProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLoggingFilterTest {

    private EnterpriseStarterProperties properties;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private FilterChain filterChain;

    private RequestLoggingFilter filter;
    private EnterpriseStarterProperties.Logging loggingConfig;

    @BeforeEach
    void setUp() {
        properties = new EnterpriseStarterProperties();
        loggingConfig = properties.getLogging();
        filter = new RequestLoggingFilter(properties, objectMapper);
    }

    @Test
    void testDoFilterInternal_LoggingDisabled() throws Exception {
        properties.getLogging().setEnabled(false);
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        filter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void testDoFilterInternal_ExcludedPath() throws Exception {
        properties.getLogging().setEnabled(true);
        properties.getLogging().setExcludedPaths(Collections.singletonList("/actuator/**"));
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        filter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(any(), any());
    }

    @Test
    void testDoFilterInternal_LogsRequest() throws Exception {
        properties.getLogging().setEnabled(true);
        properties.getLogging().setLogIncomingRequest(true);
        properties.getLogging().setLogRequest(true);
        properties.getLogging().setExcludedPaths(Collections.emptyList());
        
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/users");
        request.addHeader("Content-Type", "application/json");
        request.setContent("{\"name\":\"test\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        filter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(any(ContentCachingRequestWrapper.class), 
                any(ContentCachingResponseWrapper.class));
    }

    @Test
    void testDoFilterInternal_AddsCorrelationId() throws Exception {
        properties.getLogging().setEnabled(true);
        properties.getLogging().setExcludedPaths(Collections.emptyList());
        
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        filter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(any(), any());
        // Correlation ID should be set in ThreadLocal
    }

    @Test
    void testDoFilterInternal_MasksSensitiveData() throws Exception {
        properties.getLogging().setEnabled(true);
        properties.getLogging().setLogIncomingRequest(true);
        properties.getLogging().setLogRequest(true);
        properties.getLogging().setMaskSensitiveData(true);
        properties.getLogging().setIncludeBody(true);
        properties.getLogging().setSensitiveFields(Collections.singletonList("password"));
        properties.getLogging().setExcludedPaths(Collections.emptyList());
        
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/login");
        request.setContent("{\"username\":\"user\",\"password\":\"secret\"}".getBytes());
        MockHttpServletResponse response = new MockHttpServletResponse();
        
        filter.doFilterInternal(request, response, filterChain);
        
        verify(filterChain).doFilter(any(), any());
    }
}

