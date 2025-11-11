package com.yourcompany.starter.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Main configuration properties class for Enterprise Starter.
 * 
 * This class uses @ConfigurationProperties to bind application properties
 * with prefix "enterprise.starter" to Java objects.
 * 
 * Example usage in application.yml:
 * enterprise:
 *   starter:
 *     logging:
 *       enabled: true
 *     authentication:
 *       enabled: false
 */
@ConfigurationProperties(prefix = "enterprise.starter")
@Validated
public class EnterpriseStarterProperties {

    private Logging logging = new Logging();
    private Authentication authentication = new Authentication();
    private Swagger swagger = new Swagger();
    private Monitoring monitoring = new Monitoring();
    private Cache cache = new Cache();

    // Getters and Setters
    public Logging getLogging() { return logging; }
    public void setLogging(Logging logging) { this.logging = logging; }
    public Authentication getAuthentication() { return authentication; }
    public void setAuthentication(Authentication authentication) { this.authentication = authentication; }
    public Swagger getSwagger() { return swagger; }
    public void setSwagger(Swagger swagger) { this.swagger = swagger; }
    public Monitoring getMonitoring() { return monitoring; }
    public void setMonitoring(Monitoring monitoring) { this.monitoring = monitoring; }
    public Cache getCache() { return cache; }
    public void setCache(Cache cache) { this.cache = cache; }

    /**
     * Logging configuration properties.
     * Controls request/response logging, masking, correlation IDs, etc.
     */
    public static class Logging {
        private boolean enabled = true;
        private String level = "INFO";
        private boolean logRequest = true;
        private boolean logResponse = true;
        private boolean logIncomingRequest = true;
        private boolean logOutgoingRequest = true;
        private boolean logIncomingResponse = true;
        private boolean logOutgoingResponse = true;
        private boolean includeHeaders = true;
        private boolean includeBody = true;
        private boolean maskSensitiveData = true;
        private int maxBodyLength = 10000;
        private List<String> excludedPaths = new ArrayList<>();
        private List<String> sensitiveFields = List.of("password", "token", "secret", "authorization", 
                                                       "creditCard", "ssn", "accountNumber");
        private boolean asyncLogging = true;
        private String correlationIdHeader = "X-Correlation-ID";
        private boolean includeServiceName = true;
        private boolean includeConsumerDetails = true;

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        public boolean isLogRequest() { return logRequest; }
        public void setLogRequest(boolean logRequest) { this.logRequest = logRequest; }
        public boolean isLogResponse() { return logResponse; }
        public void setLogResponse(boolean logResponse) { this.logResponse = logResponse; }
        public boolean isLogIncomingRequest() { return logIncomingRequest; }
        public void setLogIncomingRequest(boolean logIncomingRequest) { this.logIncomingRequest = logIncomingRequest; }
        public boolean isLogOutgoingRequest() { return logOutgoingRequest; }
        public void setLogOutgoingRequest(boolean logOutgoingRequest) { this.logOutgoingRequest = logOutgoingRequest; }
        public boolean isLogIncomingResponse() { return logIncomingResponse; }
        public void setLogIncomingResponse(boolean logIncomingResponse) { this.logIncomingResponse = logIncomingResponse; }
        public boolean isLogOutgoingResponse() { return logOutgoingResponse; }
        public void setLogOutgoingResponse(boolean logOutgoingResponse) { this.logOutgoingResponse = logOutgoingResponse; }
        public boolean isIncludeHeaders() { return includeHeaders; }
        public void setIncludeHeaders(boolean includeHeaders) { this.includeHeaders = includeHeaders; }
        public boolean isIncludeBody() { return includeBody; }
        public void setIncludeBody(boolean includeBody) { this.includeBody = includeBody; }
        public boolean isMaskSensitiveData() { return maskSensitiveData; }
        public void setMaskSensitiveData(boolean maskSensitiveData) { this.maskSensitiveData = maskSensitiveData; }
        public int getMaxBodyLength() { return maxBodyLength; }
        public void setMaxBodyLength(int maxBodyLength) { this.maxBodyLength = maxBodyLength; }
        public List<String> getExcludedPaths() { return excludedPaths; }
        public void setExcludedPaths(List<String> excludedPaths) { this.excludedPaths = excludedPaths; }
        public List<String> getSensitiveFields() { return sensitiveFields; }
        public void setSensitiveFields(List<String> sensitiveFields) { this.sensitiveFields = sensitiveFields; }
        public boolean isAsyncLogging() { return asyncLogging; }
        public void setAsyncLogging(boolean asyncLogging) { this.asyncLogging = asyncLogging; }
        public String getCorrelationIdHeader() { return correlationIdHeader; }
        public void setCorrelationIdHeader(String correlationIdHeader) { this.correlationIdHeader = correlationIdHeader; }
        public boolean isIncludeServiceName() { return includeServiceName; }
        public void setIncludeServiceName(boolean includeServiceName) { this.includeServiceName = includeServiceName; }
        public boolean isIncludeConsumerDetails() { return includeConsumerDetails; }
        public void setIncludeConsumerDetails(boolean includeConsumerDetails) { this.includeConsumerDetails = includeConsumerDetails; }
    }

