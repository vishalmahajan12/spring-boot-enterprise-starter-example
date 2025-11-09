package com.yourcompany.starter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods for performance monitoring.
 * 
 * Methods annotated with this will have their execution time measured
 * and metrics recorded to Micrometer.
 * 
 * Usage:
 * @MonitorPerformance
 * public void myMethod() { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitorPerformance {
}

