package com.quickbite.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.entity.AdminUser;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.repository.AdminUserRepository;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock private AdminUserRepository adminUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserStatusSupport userStatusSupport;

    @InjectMocks
    private AdminAuthService adminAuthService;

    private AdminUser testAdmin;

    @BeforeEach
    void setUp() {
        testAdmin = new AdminUser();
        testAdmin.setAdminId(1L);
        testAdmin.setEmail("admin@quickbite.com");
        testAdmin.setPasswordHash("hashedPass");
        testAdmin.setStatus(UserStatus.ACTIVE);
        testAdmin.setIsActive(true);
    }

    @Test
    @DisplayName("Login - Success")
    void login_Success() {
        // Arrange
        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.of(testAdmin));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("mock-jwt-token");

        // Act
        ResponseDto response = adminAuthService.login("admin@quickbite.com", "password123");

        // Assert
        assertNotNull(response);
        assertEquals("Admin login successful", response.getMessage());
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("ADMIN", response.getRole());
        assertEquals(1L, response.getUserId());

        verify(userStatusSupport).ensureActive(UserStatus.ACTIVE, "Admin account");
    }

    @Test
    @DisplayName("Login - Admin Not Found")
    void login_AdminNotFound() {
        // Arrange
        when(adminUserRepository.findByEmail("wrong@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                adminAuthService.login("wrong@test.com", "password123")
        );

        assertEquals("Admin account not found", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Login - Invalid Credentials")
    void login_InvalidCredentials() {
        // Arrange
        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.of(testAdmin));
        when(userStatusSupport.resolve(any(), any())).thenReturn(UserStatus.ACTIVE);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                adminAuthService.login("admin@quickbite.com", "wrongPassword")
        );

        assertEquals("Invalid credentials", exception.getMessage());
        verify(jwtService, never()).generateToken(anyString(), anyString(), anyLong());
    }

    @Test
    @DisplayName("Login - Account Inactive/Suspended")
    void login_AccountInactive() {
        // Arrange
        testAdmin.setIsActive(false);
        testAdmin.setStatus(UserStatus.SUSPENDED);

        when(adminUserRepository.findByEmail(anyString())).thenReturn(Optional.of(testAdmin));
        when(userStatusSupport.resolve(UserStatus.SUSPENDED, false)).thenReturn(UserStatus.SUSPENDED);

        // Mocking ensureActive to throw exception since the account is suspended
        doThrow(new RuntimeException("Account is not active"))
                .when(userStatusSupport).ensureActive(UserStatus.SUSPENDED, "Admin account");

        // Act & Assert
        assertThrows(RuntimeException.class, () ->
                adminAuthService.login("admin@quickbite.com", "password123")
        );

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}