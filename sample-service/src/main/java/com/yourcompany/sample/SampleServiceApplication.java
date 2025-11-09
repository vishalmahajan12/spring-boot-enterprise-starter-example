package com.yourcompany.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Sample Spring Boot service demonstrating Enterprise Starter features.
 * 
 * Features demonstrated:
 * - Request/Response logging with correlation IDs
 * - Authentication (JWT, API Key, Basic Auth)
 * - Swagger/OpenAPI documentation
 * - Monitoring and metrics
 * - Resilience patterns (Circuit Breaker, Retry)
 * - Rate limiting
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.yourcompany.sample", "com.yourcompany.starter"})
public class SampleServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleServiceApplication.class, args);
    }
}

