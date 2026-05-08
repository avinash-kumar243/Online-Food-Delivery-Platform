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

import com.quickbite.auth.dto.*;
import com.quickbite.auth.entity.Customer;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.exception.AccountNotFoundException;
import com.quickbite.auth.exception.PasswordNotMatchException;
import com.quickbite.auth.repository.CustomerRepository;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceImplTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private EmailService emailService;
    @Mock private OtpService otpService;
    @Mock private UserStatusSupport userStatusSupport;

    @InjectMocks
    private CustomerAuthServiceImpl customerAuthService;

    private Customer testCustomer;
    private RegisterRequestDto registerDto;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setCustomerId(1L);
        testCustomer.setEmail("test@gmail.com");
        testCustomer.setPasswordHash("encodedPass");
        testCustomer.setFullName("Avinash Kumar");
        testCustomer.setIsActive(true);
        testCustomer.setStatus(UserStatus.ACTIVE);

        registerDto = new RegisterRequestDto("Avinash Kumar", "test@gmail.com", "password", "9876543210");
    }

    // --- REGISTER TESTS ---

    @Test
    @DisplayName("Register - Success")
    void register_Success() {
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPass");
        when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn("mock-token");

        ResponseDto response = customerAuthService.register(registerDto);

        assertNotNull(response);
        assertEquals("Customer registration successful", response.getMessage());
        verify(customerRepository, times(1)).save(any(Customer.class));
        verify(emailService).sendUserCreatedEmail(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Register - Throws Exception when Email Exists")
    void register_EmailExists() {
        when(customerRepository.existsByEmail(anyString())).thenReturn(true);
        assertThrows(RuntimeException.class, () -> customerAuthService.register(registerDto));
    }

    @Test
    @DisplayName("Register - Throws Exception when Phone Exists")
    void register_PhoneExists() {
        when(customerRepository.existsByEmail(anyString())).thenReturn(false);
        when(customerRepository.existsByPhone(anyString())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> customerAuthService.register(registerDto));
    }

    // --- LOGIN TESTS ---

    @Test
    @DisplayName("Login - Success")
    void login_Success() {
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.of(testCustomer));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyString(), anyLong())).thenReturn("token");

        ResponseDto response = customerAuthService.login("test@gmail.com", "password");

        assertEquals("Customer login successful", response.getMessage());
        verify(userStatusSupport).ensureActive(any(), anyString());
    }

    @Test
    @DisplayName("Login - Wrong Password Throws Exception")
    void login_WrongPassword() {
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.of(testCustomer));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(PasswordNotMatchException.class, () -> customerAuthService.login("test@gmail.com", "wrong"));
    }

    // --- REFRESH TOKEN TESTS ---

    @Test
    @DisplayName("Refresh Token - Success")
    void refreshToken_Success() {
        String oldToken = "old-token";
        when(tokenBlacklistService.isBlacklisted(oldToken)).thenReturn(false);
        when(jwtService.extractEmailFromToken(oldToken)).thenReturn("test@gmail.com");
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.of(testCustomer));
        when(jwtService.validateToken(anyString(), any())).thenReturn(true);
        when(jwtService.generateToken(anyString(), anyString(), any())).thenReturn("new-token");

        ResponseDto response = customerAuthService.refreshToken(oldToken);

        assertTrue(response.getMessage().contains("New Token"));
    }

    @Test
    @DisplayName("Refresh Token - Throws Exception if Blacklisted")
    void refreshToken_Blacklisted() {
        when(tokenBlacklistService.isBlacklisted("black-token")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> customerAuthService.refreshToken("black-token"));
    }

    @Test
    @DisplayName("Refresh Token - Throws Exception if Token Invalid")
    void refreshToken_InvalidToken() {
        String token = "old-token";
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);
        when(jwtService.extractEmailFromToken(token)).thenReturn("test@gmail.com");
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.of(testCustomer));
        when(jwtService.validateToken(anyString(), any())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> customerAuthService.refreshToken(token));
    }

    @Test
    @DisplayName("Get Profile - Success")
    void getProfile_Success() {
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(testCustomer));

        CustomerProfileDto response = customerAuthService.getProfile(1L);

        assertEquals(testCustomer.getEmail(), response.getEmail());
    }

    @Test
    @DisplayName("Update Profile Picture - Success")
    void updateProfilePic_Success() {
        CustomerUpdateProfileDto dto = new CustomerUpdateProfileDto();
        dto.setProfilePicUrl("http://image");
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(testCustomer));

        CustomerProfileDto response = customerAuthService.updateProfilePic(1L, dto);

        assertEquals("http://image", response.getProfilePicUrl());
        verify(customerRepository).save(testCustomer);
    }

    // --- PROFILE & PASSWORD TESTS ---

    @Test
    @DisplayName("Change Password - Success")
    void changePassword_Success() {
        PasswordChangeRequestDto dto = new PasswordChangeRequestDto("old", "new", "new");
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(testCustomer));
        when(passwordEncoder.matches("old", testCustomer.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("newHashed");

        ResponseDto response = customerAuthService.changePassword(1L, dto);

        assertEquals("Password changed successfully", response.getMessage());
        verify(customerRepository).save(testCustomer);
    }

    @Test
    @DisplayName("Change Password - Mismatch Confirm Password Throws Exception")
    void changePassword_Mismatch() {
        PasswordChangeRequestDto dto = new PasswordChangeRequestDto("old", "new", "different");
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(testCustomer));
        when(passwordEncoder.matches("old", testCustomer.getPasswordHash())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> customerAuthService.changePassword(1L, dto));
    }

    @Test
    @DisplayName("Change Password - Wrong Old Password Throws Exception")
    void changePassword_WrongOldPassword() {
        PasswordChangeRequestDto dto = new PasswordChangeRequestDto("old", "new", "new");
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(testCustomer));
        when(passwordEncoder.matches("old", testCustomer.getPasswordHash())).thenReturn(false);

        assertThrows(PasswordNotMatchException.class, () -> customerAuthService.changePassword(1L, dto));
    }

    // --- OTP & RESET TESTS ---

    @Test
    @DisplayName("Forget Password - Success")
    void forgetPassword_Success() {
        when(customerRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testCustomer));
        when(otpService.generateOtp("test@gmail.com")).thenReturn("1234");

        ResponseDto response = customerAuthService.forgetPassword("test@gmail.com");

        assertEquals("OTP sent to your email", response.getMessage());
        verify(emailService).sendOtpEmail("test@gmail.com", "1234");
    }

    @Test
    @DisplayName("Verify OTP - Success")
    void verifyOtp_Success() {
        when(otpService.verifyOtp("test@gmail.com", "1234"))
                .thenReturn(OtpService.OtpVerificationResult.VALID);

        ResponseDto response = customerAuthService.verifyOtp("test@gmail.com", "1234");

        assertEquals("OTP verified successfully", response.getMessage());
    }

    @Test
    @DisplayName("Verify OTP - Invalid")
    void verifyOtp_Invalid() {
        when(otpService.verifyOtp("test@gmail.com", "1234"))
                .thenReturn(OtpService.OtpVerificationResult.INVALID);

        assertThrows(RuntimeException.class, () -> customerAuthService.verifyOtp("test@gmail.com", "1234"));
    }

    @Test
    @DisplayName("Verify OTP - Expired")
    void verifyOtp_Expired() {
        when(otpService.verifyOtp("test@gmail.com", "1234"))
                .thenReturn(OtpService.OtpVerificationResult.EXPIRED);

        assertThrows(RuntimeException.class, () -> customerAuthService.verifyOtp("test@gmail.com", "1234"));
    }

    @Test
    @DisplayName("Reset Password - Success")
    void resetPassword_Success() {
        when(customerRepository.findByEmail("test@gmail.com")).thenReturn(Optional.of(testCustomer));
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");

        ResponseDto response = customerAuthService.resetPassword("test@gmail.com", "newPassword");

        assertEquals("Password reset successfully", response.getMessage());
        verify(customerRepository).save(testCustomer);
    }

    @Test
    @DisplayName("Deactivate Account - Success")
    void deactivate_Success() {
        when(customerRepository.findByCustomerId(1L)).thenReturn(Optional.of(testCustomer));

        ResponseDto response = customerAuthService.deactivateAccount(1L);

        assertFalse(testCustomer.getIsActive());
        assertEquals(UserStatus.SUSPENDED, testCustomer.getStatus());
        verify(emailService).sendUserSuspendedEmail(any(), any(), any(), any());
    }
}
