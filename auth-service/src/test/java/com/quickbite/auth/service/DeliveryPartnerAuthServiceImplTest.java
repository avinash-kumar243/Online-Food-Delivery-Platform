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
import com.quickbite.auth.entity.DeliveryPartner;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.exception.AccountNotFoundException;
import com.quickbite.auth.exception.PasswordNotMatchException;
import com.quickbite.auth.repository.DeliveryPartnerRepository;

@ExtendWith(MockitoExtension.class)
class DeliveryPartnerAuthServiceImplTest {

    @Mock private DeliveryPartnerRepository deliveryPartnerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private EmailService emailService;
    @Mock private OtpService otpService;
    @Mock private UserStatusSupport userStatusSupport;

    @InjectMocks
    private DeliveryPartnerAuthServiceImpl deliveryPartnerService;

    private DeliveryPartner testPartner;
    private RegisterRequestDto registerDto;

    @BeforeEach
    void setUp() {
        testPartner = new DeliveryPartner();
        testPartner.setPartnerId(10L);
        testPartner.setEmail("partner@quickbite.com");
        testPartner.setPasswordHash("hashedPass");
        testPartner.setFullName("Ravi Kumar");
        testPartner.setIsActive(true);
        testPartner.setStatus(UserStatus.ACTIVE);

        registerDto = new RegisterRequestDto("Ravi Kumar", "partner@quickbite.com", "pass123", "9988776655");
    }

    @Test
    @DisplayName("Register - Success")
    void register_Success() {
        when(deliveryPartnerRepository.existsByEmail(anyString())).thenReturn(false);
        when(deliveryPartnerRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPass");
        when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn("mock-token");

        ResponseDto response = deliveryPartnerService.register(registerDto);

        assertNotNull(response);
        assertEquals("Delivery partner registration successful", response.getMessage());
        verify(deliveryPartnerRepository).save(any(DeliveryPartner.class));
    }

    @Test
    @DisplayName("Register - Email Already Exists")
    void register_EmailExists() {
        when(deliveryPartnerRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> deliveryPartnerService.register(registerDto));
    }

    @Test
    @DisplayName("Register - Phone Already Exists")
    void register_PhoneExists() {
        when(deliveryPartnerRepository.existsByEmail(anyString())).thenReturn(false);
        when(deliveryPartnerRepository.existsByPhone(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> deliveryPartnerService.register(registerDto));
    }

    @Test
    @DisplayName("Login - Account Not Found Exception")
    void login_NotFound() {
        when(deliveryPartnerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(AccountNotFoundException.class, () -> deliveryPartnerService.login("wrong@test.com", "pass"));
    }

    @Test
    @DisplayName("Login - Success")
    void login_Success() {
        when(deliveryPartnerRepository.findByEmail(anyString())).thenReturn(Optional.of(testPartner));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");

        ResponseDto response = deliveryPartnerService.login("partner@quickbite.com", "pass123");

        assertEquals("Delivery partner login successful", response.getMessage());
        verify(deliveryPartnerRepository).save(testPartner);
    }

    @Test
    @DisplayName("Login - Wrong Password")
    void login_WrongPassword() {
        when(deliveryPartnerRepository.findByEmail(anyString())).thenReturn(Optional.of(testPartner));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(PasswordNotMatchException.class,
                () -> deliveryPartnerService.login("partner@quickbite.com", "wrong"));
    }

    @Test
    @DisplayName("Refresh Token - Success and Token Validation")
    void refreshToken_Success() {
        String token = "valid-token";
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractEmailFromToken(token)).thenReturn("partner@quickbite.com");
        when(deliveryPartnerRepository.findByEmail(anyString())).thenReturn(Optional.of(testPartner));
        when(jwtService.validateToken(anyString(), any())).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn("new-token");

        ResponseDto response = deliveryPartnerService.refreshToken(token);

        // Asserting the token field instead of a non-existent data field
        assertNotNull(response.getToken());
        assertEquals("new-token", response.getToken());

        // If you want to verify the other fields in the full constructor:
        assertEquals("partner@quickbite.com", response.getEmail());
        assertEquals(10L, response.getUserId());

        verify(userStatusSupport).ensureActive(any(), anyString());
    }

    @Test
    @DisplayName("Refresh Token - Blacklisted")
    void refreshToken_Blacklisted() {
        when(tokenBlacklistService.isBlacklisted("blocked-token")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> deliveryPartnerService.refreshToken("blocked-token"));
    }

    @Test
    @DisplayName("Refresh Token - Invalid")
    void refreshToken_Invalid() {
        String token = "invalid-token";
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractEmailFromToken(token)).thenReturn("partner@quickbite.com");
        when(deliveryPartnerRepository.findByEmail(anyString())).thenReturn(Optional.of(testPartner));
        when(jwtService.validateToken(anyString(), any())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> deliveryPartnerService.refreshToken(token));
    }

    @Test
    @DisplayName("Get Profile - Success")
    void getProfile_Success() {
        when(deliveryPartnerRepository.findByPartnerId(10L)).thenReturn(Optional.of(testPartner));

        DeliveryPartnerProfileDto result = deliveryPartnerService.getProfile(10L);

        assertEquals("partner@quickbite.com", result.getEmail());
    }

    @Test
    @DisplayName("Update Profile - Success with all fields")
    void updateProfile_AllFields() {
        DeliveryPartnerUpdateProfileDto updateDto = new DeliveryPartnerUpdateProfileDto();
        updateDto.setVehicleType("Bike");
        updateDto.setVehicleNumber("BR01-1234");
        updateDto.setProfilePicUrl("http://profile");
        updateDto.setLicenseNumber("LIC999");
        updateDto.setIsOnline(true);
        updateDto.setIsVerified(true);

        when(deliveryPartnerRepository.findByPartnerId(10L)).thenReturn(Optional.of(testPartner));

        DeliveryPartnerProfileDto result = deliveryPartnerService.updateProfile(10L, updateDto);

        assertEquals("Bike", testPartner.getVehicleType());
        assertEquals("BR01-1234", testPartner.getVehicleNumber());
        assertEquals("http://profile", testPartner.getProfilePicUrl());
        assertTrue(testPartner.getIsOnline());
        verify(deliveryPartnerRepository).save(testPartner);
    }

    @Test
    @DisplayName("Change Password - New and Confirm Password Mismatch")
    void changePassword_Mismatch() {
        PasswordChangeRequestDto dto = new PasswordChangeRequestDto("old", "new", "wrongConfirm");
        when(deliveryPartnerRepository.findByPartnerId(10L)).thenReturn(Optional.of(testPartner));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> deliveryPartnerService.changePassword(10L, dto));
        assertEquals("New password and confirm password do not match", ex.getMessage());
    }

