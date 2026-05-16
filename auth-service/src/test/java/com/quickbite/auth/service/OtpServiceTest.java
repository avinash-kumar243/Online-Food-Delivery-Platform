package com.quickbite.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @InjectMocks
    private OtpService otpService;

    private final String testEmail = "avinash@example.com";

    @BeforeEach
    void setUp() {
        // OtpService is initialized via @InjectMocks
    }

    @Test
    @DisplayName("Generate OTP - Should return 6-digit string and store it")
    void generateOtp_ShouldReturnSixDigitString() {
        String otp = otpService.generateOtp(testEmail);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        assertTrue(otp.matches("\\d{6}"), "OTP should only contain digits");
    }

    @Test
    @DisplayName("Verify OTP - Should return VALID for matching code")
    void verifyOtp_Success() {
        // Arrange
        String generatedOtp = otpService.generateOtp(testEmail);

        // Act
        OtpService.OtpVerificationResult result = otpService.verifyOtp(testEmail, generatedOtp);

        // Assert
        assertEquals(OtpService.OtpVerificationResult.VALID, result);
    }

    @Test
    @DisplayName("Verify OTP - Should return INVALID for mismatched code")
    void verifyOtp_InvalidCode() {
        // Arrange
        otpService.generateOtp(testEmail);
        String wrongOtp = "999999";

        // Act
        OtpService.OtpVerificationResult result = otpService.verifyOtp(testEmail, wrongOtp);

        // Assert
        assertEquals(OtpService.OtpVerificationResult.INVALID, result);
    }

    @Test
    @DisplayName("Verify OTP - Should return EXPIRED if no OTP exists for email")
    void verifyOtp_NoEntryFound() {
        // Act
        OtpService.OtpVerificationResult result = otpService.verifyOtp("unknown@example.com", "123456");

        // Assert
        assertEquals(OtpService.OtpVerificationResult.EXPIRED, result);
    }

    @Test
    @DisplayName("Verify OTP - Should be one-time use (EXPIRED on second attempt)")
    void verifyOtp_SingleUseOnly() {
        String otp = otpService.generateOtp(testEmail);

        assertEquals(OtpService.OtpVerificationResult.VALID, otpService.verifyOtp(testEmail, otp));

        assertEquals(OtpService.OtpVerificationResult.EXPIRED, otpService.verifyOtp(testEmail, otp));
    }
}