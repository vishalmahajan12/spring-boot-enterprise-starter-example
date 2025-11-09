package com.yourcompany.starter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for Swagger/OpenAPI documentation.
 * 
 * This configuration:
 * 1. Sets up OpenAPI/Swagger UI
 * 2. Configures API information (title, description, version, contact)
 * 3. Adds security schemes (JWT, API Key, etc.)
 * 4. Configures API path groups
 * 5. Excludes paths from documentation
 * 
 * Only enabled when enterprise.starter.swagger.enabled=true
 */
@Configuration
@ConditionalOnProperty(prefix = "enterprise.starter.swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SwaggerConfig {
    private final EnterpriseStarterProperties properties;

    public SwaggerConfig(EnterpriseStarterProperties properties) {
        this.properties = properties;
    }

    /**
     * Configures OpenAPI/Swagger main configuration.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title(properties.getSwagger().getTitle())
                        .description(properties.getSwagger().getDescription())
                        .version(properties.getSwagger().getVersion())
                        .contact(createContact()));

        // Add security schemes if enabled
        if (properties.getSwagger().isEnableSecuritySchemes() && 
            properties.getAuthentication().isEnabled()) {
            openAPI.components(new Components()
                    .addSecuritySchemes("bearer-jwt", createJwtSecurityScheme())
                    .addSecuritySchemes("api-key", createApiKeySecurityScheme()));
            
            // Apply security requirement globally
            openAPI.addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
        }

        return openAPI;
    }

    /**
     * Configures API groups for different path patterns.
     */
    @Bean
    public GroupedOpenApi publicApi() {
        List<String> pathsToExclude = new ArrayList<>();
        pathsToExclude.add("/actuator/**");
        pathsToExclude.add("/error");
        pathsToExclude.addAll(properties.getSwagger().getExcludePaths());

        return GroupedOpenApi.builder()
                .group("public-api")
                .pathsToMatch(properties.getSwagger().getApiPath())
                .pathsToExclude(pathsToExclude.toArray(new String[0]))
                .build();
    }

    /**
     * Creates contact information for API documentation.
     */
    private Contact createContact() {
        Contact contact = new Contact();
        if (properties.getSwagger().getContactName() != null) {
            contact.setName(properties.getSwagger().getContactName());
        }
        if (properties.getSwagger().getContactEmail() != null) {
            contact.setEmail(properties.getSwagger().getContactEmail());
        }
        if (properties.getSwagger().getContactUrl() != null) {
            contact.setUrl(properties.getSwagger().getContactUrl());
        }
        return contact;
    }

    /**
     * Creates JWT security scheme definition.
     */
    private SecurityScheme createJwtSecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT Authentication");
    }

    /**
     * Creates API Key security scheme definition.
     */
    private SecurityScheme createApiKeySecurityScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name("X-API-Key")
                .description("API Key Authentication");
    }
}