    /**
     * Authentication configuration properties.
     * Supports multiple auth types: JWT, OAuth2, API Key, Basic Auth, Custom
     */
    public static class Authentication {
        private boolean enabled = false;
        private AuthType type = AuthType.JWT;
        private String secretKey;
        private long tokenValiditySeconds = 3600;
        private List<String> excludedPaths = new ArrayList<>();
        private Map<String, List<String>> operationRules = Map.of();
        private boolean enableRateLimiting = false;
        private int rateLimitPerMinute = 100;
        private boolean enableAuditLogging = true;
        private List<String> allowedRoles = new ArrayList<>();
        private boolean enableIpWhitelist = false;
        private List<String> ipWhitelist = new ArrayList<>();
        private boolean enableSecurityHeaders = true;
        
        // API Key configuration
        private Map<String, List<String>> apiKeys = Map.of(); // apiKey -> [roles]
        private String apiKeyHeader = "X-API-Key";
        
        // OAuth2 configuration
        private String oauth2IntrospectionUrl;
        private String oauth2ClientId;
        private String oauth2ClientSecret;
        private boolean oauth2ValidateTokenEndpoint = true;
        
        // Basic Auth configuration
        private Map<String, String> basicAuthUsers = Map.of(); // username -> password (hashed in production)
        private boolean basicAuthValidateAgainstFile = false;
        private String basicAuthPasswordFile;