    @Test
    @DisplayName("Change Password - Wrong Old Password")
    void changePassword_WrongOldPassword() {
        PasswordChangeRequestDto dto = new PasswordChangeRequestDto("old", "new", "new");
        when(deliveryPartnerRepository.findByPartnerId(10L)).thenReturn(Optional.of(testPartner));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(PasswordNotMatchException.class, () -> deliveryPartnerService.changePassword(10L, dto));
    }

    @Test
    @DisplayName("Change Password - Success")
    void changePassword_Success() {
        PasswordChangeRequestDto dto = new PasswordChangeRequestDto("old", "new", "new");
        when(deliveryPartnerRepository.findByPartnerId(10L)).thenReturn(Optional.of(testPartner));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("encodedNew");

        ResponseDto response = deliveryPartnerService.changePassword(10L, dto);

        assertEquals("Password changed successfully", response.getMessage());
        verify(deliveryPartnerRepository).save(testPartner);
    }

    @Test
    @DisplayName("Deactivate Account - Success")
    void deactivate_Success() {
        when(deliveryPartnerRepository.findByPartnerId(10L)).thenReturn(Optional.of(testPartner));

        ResponseDto response = deliveryPartnerService.deactivateAccount(10L);

        assertFalse(testPartner.getIsActive());
        assertEquals(UserStatus.SUSPENDED, testPartner.getStatus());
        verify(emailService).sendUserSuspendedEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Forget Password - Success")
    void forgetPassword_Success() {
        when(deliveryPartnerRepository.findByEmail("partner@quickbite.com")).thenReturn(Optional.of(testPartner));
        when(otpService.generateOtp("partner@quickbite.com")).thenReturn("4567");

        ResponseDto response = deliveryPartnerService.forgetPassword("partner@quickbite.com");

        assertEquals("OTP sent to your email", response.getMessage());
        verify(emailService).sendOtpEmail("partner@quickbite.com", "4567");
    }

    @Test
    @DisplayName("Verify OTP - Invalid Result Branch")
    void verifyOtp_Invalid() {
        when(otpService.verifyOtp(anyString(), anyString()))
                .thenReturn(OtpService.OtpVerificationResult.INVALID);

        assertThrows(RuntimeException.class, () -> deliveryPartnerService.verifyOtp("test@test.com", "0000"));
    }

    @Test
    @DisplayName("Verify OTP - Expired Result Branch")
    void verifyOtp_Expired() {
        when(otpService.verifyOtp(anyString(), anyString()))
                .thenReturn(OtpService.OtpVerificationResult.EXPIRED);

        assertThrows(RuntimeException.class, () -> deliveryPartnerService.verifyOtp("test@test.com", "0000"));
    }

    @Test
    @DisplayName("Verify OTP - Success")
    void verifyOtp_Success() {
        when(otpService.verifyOtp(anyString(), anyString()))
                .thenReturn(OtpService.OtpVerificationResult.VALID);

        ResponseDto response = deliveryPartnerService.verifyOtp("test@test.com", "0000");

        assertEquals("OTP verified successfully", response.getMessage());
    }

    @Test
    @DisplayName("Logout - Blacklists Token")
    void logout_BlacklistsToken() {
        deliveryPartnerService.logout("jwt-token");

        verify(tokenBlacklistService).blacklistToken("jwt-token");
    }

    @Test
    @DisplayName("Reset Password - Success")
    void resetPassword_Success() {
        when(deliveryPartnerRepository.findByEmail(anyString())).thenReturn(Optional.of(testPartner));
        when(passwordEncoder.encode("newPass")).thenReturn("hashedNew");

        ResponseDto response = deliveryPartnerService.resetPassword("partner@quickbite.com", "newPass");

        assertEquals("Password reset successfully", response.getMessage());
        verify(deliveryPartnerRepository).save(testPartner);
    }
}
