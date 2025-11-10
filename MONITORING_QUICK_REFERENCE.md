# Monitoring & Observability - Quick Reference

## 🚀 Quick Start

### 1. Start the Application
```bash
cd sample-service
mvn spring-boot:run
```

### 2. Run Exploration Script
```bash
./explore-monitoring.sh
# Or with custom URL:
./explore-monitoring.sh http://localhost:8080
```

### 3. Access Key Endpoints

| Endpoint | Description |
|----------|-------------|
| `http://localhost:8080/actuator/health` | Health check |
| `http://localhost:8080/actuator/prometheus` | Prometheus metrics |
| `http://localhost:8080/actuator/metrics` | List all metrics |
| `http://localhost:8080/actuator/info` | Application info |

## 📊 Available Metrics

### Automatic Metrics (Built-in)
- `http.request.duration` - Request duration (Timer)
- `http.request.count` - Request count (Counter)
- `application.errors` - Error count (Counter)
- `method.execution.time` - Method execution time (via @MonitorPerformance)
- `method.execution.errors` - Method execution errors (Counter)
- `jvm.*` - JVM metrics (memory, threads, GC, etc.)

### Custom Metrics (via MetricsService)
- `business.metric.*` - Custom business metrics
- `operation.execution.time` - Custom operation timers

## 🔧 Using MetricsService

### Inject MetricsService
```java
@Autowired
private MetricsService metricsService;
```

### Record HTTP Request
```java
metricsService.recordHttpRequest("GET", "/api/users", 200, 150);
```

### Record Error
```java
metricsService.recordError("ValidationError", "Invalid input");
```

### Record Business Metric
```java
metricsService.recordBusinessMetric("orders.processed", 1.0, 
    "region", "us-east");
```

### Record Execution Time
```java
long start = System.currentTimeMillis();
// ... operation
metricsService.recordExecutionTime("dataProcessing", 
    System.currentTimeMillis() - start);
```

## 🎯 Performance Monitoring

### Using @MonitorPerformance Annotation
```java
import com.yourcompany.starter.annotation.MonitorPerformance;

@MonitorPerformance
public List<User> fetchUsers() {
    // Execution time automatically tracked
    return userRepository.findAll();
}
```

## 🔍 Exploring Metrics

### View All Metrics
```bash
curl http://localhost:8080/actuator/metrics | jq '.names[]'
```

### View Specific Metric
```bash
curl http://localhost:8080/actuator/metrics/http.request.count | jq
```

### View Metric with Tags
```bash
curl "http://localhost:8080/actuator/metrics/http.request.count?tag=status:200" | jq
```

### Prometheus Format
```bash
curl http://localhost:8080/actuator/prometheus
```

## ⚙️ Configuration

### Enable/Disable Features
```yaml
enterprise:
  starter:
    monitoring:
      enabled: true                    # Master switch
      enable-metrics: true            # Metrics collection
      enable-health-checks: true      # Health endpoints
      enable-tracing: false           # Distributed tracing
      enable-slow-query-detection: true
      slow-query-threshold-ms: 1000   # Slow query threshold
      enable-error-tracking: true
      expose-prometheus: true
```

### Expose Actuator Endpoints
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
```

## 🐛 Troubleshooting

### Metrics Not Appearing?
- ✅ Check `enterprise.starter.monitoring.enabled: true`
- ✅ Verify `enterprise.starter.monitoring.enable-metrics: true`
- ✅ Ensure endpoints are exposed in `management.endpoints.web.exposure.include`

### Prometheus Endpoint Not Working?
- ✅ Check `expose-prometheus: true`
- ✅ Verify `management.prometheus.metrics.export.enabled: true`
- ✅ Ensure `/actuator/prometheus` is in exposed endpoints

## 📚 More Information

- **Full Guide**: See `MONITORING_GUIDE.md`
- **Exploration Script**: Run `./explore-monitoring.sh`
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- **Micrometer**: https://micrometer.io/docs