        public enum AuthType {
            JWT, OAUTH2, API_KEY, BASIC, CUSTOM
        }

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public AuthType getType() { return type; }
        public void setType(AuthType type) { this.type = type; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public long getTokenValiditySeconds() { return tokenValiditySeconds; }
        public void setTokenValiditySeconds(long tokenValiditySeconds) { this.tokenValiditySeconds = tokenValiditySeconds; }
        public List<String> getExcludedPaths() { return excludedPaths; }
        public void setExcludedPaths(List<String> excludedPaths) { this.excludedPaths = excludedPaths; }
        public Map<String, List<String>> getOperationRules() { return operationRules; }
        public void setOperationRules(Map<String, List<String>> operationRules) { this.operationRules = operationRules; }
        public boolean isEnableRateLimiting() { return enableRateLimiting; }
        public void setEnableRateLimiting(boolean enableRateLimiting) { this.enableRateLimiting = enableRateLimiting; }
        public int getRateLimitPerMinute() { return rateLimitPerMinute; }
        public void setRateLimitPerMinute(int rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }
        public boolean isEnableAuditLogging() { return enableAuditLogging; }
        public void setEnableAuditLogging(boolean enableAuditLogging) { this.enableAuditLogging = enableAuditLogging; }
        public List<String> getAllowedRoles() { return allowedRoles; }
        public void setAllowedRoles(List<String> allowedRoles) { this.allowedRoles = allowedRoles; }
        public boolean isEnableIpWhitelist() { return enableIpWhitelist; }
        public void setEnableIpWhitelist(boolean enableIpWhitelist) { this.enableIpWhitelist = enableIpWhitelist; }
        public List<String> getIpWhitelist() { return ipWhitelist; }
        public void setIpWhitelist(List<String> ipWhitelist) { this.ipWhitelist = ipWhitelist; }
        public boolean isEnableSecurityHeaders() { return enableSecurityHeaders; }
        public void setEnableSecurityHeaders(boolean enableSecurityHeaders) { this.enableSecurityHeaders = enableSecurityHeaders; }
        public Map<String, List<String>> getApiKeys() { return apiKeys; }
        public void setApiKeys(Map<String, List<String>> apiKeys) { this.apiKeys = apiKeys; }
        public String getApiKeyHeader() { return apiKeyHeader; }
        public void setApiKeyHeader(String apiKeyHeader) { this.apiKeyHeader = apiKeyHeader; }
        public String getOauth2IntrospectionUrl() { return oauth2IntrospectionUrl; }
        public void setOauth2IntrospectionUrl(String oauth2IntrospectionUrl) { this.oauth2IntrospectionUrl = oauth2IntrospectionUrl; }
        public String getOauth2ClientId() { return oauth2ClientId; }
        public void setOauth2ClientId(String oauth2ClientId) { this.oauth2ClientId = oauth2ClientId; }
        public String getOauth2ClientSecret() { return oauth2ClientSecret; }
        public void setOauth2ClientSecret(String oauth2ClientSecret) { this.oauth2ClientSecret = oauth2ClientSecret; }
        public boolean isOauth2ValidateTokenEndpoint() { return oauth2ValidateTokenEndpoint; }
        public void setOauth2ValidateTokenEndpoint(boolean oauth2ValidateTokenEndpoint) { this.oauth2ValidateTokenEndpoint = oauth2ValidateTokenEndpoint; }
        public Map<String, String> getBasicAuthUsers() { return basicAuthUsers; }
        public void setBasicAuthUsers(Map<String, String> basicAuthUsers) { this.basicAuthUsers = basicAuthUsers; }
        public boolean isBasicAuthValidateAgainstFile() { return basicAuthValidateAgainstFile; }
        public void setBasicAuthValidateAgainstFile(boolean basicAuthValidateAgainstFile) { this.basicAuthValidateAgainstFile = basicAuthValidateAgainstFile; }
        public String getBasicAuthPasswordFile() { return basicAuthPasswordFile; }
        public void setBasicAuthPasswordFile(String basicAuthPasswordFile) { this.basicAuthPasswordFile = basicAuthPasswordFile; }
    }

    /**
     * Swagger/OpenAPI configuration properties.
     * Controls API documentation generation and UI
     */
    public static class Swagger {
        private boolean enabled = true;
        private String title = "API Documentation";
        private String description = "API Documentation";
        private String version = "1.0.0";
        private String contactName;
        private String contactEmail;
        private String contactUrl;
        private List<String> excludePaths = new ArrayList<>();
        private boolean enableSecuritySchemes = true;
        private String apiPath = "/api/**";

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getContactName() { return contactName; }
        public void setContactName(String contactName) { this.contactName = contactName; }
        public String getContactEmail() { return contactEmail; }
        public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
        public String getContactUrl() { return contactUrl; }
        public void setContactUrl(String contactUrl) { this.contactUrl = contactUrl; }
        public List<String> getExcludePaths() { return excludePaths; }
        public void setExcludePaths(List<String> excludePaths) { this.excludePaths = excludePaths; }
        public boolean isEnableSecuritySchemes() { return enableSecuritySchemes; }
        public void setEnableSecuritySchemes(boolean enableSecuritySchemes) { this.enableSecuritySchemes = enableSecuritySchemes; }
        public String getApiPath() { return apiPath; }
        public void setApiPath(String apiPath) { this.apiPath = apiPath; }
    }

