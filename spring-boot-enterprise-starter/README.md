# Spring Boot Enterprise Starter

A comprehensive Spring Boot starter library that provides enterprise-grade features out of the box, including request/response logging, authentication, API documentation, monitoring, and resilience patterns.

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Features in Detail](#features-in-detail)
- [Examples](#examples)
- [API Reference](#api-reference)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Features

### 🚀 Core Features

- **Request/Response Logging**
  - Incoming request logging with correlation IDs
  - Outgoing request logging for external service calls
  - Response logging with duration metrics
  - Sensitive data masking
  - Configurable log levels and exclusions

- **Authentication & Authorization**
  - Multiple authentication providers (JWT, OAuth2, API Key, Basic Auth)
  - Role-Based Access Control (RBAC)
  - Operation-level authorization rules
  - IP whitelisting
  - Rate limiting
  - Audit logging

- **API Documentation**
  - Automatic Swagger/OpenAPI documentation
  - Security scheme definitions
  - Customizable API groups and paths

- **Monitoring & Observability**
  - Micrometer metrics integration
  - Prometheus metrics export
  - Performance monitoring with slow query detection
  - Error tracking and aggregation
  - Custom health indicators

- **Resilience Patterns**
  - Circuit breaker (Resilience4j)
  - Retry mechanism with exponential backoff
  - Configurable timeouts

- **Cross-Cutting Concerns**
  - Global exception handling
  - Correlation ID propagation
  - Security headers
  - Request validation

## Requirements

- Java 17 or higher
- Spring Boot 3.2.0 or higher
- Maven 3.6+ or Gradle 7.0+

## Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.yourcompany</groupId>
    <artifactId>spring-boot-enterprise-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

Add to your `build.gradle`:

```gradle
dependencies {
    implementation 'com.yourcompany:spring-boot-enterprise-starter:1.0.0'
}
```

## Quick Start

### 1. Basic Configuration

Add minimal configuration to your `application.yml`:

```yaml
enterprise:
  starter:
    logging:
      enabled: true
    authentication:
      enabled: false  # Disable auth for quick start
    swagger:
      enabled: true
    monitoring:
      enabled: true
```

### 2. Enable Features

All features are enabled by default but can be toggled via configuration:

```yaml
enterprise:
  starter:
    logging:
      enabled: true
      mask-sensitive-data: true
    authentication:
      enabled: true
      type: JWT
      secret-key: "your-secret-key"
    swagger:
      enabled: true
      title: "My API"
    monitoring:
      enabled: true
      expose-prometheus: true
```

## Configuration

### Logging Configuration

```yaml
enterprise:
  starter:
    logging:
      enabled: true                    # Enable/disable logging
      level: INFO                      # Log level: DEBUG, INFO, WARN, ERROR
      log-request: true                # Log incoming requests
      log-response: true              # Log outgoing responses
      log-incoming-request: true      # Log requests received by service
      log-outgoing-request: true      # Log requests sent to external services
      log-incoming-response: true     # Log responses from external services
      log-outgoing-response: true     # Log responses sent to clients
      include-headers: true            # Include HTTP headers in logs
      include-body: true               # Include request/response body
      mask-sensitive-data: true       # Mask sensitive fields
      max-body-length: 10000           # Maximum body length to log
      excluded-paths:                  # Paths to exclude from logging
        - "/actuator/**"
        - "/health"
      sensitive-fields:                # Fields to mask
        - "password"
        - "token"
        - "secret"
        - "creditCard"
      correlation-id-header: "X-Correlation-ID"
      include-service-name: true
      include-consumer-details: true
```

### Authentication Configuration

#### JWT Authentication

```yaml
enterprise:
  starter:
    authentication:
      enabled: true
      type: JWT
      secret-key: "your-secret-key-min-256-bits"
      token-validity-seconds: 3600
      excluded-paths:
        - "/public/**"
      operation-rules:
        "/api/admin/**":
          - "ADMIN"
        "/api/user/**":
          - "USER"
          - "ADMIN"
      enable-rate-limiting: true
      rate-limit-per-minute: 100
      enable-audit-logging: true
      allowed-roles: []  # Empty = all authenticated users
      enable-ip-whitelist: false
      ip-whitelist:
        - "127.0.0.1"
      enable-security-headers: true
```

#### API Key Authentication

```yaml
enterprise:
  starter:
    authentication:
      enabled: true
      type: API_KEY
      api-keys:
        "your-api-key-1":
          - "USER"
          - "ADMIN"
        "your-api-key-2":
          - "USER"
      api-key-header: "X-API-Key"
```

#### Basic Auth

```yaml
enterprise:
  starter:
    authentication:
      enabled: true
      type: BASIC
      basic-auth-users:
        "admin": "admin123"  # Use hashed passwords in production
        "user": "user123"
```

#### OAuth2 Authentication

```yaml
enterprise:
  starter:
    authentication:
      enabled: true
      type: OAUTH2
      oauth2-introspection-url: "https://oauth2-server.com/introspect"
      oauth2-client-id: "your-client-id"
      oauth2-client-secret: "your-client-secret"
      oauth2-validate-token-endpoint: true
```

### Swagger Configuration

```yaml
enterprise:
  starter:
    swagger:
      enabled: true
      title: "My API Documentation"
      description: "API Documentation for My Application"
      version: "1.0.0"
      contact-name: "API Team"
      contact-email: "api@example.com"
      contact-url: "https://example.com"
      exclude-paths:
        - "/actuator/**"
      enable-security-schemes: true
      api-path: "/api/**"
```

### Monitoring Configuration

```yaml
enterprise:
  starter:
    monitoring:
      enabled: true
      enable-metrics: true
      enable-health-checks: true
      enable-tracing: false
      enable-slow-query-detection: true
      slow-query-threshold-ms: 1000
      enable-error-tracking: true
      expose-prometheus: true
      metrics-path: "/actuator/prometheus"
```

### Resilience Configuration

```yaml
enterprise:
  starter:
    resilience:
      enabled: true
```

## Features in Detail

### Request/Response Logging

The starter automatically logs all HTTP requests and responses with the following information:

- **Correlation ID**: Automatically generated or extracted from headers
- **Service Name**: From `spring.application.name`
- **Consumer Details**: IP address and User-Agent
- **Request Details**: Method, URI, headers, body
- **Response Details**: Status code, headers, body, duration
- **Performance Metrics**: Request duration in milliseconds

**Log Format**:
```json
{
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "serviceName": "my-service",
  "direction": "INCOMING",
  "type": "REQUEST",
  "method": "POST",
  "uri": "/api/users",
  "headers": {...},
  "body": "...",
  "timestamp": "2024-01-15T10:30:00"
}
```

### Authentication

The starter supports multiple authentication mechanisms:

#### JWT Authentication

1. Extract token from `Authorization: Bearer <token>` header
2. Validate token signature and expiration
3. Extract user roles from token claims
4. Apply RBAC and operation rules

#### API Key Authentication

1. Extract API key from configured header (default: `X-API-Key`)
2. Validate against configured API keys map
3. Assign roles based on API key configuration

#### Basic Authentication

1. Decode Base64 credentials from `Authorization: Basic <credentials>` header
2. Validate username/password against configured users
3. Assign default role (USER)

#### OAuth2 Authentication

1. Extract token from `Authorization: Bearer <token>` header
2. Call OAuth2 introspection endpoint
3. Validate token is active
4. Extract user info and scopes/roles

### Rate Limiting

Rate limiting prevents abuse by limiting requests per minute:

- **In-memory storage**: Simple sliding window algorithm
- **Per-IP limiting**: Default identifier is IP address
- **Configurable limits**: Set via `rate-limit-per-minute`
- **HTTP 429 Response**: Returns "Too Many Requests" when limit exceeded

**Note**: For distributed systems, consider using Redis-based rate limiting.

### Circuit Breaker & Retry

Built on Resilience4j:

- **Circuit Breaker**: Prevents cascading failures
  - Default: 50% failure rate threshold
  - Sliding window: 10 requests
  - Open state duration: 60 seconds

- **Retry**: Automatic retry for failed requests
  - Default: 3 attempts
  - Wait duration: 1 second with exponential backoff

### Where to Use Resilience Patterns: Gateway vs API Layer

This starter implements resilience patterns at the **API/Service Layer**, which is the recommended approach for individual services. Here's when to use resilience at each layer:

#### Architecture Overview

```
┌─────────────────────────────────────────┐
│  API Gateway Layer                     │
│  - Circuit Breaker (global)            │
│  - Rate Limiting (global)              │
│  - Timeout (global)                    │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴────────┐
       │                │
┌──────▼──────┐  ┌─────▼──────┐
│ Service A   │  │ Service B  │
│             │  │            │
│ - Circuit   │  │ - Circuit  │
│   Breaker   │  │   Breaker  │
│ - Retry     │  │ - Retry    │
│ (outgoing)  │  │ (outgoing) │
└─────────────┘  └────────────┘
```

#### API/Service Layer (This Starter) ✅

**Use resilience patterns at the service layer for:**

- ✅ **Service-to-Service Calls**: When Service A calls Service B
- ✅ **External API Calls**: When calling third-party APIs
- ✅ **Database Resilience**: Retry logic for database operations
- ✅ **Service-Specific Configurations**: Different resilience needs per service
- ✅ **Independence**: Services work with or without a gateway

**Benefits:**
- Service-specific configurations
- Fine-grained control per service
- Better observability (service-level metrics)
- Works independently of gateway infrastructure

**Example Use Cases:**
```java
// Service calling external API
@Service
public class PaymentService {
    @Autowired
    private CircuitBreaker paymentCircuitBreaker;
    
    public PaymentResult processPayment(PaymentRequest request) {
        return paymentCircuitBreaker.executeSupplier(() -> {
            return externalPaymentGateway.process(request);
        });
    }
}

// Service calling another service
@Service
public class OrderService {
    @Autowired
    private RestTemplate restTemplate; // With circuit breaker
    
    public User getUser(String userId) {
        return restTemplate.getForObject(
            "http://user-service/api/users/" + userId,
            User.class
        );
    }
}
```

#### Gateway Layer

**Use resilience patterns at the gateway layer for:**

- ✅ **Centralized Protection**: Protect gateway from downstream failures
- ✅ **Global Rate Limiting**: Consistent rate limiting across all services
- ✅ **Edge-Level Retries**: Retry for network/transient gateway issues
- ✅ **Load Balancing**: Failover and health checks

**Benefits:**
- Centralized configuration
- Consistent behavior across services
- Protects gateway infrastructure
- Less code in individual services

**Note**: Gateway-layer resilience is typically handled by:
- Spring Cloud Gateway with Resilience4j
- Dedicated API gateways (Kong, AWS API Gateway, etc.)
- Service mesh solutions (Istio, Linkerd)

#### Decision Matrix

| Scenario | Recommended Layer | Reason |
|----------|-------------------|--------|
| Service calling external API | **API Layer** ✅ | Service-specific needs |
| Service calling another service | **API Layer** ✅ | Direct service-to-service |
| Gateway routing to services | **Gateway Layer** | Centralized protection |
| Global rate limiting | **Gateway Layer** | Centralized policy |
| Database resilience | **API Layer** ✅ | Service-specific |
| Third-party integration | **API Layer** ✅ | Service-specific |
| Edge protection | **Gateway Layer** | Infrastructure level |

#### Best Practice: Defense in Depth

For maximum resilience, use **both layers**:

1. **Gateway Layer**: Protects gateway infrastructure and provides global policies
2. **API Layer** (This Starter): Protects individual services and provides service-specific resilience

**Example Layered Approach:**
```
Client Request
    ↓
API Gateway (Circuit Breaker + Rate Limiting)
    ↓
Service A (Circuit Breaker + Retry for outgoing calls)
    ↓
External API / Service B (Protected by Service A's resilience)
```

#### Why This Starter Uses API Layer

This starter implements resilience at the **API/Service Layer** because:

1. ✅ **Target Audience**: Designed for individual Spring Boot services
2. ✅ **Flexibility**: Services can be used with or without a gateway
3. ✅ **Service-Specific Needs**: Each service may have different resilience requirements
4. ✅ **Outgoing Calls**: Resilience is needed for service-to-service and external API calls
5. ✅ **Independence**: Services don't depend on gateway infrastructure

#### When to Use Gateway Layer Resilience

Consider gateway-layer resilience when:
- You have a dedicated API Gateway (Spring Cloud Gateway, Kong, etc.)
- You need centralized rate limiting across all services
- You want to protect gateway infrastructure from cascading failures
- You need global timeout policies

**Note**: Gateway-layer resilience complements (not replaces) service-layer resilience. Use both for defense in depth.

## Examples

### Example 1: Simple REST Controller

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        // Request/response automatically logged
        // Correlation ID automatically propagated
        return ResponseEntity.ok(userService.findById(id));
    }
}
```

### Example 2: Using Authentication Context

```java
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    
    @GetMapping
    public ResponseEntity<Profile> getProfile(HttpServletRequest request) {
        AuthContext authContext = (AuthContext) request.getAttribute("authContext");
        String username = authContext.getUsername();
        List<String> roles = authContext.getRoles();
        
        return ResponseEntity.ok(profileService.getProfile(username));
    }
}
```

### Example 3: Making External API Calls

```java
@Service
public class ExternalServiceClient {
    
