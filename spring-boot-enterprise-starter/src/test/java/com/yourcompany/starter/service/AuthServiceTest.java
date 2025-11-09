package com.yourcompany.starter.service;

import com.yourcompany.starter.config.EnterpriseStarterProperties;
import com.yourcompany.starter.model.AuthContext;
import com.yourcompany.starter.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private EnterpriseStarterProperties properties;

    @Mock
    private JwtUtil jwtUtil;

    private AuthService authService;

    private EnterpriseStarterProperties.Authentication authConfig;

    @BeforeEach
    void setUp() {
        properties = new EnterpriseStarterProperties();
        authConfig = properties.getAuthentication();
        authService = new AuthService(properties, jwtUtil);
    }

    @Test
    void testValidateAuthentication_AuthDisabled() {
        authConfig.setEnabled(false);
        
        AuthContext context = authService.validateAuthentication("token", "127.0.0.1", "operation");
        
        assertTrue(context.isAuthenticated());
    }

    @Test
    void testValidateAuthentication_JWT_ValidToken() {
        authConfig.setEnabled(true);
        authConfig.setType(EnterpriseStarterProperties.Authentication.AuthType.JWT);
        authConfig.setSecretKey("secret-key-123456789012345678901234567890");
        authConfig.setAllowedRoles(List.of());
        authConfig.setEnableIpWhitelist(false);
        authConfig.setOperationRules(Map.of());
        
        when(jwtUtil.validateToken(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.extractUsername(anyString(), anyString())).thenReturn("testuser");
        when(jwtUtil.extractRoles(anyString(), anyString())).thenReturn(List.of("USER"));
        
        AuthContext context = authService.validateAuthentication("valid-token", "127.0.0.1", "operation");
        
        assertTrue(context.isAuthenticated());
        assertEquals("testuser", context.getUsername());
        assertEquals(List.of("USER"), context.getRoles());
    }

    @Test
    void testValidateAuthentication_JWT_InvalidToken() {
        authConfig.setEnabled(true);
        authConfig.setType(EnterpriseStarterProperties.Authentication.AuthType.JWT);
        authConfig.setSecretKey("secret-key");
        authConfig.setEnableAuditLogging(false);
        
        when(jwtUtil.validateToken(anyString(), anyString())).thenReturn(false);
        
        AuthContext context = authService.validateAuthentication("invalid-token", "127.0.0.1", "operation");
        
        assertFalse(context.isAuthenticated());
    }

    @Test
    void testValidateAuthentication_API_KEY_Valid() {
        authConfig.setEnabled(true);
        authConfig.setType(EnterpriseStarterProperties.Authentication.AuthType.API_KEY);
        authConfig.setApiKeys(Map.of("valid-api-key", List.of("USER", "ADMIN")));
        authConfig.setAllowedRoles(List.of());
        authConfig.setEnableIpWhitelist(false);
        authConfig.setOperationRules(Map.of());
        
        AuthContext context = authService.validateAuthentication("valid-api-key", "127.0.0.1", "operation");
        
        assertTrue(context.isAuthenticated());
        assertEquals("api-key-user", context.getUsername());
        assertEquals(List.of("USER", "ADMIN"), context.getRoles());
    }

    @Test
    void testValidateAuthentication_API_KEY_Invalid() {
        authConfig.setEnabled(true);
        authConfig.setType(EnterpriseStarterProperties.Authentication.AuthType.API_KEY);
        authConfig.setApiKeys(Map.of("valid-key", List.of("USER")));
        authConfig.setEnableAuditLogging(false);
        
        AuthContext context = authService.validateAuthentication("invalid-key", "127.0.0.1", "operation");
        
        assertFalse(context.isAuthenticated());
    }

    @Test
    void testValidateAuthentication_BASIC_Valid() {
        authConfig.setEnabled(true);
        authConfig.setType(EnterpriseStarterProperties.Authentication.AuthType.BASIC);
        String credentials = Base64.getEncoder().encodeToString("user:password".getBytes());
        authConfig.setBasicAuthUsers(Map.of("user", "password"));
        authConfig.setAllowedRoles(List.of());
        authConfig.setEnableIpWhitelist(false);
        authConfig.setOperationRules(Map.of());
        
        AuthContext context = authService.validateAuthentication(credentials, "127.0.0.1", "operation");
        
        assertTrue(context.isAuthenticated());
        assertEquals("user", context.getUsername());
        assertEquals(List.of("USER"), context.getRoles());
    }

    @Test
    void testValidateAuthentication_BASIC_InvalidPassword() {
        authConfig.setEnabled(true);
        authConfig.setType(EnterpriseStarterProperties.Authentication.AuthType.BASIC);
        String credentials = Base64.getEncoder().encodeToString("user:wrongpassword".getBytes());
        authConfig.setBasicAuthUsers(Map.of("user", "password"));
        authConfig.setEnableAuditLogging(false);
        
        AuthContext context = authService.validateAuthentication(credentials, "127.0.0.1", "operation");
        
        assertFalse(context.isAuthenticated());
    }

    @Test
    void testValidateAuthentication_IPWhitelist_Blocked() {
        authConfig.setEnabled(true);
        authConfig.setType(EnterpriseStarterProperties.Authentication.AuthType.API_KEY);
        authConfig.setApiKeys(Map.of("key", List.of("USER")));
        authConfig.setEnableIpWhitelist(true);
        authConfig.setIpWhitelist(List.of("192.168.1.1"));
        authConfig.setAllowedRoles(List.of());
        authConfig.setOperationRules(Map.of());
        
        AuthContext context = authService.validateAuthentication("key", "127.0.0.1", "operation");
        
        assertFalse(context.isAuthenticated());
    }

    @Test
    void testValidateAuthentication_RoleBasedAccess_Denied() {
        authConfig.setEnabled(true);
        authConfig.setType(EnterpriseStarterProperties.Authentication.AuthType.API_KEY);
        authConfig.setApiKeys(Map.of("key", List.of("USER")));
        authConfig.setAllowedRoles(List.of("ADMIN")); // Only ADMIN allowed
        authConfig.setEnableIpWhitelist(false);
        authConfig.setOperationRules(Map.of());
        
        AuthContext context = authService.validateAuthentication("key", "127.0.0.1", "operation");
        
        assertFalse(context.isAuthenticated());
    }

    @Test
    void testValidateAuthentication_OperationRules_Denied() {
        authConfig.setEnabled(true);
        authConfig.setType(EnterpriseStarterProperties.Authentication.AuthType.API_KEY);
        authConfig.setApiKeys(Map.of("key", List.of("USER")));
        authConfig.setAllowedRoles(List.of());
        authConfig.setEnableIpWhitelist(false);
        authConfig.setOperationRules(Map.of("/admin/operation", List.of("ADMIN")));
        
        AuthContext context = authService.validateAuthentication("key", "127.0.0.1", "/admin/operation");
        
        assertFalse(context.isAuthenticated());
    }
}

