# Monitoring & Observability Guide

This guide will help you explore and utilize the monitoring and observability features available in the Spring Boot Enterprise Starter.

## Table of Contents

1. [Overview](#overview)
2. [Available Features](#available-features)
3. [Accessing Metrics](#accessing-metrics)
4. [Health Checks](#health-checks)
5. [Using MetricsService](#using-metricsservice)
6. [Performance Monitoring](#performance-monitoring)
7. [Error Tracking](#error-tracking)
8. [Prometheus Integration](#prometheus-integration)
9. [Custom Metrics](#custom-metrics)
10. [Exploring Endpoints](#exploring-endpoints)

## Overview

The Enterprise Starter provides comprehensive monitoring and observability capabilities built on:
- **Spring Boot Actuator** - Health checks and metrics endpoints
- **Micrometer** - Metrics collection and aggregation
- **Prometheus** - Metrics export format
- **Custom MetricsService** - Business metrics tracking

## Available Features

### ✅ Currently Enabled Features

Based on your `application.yml` configuration:

- ✅ **Metrics Collection** (`enable-metrics: true`)
- ✅ **Health Checks** (`enable-health-checks: true`)
- ✅ **Prometheus Export** (`expose-prometheus: true`)
- ✅ **Slow Query Detection** (`enable-slow-query-detection: true`)
- ✅ **Error Tracking** (`enable-error-tracking: true`)
- ❌ **Tracing** (`enable-tracing: false`) - Not currently enabled

## Accessing Metrics

### 1. Actuator Endpoints

The following endpoints are exposed (configured in `management.endpoints.web.exposure.include`):

#### Health Check
```bash
# Basic health check
curl http://localhost:8080/actuator/health

# Health check with details (requires authorization)
curl http://localhost:8080/actuator/health
```

**Response Example:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 400000000000,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

#### Prometheus Metrics
```bash
# Get all metrics in Prometheus format
curl http://localhost:8080/actuator/prometheus
```

**Example Metrics Output:**
```
# HELP http_request_duration_seconds HTTP request duration
# TYPE http_request_duration_seconds summary
http_request_duration_seconds{application="sample-service",method="GET",path="/api/users",status="200",quantile="0.5",} 0.15
http_request_duration_seconds{application="sample-service",method="GET",path="/api/users",status="200",quantile="0.99",} 0.25

# HELP http_request_count_total HTTP request count
# TYPE http_request_count_total counter
http_request_count_total{application="sample-service",method="GET",path="/api/users",status="200",} 42.0

# HELP application_errors_total Application errors
# TYPE application_errors_total counter
application_errors_total{application="sample-service",type="ValidationError",message="Invalid input",} 3.0
```

#### General Metrics Endpoint
```bash
# Get metrics in JSON format
curl http://localhost:8080/actuator/metrics

# Get specific metric
curl http://localhost:8080/actuator/metrics/http.request.count

# Get metric with tags
curl "http://localhost:8080/actuator/metrics/http.request.count?tag=status:200"
```

#### Application Info
```bash
curl http://localhost:8080/actuator/info
```

## Health Checks

### Built-in Health Indicators

Spring Boot Actuator automatically provides:
- **Ping** - Basic application health
- **Disk Space** - Available disk space
- **Database** - Database connectivity (if configured)

### Custom Health Indicators

You can create custom health indicators:

```java
package com.yourcompany.sample.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CustomHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        // Add your custom health check logic here
        boolean isHealthy = checkExternalService();
        
        if (isHealthy) {
            return Health.up()
                .withDetail("externalService", "Available")
                .withDetail("responseTime", "50ms")
                .build();
        } else {
            return Health.down()
                .withDetail("externalService", "Unavailable")
                .withDetail("error", "Connection timeout")
                .build();
        }
    }
    
    private boolean checkExternalService() {
        // Your health check logic
        return true;
    }
}
```

## Using MetricsService

The `MetricsService` provides convenient methods to record custom metrics.

### Injecting MetricsService

```java
import com.yourcompany.starter.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {
    
    @Autowired
    private MetricsService metricsService;
    
    // ... your code
}
```

### Available Methods

#### 1. Record HTTP Request Metrics

```java
// Record HTTP request with method, path, status code, and duration
metricsService.recordHttpRequest("GET", "/api/users", 200, 150);
// Creates:
// - http.request.duration (Timer) with tags: method, path, status
// - http.request.count (Counter) with tags: method, path, status
```

#### 2. Record Error Metrics

```java
try {
    // your code
} catch (Exception e) {
    metricsService.recordError("ValidationError", e.getMessage());
    // Creates: application.errors counter with tags: type, message
    throw e;
}
```

#### 3. Record Business Metrics

```java
// Record a business metric with custom tags
metricsService.recordBusinessMetric("orders.processed", 1.0, 
    "region", "us-east", 
    "status", "completed");
// Creates: business.metric.orders.processed counter with custom tags
```

#### 4. Record Execution Time

```java
long startTime = System.currentTimeMillis();
// ... your operation
long duration = System.currentTimeMillis() - startTime;
metricsService.recordExecutionTime("dataProcessing", duration);
// Creates: operation.execution.time timer with tag: operation
```

## Performance Monitoring

### Using @MonitorPerformance Annotation

Annotate methods to automatically track execution time:

```java
import com.yourcompany.starter.annotation.MonitorPerformance;

@Service
public class DataService {
    
    @MonitorPerformance
    public List<User> fetchUsers() {
        // This method's execution time will be automatically tracked
        // Creates: method.execution.time timer with tags: class, method
        return userRepository.findAll();
    }
}
```

### Slow Query Detection

When a method takes longer than the configured threshold (`slow-query-threshold-ms: 1000`), a warning is logged:

```
WARN  - Slow operation detected: DataService.fetchUsers() took 1250ms (threshold: 1000ms)
```

The metric is still recorded, allowing you to:
- Set up alerts in Prometheus/Grafana
- Identify performance bottlenecks
- Track performance trends over time

## Error Tracking

### Automatic Error Tracking

The `GlobalExceptionHandler` automatically records errors:

```java
// Any unhandled exception is automatically tracked
// Creates: application.errors counter with tags: type, message
```

### Manual Error Tracking

You can also manually track errors:

```java
try {
    riskyOperation();
} catch (SpecificException e) {
    metricsService.recordError("SpecificError", e.getMessage());
    // Handle error
}
```

## Prometheus Integration

### Viewing Metrics

1. **Start your application**
   ```bash
   cd sample-service
   mvn spring-boot:run
   ```

2. **Access Prometheus endpoint**
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

3. **Set up Prometheus Server** (optional)

   Create `prometheus.yml`:
   ```yaml
   global:
     scrape_interval: 15s
   
   scrape_configs:
     - job_name: 'sample-service'
       metrics_path: '/actuator/prometheus'
       static_configs:
         - targets: ['localhost:8080']
   ```

4. **Set up Grafana** (optional)

   Import Prometheus as a data source and create dashboards to visualize:
   - Request rates
   - Error rates
   - Response times (p50, p95, p99)
   - Business metrics

## Custom Metrics

### Available Metric Types

The MetricsService uses Micrometer, which supports:

1. **Counter** - Incrementing values (e.g., request counts, error counts)
2. **Timer** - Time-based measurements (e.g., request duration, execution time)
3. **Gauge** - Current value measurements (e.g., queue size, active connections)
4. **Summary** - Distribution statistics

### Creating Custom Metrics Directly

If you need more control, inject `MeterRegistry` directly:

```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Gauge;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class CustomMetricsService {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    public void registerCustomGauge() {
        Gauge.builder("custom.queue.size", this, service -> getQueueSize())
            .description("Current queue size")
            .tag("queue", "processing")
            .register(meterRegistry);
    }
    
    private double getQueueSize() {
        return queue.size();
    }
}
```

## Exploring Endpoints

### Quick Test Script

Create a script to explore all monitoring endpoints:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== Health Check ==="
curl -s $BASE_URL/actuator/health | jq

echo -e "\n=== Available Metrics ==="
curl -s $BASE_URL/actuator/metrics | jq '.names[]'

echo -e "\n=== HTTP Request Count ==="
curl -s "$BASE_URL/actuator/metrics/http.request.count" | jq

echo -e "\n=== HTTP Request Duration ==="
curl -s "$BASE_URL/actuator/metrics/http.request.duration" | jq

echo -e "\n=== Application Errors ==="
curl -s "$BASE_URL/actuator/metrics/application.errors" | jq

echo -e "\n=== Prometheus Metrics (first 50 lines) ==="
curl -s $BASE_URL/actuator/prometheus | head -50

echo -e "\n=== Info ==="
curl -s $BASE_URL/actuator/info | jq
```

### Using jq for Pretty Output

Install `jq` for formatted JSON output:
```bash
# Ubuntu/Debian
sudo apt-get install jq

# macOS
brew install jq
```

## Configuration Reference

### Current Configuration

From `sample-service/src/main/resources/application.yml`:

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

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when-authorized
  prometheus:
    metrics:
      export:
        enabled: true
```

### Enabling Tracing (Optional)

To enable distributed tracing, you would need to:

1. Add tracing dependencies (e.g., Zipkin, Jaeger)
2. Set `enable-tracing: true`
3. Configure tracing backend

## Next Steps

1. **Explore Metrics**: Start your application and visit the endpoints listed above
2. **Add Custom Metrics**: Use `MetricsService` in your controllers/services
3. **Set up Prometheus**: Configure Prometheus to scrape your application
4. **Create Dashboards**: Use Grafana to visualize your metrics
5. **Set up Alerts**: Configure alerting rules based on your metrics

## Troubleshooting

### Metrics Not Appearing

- Check `enterprise.starter.monitoring.enabled: true`
- Verify `enterprise.starter.monitoring.enable-metrics: true`
- Ensure endpoints are exposed: `management.endpoints.web.exposure.include`

### Prometheus Endpoint Not Working

- Verify `expose-prometheus: true`
- Check `management.prometheus.metrics.export.enabled: true`
- Ensure `/actuator/prometheus` is in exposed endpoints

### Health Check Shows DOWN

- Check application logs for errors
- Verify database connectivity (if applicable)
- Check disk space availability

## Additional Resources

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Documentation](https://micrometer.io/docs)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)

