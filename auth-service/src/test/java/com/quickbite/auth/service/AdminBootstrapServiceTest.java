package com.quickbite.auth.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickbite.auth.entity.AdminUser;
import com.quickbite.auth.repository.AdminUserRepository;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    @Mock private AdminUserRepository adminUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private AdminBootstrapService adminBootstrapService;

    @BeforeEach
    void setUp() {
        // Manually injecting @Value fields using ReflectionTestUtils
        ReflectionTestUtils.setField(adminBootstrapService, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(adminBootstrapService, "adminFullName", "QuickBite Admin");
        ReflectionTestUtils.setField(adminBootstrapService, "adminEmail", "admin@quickbite.local");
        ReflectionTestUtils.setField(adminBootstrapService, "adminPassword", "Admin@12345");
    }

    @Test
    @DisplayName("Run - Should Create Admin when not exists")
    void run_CreateAdminWhenNotExists() throws Exception {
        // Arrange
        when(adminUserRepository.existsByEmail("admin@quickbite.local")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        // Act
        adminBootstrapService.run();

        // Assert
        verify(adminUserRepository, times(1)).save(any(AdminUser.class));
        verify(emailService, times(1)).sendUserCreatedEmail(any(), anyString(), anyString(), eq("ADMIN"));
    }

    @Test
    @DisplayName("Run - Should Skip when Admin Email already exists")
    void run_SkipWhenAdminExists() throws Exception {
        // Arrange
        when(adminUserRepository.existsByEmail("admin@quickbite.local")).thenReturn(true);

        // Act
        adminBootstrapService.run();

        // Assert
        verify(adminUserRepository, never()).save(any(AdminUser.class));
        verify(emailService, never()).sendUserCreatedEmail(any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Run - Should Skip when Bootstrap is Disabled")
    void run_SkipWhenDisabled() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(adminBootstrapService, "bootstrapEnabled", false);

        // Act
        adminBootstrapService.run();

        // Assert
        verify(adminUserRepository, never()).existsByEmail(anyString());
        verify(adminUserRepository, never()).save(any(AdminUser.class));
    }

    @Test
    @DisplayName("Run - Should Skip when Bootstrap Password is Missing")
    void run_SkipWhenPasswordMissing() throws Exception {
        ReflectionTestUtils.setField(adminBootstrapService, "adminPassword", "   ");
        when(adminUserRepository.existsByEmail("admin@quickbite.local")).thenReturn(false);

        adminBootstrapService.run();

        verify(passwordEncoder, never()).encode(anyString());
        verify(adminUserRepository, never()).save(any(AdminUser.class));
        verify(emailService, never()).sendUserCreatedEmail(any(), anyString(), anyString(), anyString());
    }
}