    /**
     * Monitoring configuration properties.
     * Controls metrics, health checks, tracing, and performance monitoring
     */
    public static class Monitoring {
        private boolean enabled = true;
        private boolean enableMetrics = true;
        private boolean enableHealthChecks = true;
        private boolean enableTracing = false;
        private boolean enableSlowQueryDetection = true;
        private long slowQueryThresholdMs = 1000;
        private boolean enableErrorTracking = true;
        private List<String> customHealthIndicators = new ArrayList<>();

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isEnableMetrics() { return enableMetrics; }
        public void setEnableMetrics(boolean enableMetrics) { this.enableMetrics = enableMetrics; }
        public boolean isEnableHealthChecks() { return enableHealthChecks; }
        public void setEnableHealthChecks(boolean enableHealthChecks) { this.enableHealthChecks = enableHealthChecks; }
        public boolean isEnableTracing() { return enableTracing; }
        public void setEnableTracing(boolean enableTracing) { this.enableTracing = enableTracing; }
        public boolean isEnableSlowQueryDetection() { return enableSlowQueryDetection; }
        public void setEnableSlowQueryDetection(boolean enableSlowQueryDetection) { this.enableSlowQueryDetection = enableSlowQueryDetection; }
        public long getSlowQueryThresholdMs() { return slowQueryThresholdMs; }
        public void setSlowQueryThresholdMs(long slowQueryThresholdMs) { this.slowQueryThresholdMs = slowQueryThresholdMs; }
        public boolean isEnableErrorTracking() { return enableErrorTracking; }
        public void setEnableErrorTracking(boolean enableErrorTracking) { this.enableErrorTracking = enableErrorTracking; }
        public List<String> getCustomHealthIndicators() { return customHealthIndicators; }
        public void setCustomHealthIndicators(List<String> customHealthIndicators) { this.customHealthIndicators = customHealthIndicators; }
    }

    /**
     * Cache configuration properties.
     * Supports multi-level caching with Caffeine (L1) and Redis (L2).
     * Controls caching behavior, TTL, size limits, etc.
     */
    public static class Cache {
        private boolean enabled = false;
        
        // Caffeine (L1) Configuration - Fast local in-memory cache
        private long caffeineMaxSize = 10000;
        private long caffeineTtlSeconds = 300; // 5 minutes - shorter for L1
        private long caffeineAccessExpirationSeconds = 180; // 3 minutes
        
        // Redis (L2) Configuration - Distributed cache
        private boolean redisEnabled = true;
        private long redisTtlSeconds = 3600; // 1 hour - longer for L2
        
        // Cache names
        private List<String> cacheNames = List.of("default", "users", "config", "tokens", "api-responses");

        // Getters and Setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public long getCaffeineMaxSize() { return caffeineMaxSize; }
        public void setCaffeineMaxSize(long caffeineMaxSize) { this.caffeineMaxSize = caffeineMaxSize; }
        
        public long getCaffeineTtlSeconds() { return caffeineTtlSeconds; }
        public void setCaffeineTtlSeconds(long caffeineTtlSeconds) { this.caffeineTtlSeconds = caffeineTtlSeconds; }
        
        public long getCaffeineAccessExpirationSeconds() { return caffeineAccessExpirationSeconds; }
        public void setCaffeineAccessExpirationSeconds(long caffeineAccessExpirationSeconds) {
            this.caffeineAccessExpirationSeconds = caffeineAccessExpirationSeconds;
        }
        
        public boolean isRedisEnabled() { return redisEnabled; }
        public void setRedisEnabled(boolean redisEnabled) { this.redisEnabled = redisEnabled; }
        
        public long getRedisTtlSeconds() { return redisTtlSeconds; }
        public void setRedisTtlSeconds(long redisTtlSeconds) { this.redisTtlSeconds = redisTtlSeconds; }
        
        public List<String> getCacheNames() { return cacheNames; }
        public void setCacheNames(List<String> cacheNames) { this.cacheNames = cacheNames; }
    }
}

