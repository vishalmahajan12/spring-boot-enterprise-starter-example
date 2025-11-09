package com.yourcompany.starter.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yourcompany.starter.interceptor.OutgoingRequestLoggingInterceptor;

/**
 * Auto-configuration for Enterprise Starter.
 * 
 * This class automatically configures all enterprise features when the starter is on the classpath.
 * Features are enabled/disabled via configuration properties with prefix "enterprise.starter".
 * 
 * Features configured:
 * 1. Request/Response logging filters
 * 2. Authentication filter
 * 3. Swagger/OpenAPI configuration
 * 4. Monitoring configuration
 * 5. RestTemplate with logging interceptor
 * 6. WebClient with logging support
 */
@AutoConfiguration
@EnableConfigurationProperties(EnterpriseStarterProperties.class)
public class EnterpriseStarterAutoConfiguration {

    /**
     * Creates RestTemplateBuilder if not already provided by Spring Boot.
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }

    /**
     * Configures RestTemplate with outgoing request logging interceptor.
     * Only configured when logging is enabled and RestTemplateBuilder is available.
     */
    @Bean
    @ConditionalOnProperty(prefix = "enterprise.starter.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnBean(RestTemplateBuilder.class)
    public RestTemplate restTemplate(RestTemplateBuilder builder, 
                                     EnterpriseStarterProperties properties,
                                     ObjectMapper objectMapper) {
        RestTemplate restTemplate = builder
                .requestFactory(() -> new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
                .build();
        
        // Add logging interceptor
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
        interceptors.add(new OutgoingRequestLoggingInterceptor(properties, objectMapper));
        restTemplate.setInterceptors(interceptors);
        
        return restTemplate;
    }

    /**
     * Configures WebClient builder with correlation ID propagation.
     * Applications can use this bean to create WebClient instances with automatic correlation ID propagation.
     */
    @Bean
    @ConditionalOnProperty(prefix = "enterprise.starter.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Ensures ObjectMapper bean is available for JSON processing.
     * Registers JavaTimeModule to handle Java 8 date/time types (LocalDateTime, etc.).
     */
    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}

