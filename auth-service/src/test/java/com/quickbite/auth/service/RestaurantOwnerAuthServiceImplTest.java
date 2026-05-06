package com.quickbite.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.quickbite.auth.dto.*;
import com.quickbite.auth.entity.RestaurantOwner;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.exception.AccountNotFoundException;
import com.quickbite.auth.exception.PasswordNotMatchException;
import com.quickbite.auth.repository.RestaurantOwnerRepository;

@ExtendWith(MockitoExtension.class)
class RestaurantOwnerAuthServiceImplTest {

    @Mock private RestaurantOwnerRepository restaurantOwnerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private EmailService emailService;
    @Mock private OtpService otpService;
    @Mock private UserStatusSupport userStatusSupport;

    @InjectMocks
    private RestaurantOwnerAuthServiceImpl restaurantOwnerService;

    private RestaurantOwner testOwner;
    private RegisterRequestDto registerDto;

    @BeforeEach
    void setUp() {
        testOwner = new RestaurantOwner();
        testOwner.setOwnerId(100L);
        testOwner.setEmail("owner@test.com");
        testOwner.setPhone("9988776655");
        testOwner.setPasswordHash("hashedPass");
        testOwner.setIsActive(true);
        testOwner.setStatus(UserStatus.ACTIVE);

        registerDto = new RegisterRequestDto("Avinash Owner", "owner@test.com", "pass123", "9988776655");
    }

    @Test
    @DisplayName("Register - Throws Exception if Phone Exists")
    void register_PhoneExists() {
        when(restaurantOwnerRepository.existsByEmail(anyString())).thenReturn(false);
        when(restaurantOwnerRepository.existsByPhone(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> restaurantOwnerService.register(registerDto));
    }

    @Test
    @DisplayName("Login - Success")
    void login_Success() {
        when(restaurantOwnerRepository.findByEmail(anyString())).thenReturn(Optional.of(testOwner));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");

        ResponseDto response = restaurantOwnerService.login("owner@test.com", "pass123");

        assertEquals("Restaurant owner login successful", response.getMessage());
        verify(restaurantOwnerRepository).save(testOwner);
    }

    @Test
    @DisplayName("Update Profile - Success with Phone Change")
    void updateProfile_Success() {
        RestaurantOwnerUpdateProfileDto updateDto = new RestaurantOwnerUpdateProfileDto();
        updateDto.setFullName("New Name");
        updateDto.setPhone("1122334455"); // Different from testOwner.getPhone()
        updateDto.setRestaurantName("Bhopal Express");

        when(restaurantOwnerRepository.findByOwnerId(100L)).thenReturn(Optional.of(testOwner));
        when(restaurantOwnerRepository.existsByPhone("1122334455")).thenReturn(false);

        RestaurantOwnerProfileDto result = restaurantOwnerService.updateProfile(100L, updateDto);

        assertEquals("New Name", testOwner.getFullName());
        assertEquals("Bhopal Express", testOwner.getRestaurantName());
        verify(restaurantOwnerRepository).save(testOwner);
    }

    @Test
    @DisplayName("Update Profile - Throws Exception if New Phone in Use")
    void updateProfile_PhoneInUse() {
        RestaurantOwnerUpdateProfileDto updateDto = new RestaurantOwnerUpdateProfileDto();
        updateDto.setPhone("1122334455");

        when(restaurantOwnerRepository.findByOwnerId(100L)).thenReturn(Optional.of(testOwner));
        when(restaurantOwnerRepository.existsByPhone("1122334455")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> restaurantOwnerService.updateProfile(100L, updateDto));
    }

    @Test
    @DisplayName("Refresh Token - Failure if Token Invalid")
    void refreshToken_InvalidToken() {
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(jwtService.extractEmailFromToken(anyString())).thenReturn("owner@test.com");
        when(restaurantOwnerRepository.findByEmail(anyString())).thenReturn(Optional.of(testOwner));
        when(jwtService.validateToken(anyString(), any())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> restaurantOwnerService.refreshToken("invalid-token"));
    }

    @Test
    @DisplayName("Change Password - Success")
    void changePassword_Success() {
        PasswordChangeRequestDto dto = new PasswordChangeRequestDto("old", "new", "new");
        when(restaurantOwnerRepository.findByOwnerId(100L)).thenReturn(Optional.of(testOwner));
        when(passwordEncoder.matches("old", testOwner.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("newHashed");

        ResponseDto response = restaurantOwnerService.changePassword(100L, dto);

        assertEquals("Password changed successfully", response.getMessage());
        verify(restaurantOwnerRepository).save(testOwner);
    }

    @Test
    @DisplayName("Verify OTP - Exists Check and Success")
    void verifyOtp_Success() {
        when(otpService.verifyOtp("owner@test.com", "9999"))
                .thenReturn(OtpService.OtpVerificationResult.VALID);

        ResponseDto response = restaurantOwnerService.verifyOtp("owner@test.com", "9999");

        assertEquals("OTP verified successfully", response.getMessage());
    }

    @Test
    @DisplayName("Deactivate Account - Success and Email Sent")
    void deactivateAccount_Success() {
        when(restaurantOwnerRepository.findByOwnerId(100L)).thenReturn(Optional.of(testOwner));

        ResponseDto response = restaurantOwnerService.deactivateAccount(100L);

        assertFalse(testOwner.getIsActive());
        assertEquals(UserStatus.SUSPENDED, testOwner.getStatus());
        verify(emailService).sendUserSuspendedEmail(eq(100L), nullable(String.class), anyString(), anyString());
    }

    @Test
    @DisplayName("Reset Password - Account Not Found")
    void resetPassword_NotFound() {
        when(restaurantOwnerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> restaurantOwnerService.resetPassword("none@test.com", "new"));
    }
}