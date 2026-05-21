package com.quickbite.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.auth.dto.ForgetPasswordRequestDto;
import com.quickbite.auth.dto.OtpVerificationRequestDto;
import com.quickbite.auth.dto.ResetPasswordRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.service.CustomerAuthServiceImpl;
import com.quickbite.auth.service.CustomUserDetailsService;
import com.quickbite.auth.service.DeliveryPartnerAuthServiceImpl;
import com.quickbite.auth.service.JwtService;
import com.quickbite.auth.service.RestaurantOwnerAuthServiceImpl;
import com.quickbite.auth.service.TokenBlacklistService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PasswordResetController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordResetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerAuthServiceImpl customerAuthService;

    @MockBean
    private DeliveryPartnerAuthServiceImpl deliveryPartnerAuthService;

    @MockBean
    private RestaurantOwnerAuthServiceImpl restaurantOwnerAuthService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("POST /auth/customer/forget-password - Success")
    void customerForgetPassword_Success() throws Exception {
        ForgetPasswordRequestDto request = new ForgetPasswordRequestDto();
        request.setEmail("customer@test.com");

        when(customerAuthService.forgetPassword(anyString()))
                .thenReturn(new ResponseDto("OTP Sent Successfully", null));

        mockMvc.perform(post("/auth/customer/forget-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP Sent Successfully"));

        verify(customerAuthService).forgetPassword("customer@test.com");
    }

    @Test
    @DisplayName("POST /auth/delivery-partner/forget-password - Success")
    void deliveryPartnerForgetPassword_Success() throws Exception {
        ForgetPasswordRequestDto request = new ForgetPasswordRequestDto();
        request.setEmail("partner@test.com");

        when(deliveryPartnerAuthService.forgetPassword(anyString()))
                .thenReturn(new ResponseDto("OTP Sent Successfully", null));

        mockMvc.perform(post("/auth/delivery-partner/forget-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP Sent Successfully"));

        verify(deliveryPartnerAuthService).forgetPassword("partner@test.com");
    }

    @Test
    @DisplayName("POST /auth/restaurant/forget-password - Success")
    void restaurantForgetPassword_Success() throws Exception {
        ForgetPasswordRequestDto request = new ForgetPasswordRequestDto();
        request.setEmail("owner@test.com");

        when(restaurantOwnerAuthService.forgetPassword(anyString()))
                .thenReturn(new ResponseDto("OTP Sent Successfully", null));

        mockMvc.perform(post("/auth/restaurant/forget-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP Sent Successfully"));

        verify(restaurantOwnerAuthService).forgetPassword("owner@test.com");
    }

    @Test
    @DisplayName("POST /auth/customer/verify-otp - Success")
    void customerVerifyOtp_Success() throws Exception {
        OtpVerificationRequestDto request = new OtpVerificationRequestDto();
        request.setEmail("customer@test.com");
        request.setOtp("123456");

        when(customerAuthService.verifyOtp(anyString(), anyString()))
                .thenReturn(new ResponseDto("OTP Verified", "CUSTOMER_SESSION"));

        mockMvc.perform(post("/auth/customer/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP Verified"))
                .andExpect(jsonPath("$.token").value("CUSTOMER_SESSION"));

        verify(customerAuthService).verifyOtp("customer@test.com", "123456");
    }

    @Test
    @DisplayName("POST /auth/delivery-partner/verify-otp - Success")
    void deliveryPartnerVerifyOtp_Success() throws Exception {
        OtpVerificationRequestDto request = new OtpVerificationRequestDto();
        request.setEmail("partner@test.com");
        request.setOtp("123456");

        when(deliveryPartnerAuthService.verifyOtp(anyString(), anyString()))
                .thenReturn(new ResponseDto("OTP Verified", "PARTNER_SESSION"));

        mockMvc.perform(post("/auth/delivery-partner/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP Verified"))
                .andExpect(jsonPath("$.token").value("PARTNER_SESSION"));

        verify(deliveryPartnerAuthService).verifyOtp("partner@test.com", "123456");
    }

    @Test
    @DisplayName("POST /auth/restaurant/verify-otp - Success")
    void restaurantVerifyOtp_Success() throws Exception {
        OtpVerificationRequestDto request = new OtpVerificationRequestDto();
        request.setEmail("owner@test.com");
        request.setOtp("123456");

        when(restaurantOwnerAuthService.verifyOtp(anyString(), anyString()))
                .thenReturn(new ResponseDto("OTP Verified", "OWNER_SESSION"));

        mockMvc.perform(post("/auth/restaurant/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP Verified"))
                .andExpect(jsonPath("$.token").value("OWNER_SESSION"));

        verify(restaurantOwnerAuthService).verifyOtp("owner@test.com", "123456");
    }

    @Test
    @DisplayName("POST /auth/customer/reset-password - Success")
    void customerResetPassword_Success() throws Exception {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto();
        request.setEmail("customer@test.com");
        request.setNewPassword("securePass123");
        request.setConfirmPassword("securePass123");

        when(customerAuthService.resetPassword(anyString(), anyString()))
                .thenReturn(new ResponseDto("Password Reset Success", null));

        mockMvc.perform(post("/auth/customer/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password Reset Success"));

        verify(customerAuthService).resetPassword("customer@test.com", "securePass123");
    }

    @Test
    @DisplayName("POST /auth/delivery-partner/reset-password - Success")
    void deliveryPartnerResetPassword_Success() throws Exception {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto();
        request.setEmail("partner@test.com");
        request.setNewPassword("securePass123");
        request.setConfirmPassword("securePass123");

        when(deliveryPartnerAuthService.resetPassword(anyString(), anyString()))
                .thenReturn(new ResponseDto("Password Reset Success", null));

        mockMvc.perform(post("/auth/delivery-partner/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password Reset Success"));

        verify(deliveryPartnerAuthService).resetPassword("partner@test.com", "securePass123");
    }

    @Test
    @DisplayName("POST /auth/restaurant/reset-password - Success")
    void restaurantResetPassword_Success() throws Exception {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto();
        request.setEmail("owner@test.com");
        request.setNewPassword("securePass123");
        request.setConfirmPassword("securePass123");

        when(restaurantOwnerAuthService.resetPassword(anyString(), anyString()))
                .thenReturn(new ResponseDto("Password Reset Success", null));

        mockMvc.perform(post("/auth/restaurant/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password Reset Success"));

        verify(restaurantOwnerAuthService).resetPassword("owner@test.com", "securePass123");
    }

    @Test
    @DisplayName("POST /auth/customer/reset-password - Password Mismatch")
    void customerResetPassword_Mismatch_ShouldReturnBadRequest() throws Exception {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto();
        request.setEmail("customer@test.com");
        request.setNewPassword("password123");
        request.setConfirmPassword("differentPass");

        mockMvc.perform(post("/auth/customer/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password and confirm password do not match"));

        verify(customerAuthService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /auth/delivery-partner/reset-password - Password Mismatch")
    void deliveryPartnerResetPassword_Mismatch_ShouldReturnBadRequest() throws Exception {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto();
        request.setEmail("partner@test.com");
        request.setNewPassword("password123");
        request.setConfirmPassword("differentPass");

        mockMvc.perform(post("/auth/delivery-partner/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password and confirm password do not match"));

        verify(deliveryPartnerAuthService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /auth/restaurant/reset-password - Password Mismatch")
    void restaurantResetPassword_Mismatch_ShouldReturnBadRequest() throws Exception {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto();
        request.setEmail("owner@test.com");
        request.setNewPassword("password123");
        request.setConfirmPassword("differentPass");

        mockMvc.perform(post("/auth/restaurant/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password and confirm password do not match"));

        verify(restaurantOwnerAuthService, never()).resetPassword(anyString(), anyString());
    }
}