    @Autowired
    private RestTemplate restTemplate;  // Auto-configured with logging interceptor
    
    public UserData fetchUserData(String userId) {
        // Outgoing request automatically logged
        // Correlation ID automatically added to headers
        return restTemplate.getForObject(
            "https://external-api.com/users/" + userId,
            UserData.class
        );
    }
}
```

### Example 4: Using Circuit Breaker

```java
@Service
public class ResilientService {
    
    @Autowired
    private CircuitBreaker circuitBreaker;
    
    public String callExternalService() {
        return circuitBreaker.executeSupplier(() -> {
            // Your external service call
            return externalClient.call();
        });
    }
}
```

### Example 5: Performance Monitoring

```java
@Service
public class BusinessService {
    
    @MonitorPerformance
    public void performSlowOperation() {
        // Execution time automatically tracked
        // Slow operations logged if threshold exceeded
        // Metrics recorded to Micrometer
    }
}
```

## API Reference

### Configuration Properties

All configuration properties are documented in `spring-configuration-metadata.json` for IDE autocomplete support.

**Main Properties**:
- `enterprise.starter.logging.*` - Logging configuration
- `enterprise.starter.authentication.*` - Authentication configuration
- `enterprise.starter.swagger.*` - Swagger/OpenAPI configuration
- `enterprise.starter.monitoring.*` - Monitoring configuration
- `enterprise.starter.resilience.*` - Resilience configuration

### Programmatic Access

#### Getting Correlation ID

```java
import com.yourcompany.starter.util.CorrelationIdUtil;

