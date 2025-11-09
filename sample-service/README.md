# Sample Service - Enterprise Starter Demo

This is a sample Spring Boot service that demonstrates all features of the Enterprise Starter library.

## Features Demonstrated

- ✅ Request/Response Logging with Correlation IDs
- ✅ Authentication (JWT, API Key, Basic Auth)
- ✅ Role-Based Access Control (RBAC)
- ✅ Swagger/OpenAPI Documentation
- ✅ Monitoring and Metrics
- ✅ Resilience Patterns (Circuit Breaker, Retry)
- ✅ Rate Limiting
- ✅ Performance Monitoring
- ✅ Error Tracking

## Prerequisites

1. Java 17+
2. Maven 3.6+
3. Enterprise Starter library built and installed in local Maven repository

## Building the Enterprise Starter

Before running the sample service, build and install the Enterprise Starter:

```bash
cd ..
mvn clean install
```

## Running the Sample Service

```bash
cd sample-service
mvn spring-boot:run
```

The service will start on `http://localhost:8080`

## Testing the Features

### 1. Swagger UI

Access Swagger UI to see API documentation:
```
http://localhost:8080/swagger-ui.html
```

### 2. Public Endpoints (No Auth Required)

```bash
# Health check
curl http://localhost:8080/api/public/health

# Service info
curl http://localhost:8080/api/public/info
```

### 3. Request/Response Logging

```bash
# Test logging with sensitive data
curl -X POST http://localhost:8080/api/demo/logging \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"secret123","token":"abc123"}'

# Check logs - password and token should be masked
```

### 4. Authentication - JWT

```bash
# Step 1: Login to get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Response will contain a token
# Step 2: Use token in Authorization header
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer <YOUR_TOKEN_HERE>"

# Test without token (should return 401)
curl http://localhost:8080/api/user/profile
```

### 5. Authentication - API Key

Change authentication type in `application.yml`:
```yaml
enterprise:
  starter:
    authentication:
      type: API_KEY
```

Then test:
```bash
curl http://localhost:8080/api/user/profile \
  -H "X-API-Key: demo-api-key-123"
```

### 6. Authentication - Basic Auth

Change authentication type in `application.yml`:
```yaml
enterprise:
  starter:
    authentication:
      type: BASIC
```

Then test:
```bash
curl http://localhost:8080/api/user/profile \
  -u admin:admin123
```

### 7. Role-Based Access Control

```bash
# Login as admin (has ADMIN role)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

# Access admin endpoint (requires ADMIN role)
curl http://localhost:8080/api/admin/stats \
  -H "Authorization: Bearer $TOKEN"

# Login as regular user (only USER role)
USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"user123"}' | jq -r '.token')

# Try to access admin endpoint (should return 403)
curl http://localhost:8080/api/admin/stats \
  -H "Authorization: Bearer $USER_TOKEN"
```

### 8. Rate Limiting

```bash
# Make multiple requests quickly (limit is 10 per minute)
for i in {1..15}; do
  curl http://localhost:8080/api/demo/rate-limit
  echo "Request $i"
done

# After 10 requests, you should get 429 Too Many Requests
```

### 9. Performance Monitoring

```bash
# Test slow operation detection
curl http://localhost:8080/api/demo/slow-operation

# Check logs for slow query warning (threshold is 1000ms)
```

### 10. External API Calls (Outgoing Request Logging)

```bash
# Test outgoing request logging
curl http://localhost:8080/api/demo/external-api?url=https://httpbin.org/json

# Check logs for outgoing request/response logging
```

### 11. Error Tracking

```bash
# Test error tracking
curl http://localhost:8080/api/demo/error?shouldFail=true

# Check logs and metrics for error tracking
```

### 12. Monitoring Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus
```

## API Endpoints

### Public Endpoints (No Auth)
- `GET /api/public/health` - Health check
- `GET /api/public/info` - Service information

### Authentication
- `POST /api/auth/login` - Generate JWT token
- `GET /api/auth/verify` - Verify authentication

### User Endpoints (Requires Auth, USER role)
- `GET /api/user/profile` - Get current user profile
- `GET /api/user/users` - List all users
- `GET /api/user/users/{id}` - Get user by ID
- `POST /api/user/users` - Create new user

### Admin Endpoints (Requires Auth, ADMIN role)
- `GET /api/admin/stats` - Get system statistics
- `DELETE /api/admin/users/{id}` - Delete user

### Demo Endpoints (Various features)
- `GET /api/demo/logging` - Test request/response logging
- `POST /api/demo/logging` - Test POST request logging
- `GET /api/demo/slow-operation` - Test slow query detection
- `GET /api/demo/external-api` - Test external API calls
- `GET /api/demo/error` - Test error tracking
- `GET /api/demo/rate-limit` - Test rate limiting

## Configuration

All configuration is in `src/main/resources/application.yml`. Key settings:

- **Logging**: Enabled with sensitive data masking
- **Authentication**: JWT (can be changed to API_KEY or BASIC)
- **Rate Limiting**: 10 requests per minute (low for testing)
- **Monitoring**: Enabled with Prometheus export

## Testing Checklist

- [ ] Request/Response logging appears in logs
- [ ] Correlation IDs are generated and propagated
- [ ] Sensitive data is masked in logs
- [ ] JWT authentication works
- [ ] API Key authentication works (when configured)
- [ ] Basic Auth works (when configured)
- [ ] RBAC restricts access based on roles
- [ ] Rate limiting blocks excessive requests
- [ ] Slow query detection logs warnings
- [ ] Outgoing requests are logged
- [ ] Errors are tracked and logged
- [ ] Swagger UI displays API documentation
- [ ] Prometheus metrics are available
- [ ] Circuit breaker protects external calls

## Troubleshooting

### Issue: Cannot find Enterprise Starter dependency

**Solution**: Build and install the Enterprise Starter first:
```bash
cd ..
mvn clean install
```

### Issue: Authentication not working

**Solution**: 
- Check `application.yml` configuration
- Verify secret key is set (for JWT)
- Check API keys are configured (for API_KEY type)
- Review audit logs for authentication failures

### Issue: Rate limiting too aggressive

**Solution**: Increase `rate-limit-per-minute` in `application.yml`

### Issue: Swagger UI not accessible

**Solution**: 
- Check `enterprise.starter.swagger.enabled: true`
- Access at `http://localhost:8080/swagger-ui.html`

## Next Steps

1. Experiment with different authentication types
2. Test resilience patterns with external services
3. Monitor metrics in Prometheus
4. Adjust configuration based on your needs
5. Integrate with your own services

