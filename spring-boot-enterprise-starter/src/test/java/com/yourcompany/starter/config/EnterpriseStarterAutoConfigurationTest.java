package com.yourcompany.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourcompany.starter.filter.RequestLoggingFilter;
import com.yourcompany.starter.filter.AuthenticationFilter;
import com.yourcompany.starter.interceptor.OutgoingRequestLoggingInterceptor;
import com.yourcompany.starter.service.AuthService;
import com.yourcompany.starter.service.MetricsService;
import com.yourcompany.starter.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for auto-configuration.
 */
class EnterpriseStarterAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EnterpriseStarterAutoConfiguration.class,
                    org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration.class
            ));

    @Test
    void testAutoConfiguration_WhenEnabled() {
        contextRunner
                .withPropertyValues(
                        "enterprise.starter.logging.enabled=true",
                        "enterprise.starter.authentication.enabled=false",
                        "enterprise.starter.swagger.enabled=true",
                        "enterprise.starter.monitoring.enabled=true"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EnterpriseStarterProperties.class);
                    assertThat(context).hasSingleBean(ObjectMapper.class);
                    assertThat(context).hasSingleBean(RestTemplate.class);
                    assertThat(context).hasSingleBean(WebClient.Builder.class);
                });
    }

    @Test
    void testAutoConfiguration_RestTemplateNotCreatedWhenLoggingDisabled() {
        contextRunner
                .withPropertyValues("enterprise.starter.logging.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(RestTemplate.class);
                });
    }

    @Test
    void testAutoConfiguration_BeansAreConditional() {
        contextRunner
                .withPropertyValues(
                        "enterprise.starter.logging.enabled=false",
                        "enterprise.starter.authentication.enabled=false",
                        "enterprise.starter.swagger.enabled=false",
                        "enterprise.starter.monitoring.enabled=false"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(EnterpriseStarterProperties.class);
                    assertThat(context).hasSingleBean(ObjectMapper.class);
                });
    }
}