String correlationId = CorrelationIdUtil.getCorrelationId();
```

#### Getting Auth Context

```java
AuthContext authContext = (AuthContext) request.getAttribute("authContext");
if (authContext != null && authContext.isAuthenticated()) {
    String username = authContext.getUsername();
    List<String> roles = authContext.getRoles();
}
```

#### Recording Custom Metrics

```java
@Autowired
private MetricsService metricsService;

metricsService.recordHttpRequest("GET", "/api/users", 200, 150);
metricsService.recordError("ValidationError", "Invalid input");
metricsService.recordBusinessMetric("orders.processed", 1.0, "region", "us-east");
```

## Best Practices

### Security

1. **Never commit secrets**: Use environment variables or secret management
2. **Use strong secret keys**: Minimum 256 bits for JWT
3. **Hash passwords**: Use BCrypt or similar for Basic Auth
4. **Enable security headers**: Keep `enable-security-headers: true`
5. **Regular key rotation**: Rotate API keys and JWT secrets regularly

### Logging

1. **Exclude sensitive paths**: Add health checks and metrics to excluded paths
2. **Configure sensitive fields**: List all fields containing sensitive data
3. **Set appropriate log levels**: Use DEBUG in development, INFO in production
4. **Monitor log volume**: Adjust `max-body-length` to control log size

### Performance

1. **Use async logging**: Enable `async-logging: true` for high throughput
2. **Limit body logging**: Set `max-body-length` appropriately
3. **Exclude noisy endpoints**: Add actuator endpoints to exclusions
4. **Monitor slow queries**: Set `slow-query-threshold-ms` based on SLA

### Monitoring

1. **Enable Prometheus**: Set `expose-prometheus: true`
2. **Configure alerting**: Set up alerts based on error rates and latency
3. **Track business metrics**: Use `MetricsService` for custom metrics
4. **Monitor correlation IDs**: Use correlation IDs for distributed tracing

## Troubleshooting

### Issue: Logs not appearing

**Solution**:
- Check `enterprise.starter.logging.enabled: true`
- Verify log level configuration
- Ensure paths are not in `excluded-paths`

### Issue: Authentication not working

**Solution**:
- Verify `enterprise.starter.authentication.enabled: true`
- Check authentication type matches your setup
- Verify secret key is configured (for JWT)
- Check API keys are configured (for API_KEY type)
- Review audit logs for authentication failures

### Issue: Swagger UI not accessible

**Solution**:
- Verify `enterprise.starter.swagger.enabled: true`
- Check Swagger UI path: `http://localhost:8080/swagger-ui.html`
- Ensure `api-path` matches your controller paths
- Check that paths are not in `exclude-paths`

### Issue: Rate limiting too aggressive

**Solution**:
- Increase `rate-limit-per-minute` value
- Consider per-user rate limiting instead of per-IP
- For distributed systems, use Redis-based rate limiting

### Issue: Circuit breaker opening too frequently

**Solution**:
- Adjust `failure-rate-threshold` in ResilienceConfig
- Increase `sliding-window-size`
- Check external service health
- Review error logs for root cause

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues, questions, or contributions:
- Open an issue on GitHub
- Contact: your-email@example.com

## Testing

The project includes comprehensive unit and integration tests. See [TESTING.md](TESTING.md) for detailed information.

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CorrelationIdUtilTest

# Run with coverage
mvn test jacoco:report
```

### Test Coverage

- **Unit Tests**: 90%+ coverage for utilities, services, filters, and interceptors
- **Integration Tests**: End-to-end tests for complete request/response flows
- **Configuration Tests**: Auto-configuration behavior verification

## Version History

- **1.0.0** (Current)
  - Initial release
  - Request/response logging
  - Multiple authentication providers
  - Swagger/OpenAPI integration
  - Monitoring and metrics
  - Circuit breaker and retry mechanisms
  - Comprehensive test suite

---

**Built with ❤️ for Spring Boot developers**

