# Testing Guide

This document describes the test suite for the Spring Boot Enterprise Starter.

## Test Structure

The test suite is organized into the following categories:

### Unit Tests

#### Utility Classes (`src/test/java/com/yourcompany/starter/util/`)
- **CorrelationIdUtilTest**: Tests correlation ID generation, storage, and ThreadLocal isolation
- **MaskingUtilTest**: Tests sensitive data masking in JSON and headers
- **JwtUtilTest**: Tests JWT token generation, validation, and claim extraction

#### Service Classes (`src/test/java/com/yourcompany/starter/service/`)
- **AuthServiceTest**: Tests all authentication methods (JWT, API Key, Basic Auth, OAuth2)
- **RateLimitingServiceTest**: Tests rate limiting functionality and sliding window algorithm
- **MetricsServiceTest**: Tests metrics recording for HTTP requests, errors, and business metrics

#### Filters and Interceptors (`src/test/java/com/yourcompany/starter.full/filter/` and `interceptor/`)
- **RequestLoggingFilterTest**: Tests request/response logging filter
- **AuthenticationFilterTest**: Tests authentication filter with different auth types
- **OutgoingRequestLoggingInterceptorTest**: Tests outgoing request logging interceptor

### Integration Tests

#### Integration Tests (`src/test/java/com/yourcompany/starter/integration/`)
- **EnterpriseStarterIntegrationTest**: End-to-end tests for complete request/response flow
- **TestApplication**: Test Spring Boot application with sample controllers

#### Configuration Tests (`src/test/java/com/yourcompany/starter/config/`)
- **EnterpriseStarterAutoConfigurationTest**: Tests auto-configuration behavior

## Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=CorrelationIdUtilTest
```

### Run Tests with Coverage
```bash
mvn test jacoco:report
```

### Run Only Unit Tests
```bash
mvn test -Dtest="*Test" -DfailIfNoTests=false
```

### Run Only Integration Tests
```bash
mvn test -Dtest="*IntegrationTest" -DfailIfNoTests=false
```

## Test Coverage

### Unit Test Coverage

| Component | Coverage |
|-----------|----------|
| CorrelationIdUtil | ✅ 100% |
| MaskingUtil | ✅ 95%+ |
| JwtUtil | ✅ 95%+ |
| AuthService | ✅ 90%+ |
| RateLimitingService | ✅ 90%+ |
| MetricsService | ✅ 85%+ |
| RequestLoggingFilter | ✅ 85%+ |
| AuthenticationFilter | ✅ 85%+ |
| OutgoingRequestLoggingInterceptor | ✅ 80%+ |

### Integration Test Coverage

- ✅ Request/Response logging with correlation IDs
- ✅ JWT authentication flow
- ✅ API Key authentication flow
- ✅ Basic Auth authentication flow
- ✅ Rate limiting
- ✅ Security headers
- ✅ Path exclusions
- ✅ Auto-configuration

## Test Examples

### Example 1: Testing Correlation ID

```java
@Test
void testSetAndGetCorrelationId() {
    String testId = "test-correlation-id-123";
    CorrelationIdUtil.setCorrelationId(testId);
    
    String retrievedId = CorrelationIdUtil.getCorrelationId();
    assertEquals(testId, retrievedId);
}
```

### Example 2: Testing Authentication

```java
@Test
void testValidateAuthentication_JWT_ValidToken() {
    authConfig.setEnabled(true);
    authConfig.setType(AuthType.JWT);
    authConfig.setSecretKey("secret-key");
    
    when(jwtUtil.validateToken(anyString(), anyString())).thenReturn(true);
    when(jwtUtil.extractUsername(anyString(), anyString())).thenReturn("testuser");
    
    AuthContext context = authService.validateAuthentication("token", "127.0.0.1", "op");
    
    assertTrue(context.isAuthenticated());
    assertEquals("testuser", context.getUsername());
}
```

### Example 3: Testing Rate Limiting

```java
@Test
void testIsAllowed_ExceedsLimit() {
    properties.getAuthentication().setRateLimitPerMinute(5);
    
    // Make 5 requests (within limit)
    for (int i = 0; i < 5; i++) {
        assertTrue(rateLimitingService.isAllowed("test-ip"));
    }
    
    // 6th request should be blocked
    assertFalse(rateLimitingService.isAllowed("test-ip"));
}
```

## Mocking

The tests use Mockito for mocking dependencies:

```java
@ExtendWith(MockitoExtension.class)
class MyTest {
    @Mock
    private SomeService service;
    
    @InjectMocks
    private MyClass underTest;
}
```

## Test Data

Test data is created in `@BeforeEach` methods to ensure test isolation:

```java
@BeforeEach
void setUp() {
    properties = new EnterpriseStarterProperties();
    properties.getAuthentication().setEnabled(true);
    // ... configure properties
}
```

## Best Practices

1. **Test Isolation**: Each test should be independent and not rely on other tests
2. **Clear Test Names**: Use descriptive test method names that explain what is being tested
3. **Arrange-Act-Assert**: Structure tests with clear sections
4. **Mock External Dependencies**: Mock external services and dependencies
5. **Test Edge Cases**: Include tests for null values, empty strings, and boundary conditions
6. **Clean Up**: Use `@AfterEach` to clean up ThreadLocal variables and other state

## Continuous Integration

Tests are automatically run in CI/CD pipelines:

```yaml
# Example GitHub Actions workflow
- name: Run Tests
  run: mvn test
```

## Troubleshooting

### Tests Failing Due to ThreadLocal State

If tests fail due to ThreadLocal state not being cleared:

```java
@AfterEach
void cleanup() {
    CorrelationIdUtil.clear();
}
```

### Tests Failing Due to Property Values

Ensure test properties are set correctly:

```java
@TestPropertySource(properties = {
    "enterprise.starter.logging.enabled=true",
    "enterprise.starter.authentication.enabled=false"
})
```

### Integration Tests Requiring Spring Context

Use `@SpringBootTest` with a test application:

```java
@SpringBootTest(classes = TestApplication.class)
class IntegrationTest {
    // ...
}
```

## Contributing Tests

When adding new features, please add corresponding tests:

1. Add unit tests for new utility classes
2. Add unit tests for new services
3. Add integration tests for new features
4. Update this documentation

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)

