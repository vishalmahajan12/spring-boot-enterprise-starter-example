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
- ✅ **Multi-Level Caching** (Caffeine L1 + Redis L2)

## Prerequisites

1. Java 17+
2. Maven 3.6+
3. Enterprise Starter library built and installed in local Maven repository
4. **Redis** (optional, for distributed caching) - See [Redis Setup](#redis-setup) section

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

### 13. Multi-Level Caching

The sample service demonstrates multi-level caching with Caffeine (L1) and optional Redis (L2).

#### Test Caching (Caffeine Only)

```bash
# First call - slow (cache miss, fetches from source)
curl "http://localhost:8080/api/demo/cache/config?key=test1"

# Second call - fast (cache hit from L1 - Caffeine)
curl "http://localhost:8080/api/demo/cache/config?key=test1"

# Notice the durationMs difference - second call should be much faster
```

#### Test Cache Operations

```bash
# Cache user data
curl "http://localhost:8080/api/demo/cache/user?id=1"

# Update cache
curl -X PUT "http://localhost:8080/api/demo/cache/config?key=test1&value=updated"

# Evict specific cache entry
curl -X DELETE "http://localhost:8080/api/demo/cache/config?key=test1"

# Clear entire cache
curl -X POST "http://localhost:8080/api/demo/cache/config/clear"

# Cache API responses
curl "http://localhost:8080/api/demo/cache/api-response?endpoint=/api/test"

# Compare with uncached endpoint
curl "http://localhost:8080/api/demo/cache/uncached?key=test1"
```

#### Enable Redis for Distributed Caching

1. **Add Redis dependency** to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

2. **Start Redis** (using Docker):
```bash
docker run -d --name redis-test -p 6379:6379 redis:latest
```

3. **Enable Redis** in `application.yml`:
```yaml
enterprise:
  starter:
    cache:
      redis-enabled: true  # Change from false to true

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

4. **Test Redis caching**:
```bash
# First call - stores in both L1 and L2
curl "http://localhost:8080/api/demo/cache/config?key=test1"

# Check Redis keys
redis-cli KEYS "*"

# Get cached value from Redis
redis-cli GET "config::test1"
```

#### Verify Cache Managers

Check which cache managers are active:
```bash
# If you add the cache info endpoint (see below)
curl http://localhost:8080/api/cache/info
```

Expected response with Redis enabled:
```json
{
  "caffeineAvailable": true,
  "redisAvailable": true,
  "compositeAvailable": true,
  "activeCacheManager": "composite"
}
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

### Cache Endpoints (Multi-Level Caching)
- `GET /api/demo/cache/config?key=xxx` - Test cached config (with timing)
- `GET /api/demo/cache/user?id=xxx` - Test cached user data
- `PUT /api/demo/cache/config?key=xxx&value=yyy` - Update cache
- `DELETE /api/demo/cache/config?key=xxx` - Evict from cache
- `POST /api/demo/cache/config/clear` - Clear all cache entries
- `GET /api/demo/cache/api-response?endpoint=xxx` - Cache API responses
- `GET /api/demo/cache/uncached?key=xxx` - Compare with uncached endpoint

## Configuration

All configuration is in `src/main/resources/application.yml`. Key settings:

- **Logging**: Enabled with sensitive data masking
- **Authentication**: JWT (can be changed to API_KEY or BASIC)
- **Rate Limiting**: 10 requests per minute (low for testing)
- **Monitoring**: Enabled with Prometheus export
- **Caching**: 
  - Caffeine (L1): Enabled by default - fast local cache
  - Redis (L2): Optional - distributed cache (set `redis-enabled: true`)

### Cache Configuration

```yaml
enterprise:
  starter:
    cache:
      enabled: true
      # Caffeine (L1) - Fast local cache
      caffeine-max-size: 10000
      caffeine-ttl-seconds: 300  # 5 minutes
      caffeine-access-expiration-seconds: 180  # 3 minutes
      # Redis (L2) - Distributed cache (optional)
      redis-enabled: false  # Set to true if Redis is available
      redis-ttl-seconds: 3600  # 1 hour
      cache-names:
        - default
        - users
        - config
        - tokens
        - api-responses

spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2000ms
```

## Redis Setup

### Option 1: Docker (Recommended)

```bash
# Start Redis container
docker run -d --name redis-test -p 6379:6379 redis:latest

# Verify Redis is running
docker ps | grep redis

# Test Redis connection
docker exec -it redis-test redis-cli ping
# Should return: PONG
```

### Option 2: Local Installation

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install redis-server
sudo systemctl start redis
```

**macOS:**
```bash
brew install redis
brew services start redis
```

### Verify Redis Connection

```bash
# Using redis-cli
redis-cli ping
# Should return: PONG

# Monitor Redis commands
redis-cli MONITOR
```

### Testing Redis Caching

1. **Enable Redis** in `application.yml`:
   ```yaml
   enterprise:
     starter:
       cache:
         redis-enabled: true
   ```

2. **Start the application** and make cache calls

3. **Check Redis keys**:
   ```bash
   redis-cli KEYS "*"
   # Should show cache keys like: "config::test1", "users::1"
   ```

4. **Verify multi-level caching**:
   - First call: Slow (fetches from source, stores in L1 and L2)
   - Second call: Fast (hits L1 - Caffeine)
   - After L1 expires: Still fast (hits L2 - Redis)

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
- [ ] **Caching works** - First call slow, subsequent calls fast
- [ ] **Cache eviction works** - Cache cleared after evict call
- [ ] **Redis caching works** (if Redis enabled) - Keys visible in Redis
- [ ] **Multi-level cache** - L1 (Caffeine) and L2 (Redis) both working

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

### Issue: Redis cache not working

**Solution**:
- Verify Redis is running: `redis-cli ping`
- Check `redis-enabled: true` in configuration
- Verify `spring-boot-starter-data-redis` dependency is added
- Check Redis connection settings in `application.yml`
- Review application logs for Redis connection errors

### Issue: Cache not improving performance

**Solution**:
- Verify `enterprise.starter.cache.enabled: true`
- Check cache annotations (`@Cacheable`) are present on methods
- Verify cache names match configuration
- Test with multiple calls to see cache hit improvement

## Cache Examples

### Example 1: Basic Caching

```java
@Service
public class UserService {
    @Cacheable(value = "users", key = "#id")
    public User getUser(Long id) {
        // This will be cached after first call
        return userRepository.findById(id);
    }
}
```

### Example 2: Cache Update

```java
@CachePut(value = "users", key = "#user.id")
public User updateUser(User user) {
    // Cache is automatically updated
    return userRepository.save(user);
}
```

### Example 3: Cache Eviction

```java
@CacheEvict(value = "users", key = "#id")
public void deleteUser(Long id) {
    // Cache entry is removed
    userRepository.deleteById(id);
}
```

## Next Steps

1. Experiment with different authentication types
2. Test resilience patterns with external services
3. Monitor metrics in Prometheus
4. **Enable Redis** for distributed caching across instances
5. **Test multi-level caching** - Compare L1 vs L2 performance
6. Adjust cache TTL and size based on your needs
7. Integrate with your own services

