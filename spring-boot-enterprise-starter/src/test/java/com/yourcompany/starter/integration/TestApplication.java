package com.yourcompany.starter.integration;

import com.yourcompany.starter.config.EnterpriseStarterProperties;
import com.yourcompany.starter.service.RateLimitingService;
import com.yourcompany.starter.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test Spring Boot application for integration tests.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.yourcompany.starter"})
public class TestApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    /**
     * Ensures JwtUtil is available as a bean for tests.
     */
    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }

    /**
     * Ensures RateLimitingService is available as a bean for tests.
     * Note: RateLimitingService is conditionally created, so we ensure it's always available in tests.
     */
    @Bean
    public RateLimitingService rateLimitingService(@Autowired EnterpriseStarterProperties properties) {
        return new RateLimitingService(properties);
    }

    @RestController
    @RequestMapping("/api")
    static class TestController {

        @GetMapping("/test")
        public String testGet() {
            return "OK";
        }

        @PostMapping("/test")
        public String testPost(@RequestBody String body) {
            return "OK";
        }
    }
}

