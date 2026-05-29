package com.quickbite.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.quickbite.auth.service.AdminAuthService;
import com.quickbite.auth.service.CustomUserDetailsService;
import com.quickbite.auth.service.JwtService;
import com.quickbite.auth.service.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.auth.dto.*;
import com.quickbite.auth.service.DeliveryPartnerAuthServiceImpl;

@WebMvcTest(DeliveryPartnerAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeliveryPartnerAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeliveryPartnerAuthServiceImpl deliveryPartnerAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private TokenBlacklistService tokenBlacklistService;
    @MockBean private AdminAuthService adminAuthService;

    @Test
    @DisplayName("POST /register - Success")
    void register_Success() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("Partner Name", "partner@test.com", "pass123", "9998887776");
        ResponseDto response = new ResponseDto("Partner Registered", "TOKEN_123");

        when(deliveryPartnerAuthService.register(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/auth/delivery-partner/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Partner Registered"));
    }

    @Test
    @DisplayName("POST /login - Success")
    void login_Success() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("partner@test.com", "pass123");
        ResponseDto response = new ResponseDto("Login Successful", "JWT_TOKEN");

        when(deliveryPartnerAuthService.login(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/auth/delivery-partner/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                // In login_Success
                .andExpect(jsonPath("$.message").value("Login Successful"))
                .andExpect(jsonPath("$.token").value("JWT_TOKEN")); // Changed from $.data
    }

    @Test
    @DisplayName("POST /logout - Success with Bearer")
    void logout_Success() throws Exception {
        mockMvc.perform(post("/auth/delivery-partner/logout")
                        .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Delivery partner logged out successfully"));

        verify(deliveryPartnerAuthService).logout("valid_token");
    }

    @Test
    @DisplayName("POST /logout - Failure Missing/Invalid Header")
    void logout_Failure() throws Exception {
        mockMvc.perform(post("/auth/delivery-partner/logout")
                        .header("Authorization", "InvalidFormat"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or missing Authorization header"));
    }

    @Test
    @DisplayName("POST /refresh - Success")
    void refresh_Success() throws Exception {
        ResponseDto response = new ResponseDto("Refreshed", "NEW_TOKEN");
        when(deliveryPartnerAuthService.refreshToken("old_token")).thenReturn(response);

        mockMvc.perform(post("/auth/delivery-partner/refresh")
                        .header("Authorization", "Bearer old_token"))
                .andExpect(status().isOk())
                // In refresh_Success
                .andExpect(jsonPath("$.message").value("Refreshed"))
                .andExpect(jsonPath("$.token").value("NEW_TOKEN")); // Changed from $.data
    }

    @Test
    @DisplayName("GET /profile/{id} - Success")
    void getProfile_Success() throws Exception {
        DeliveryPartnerProfileDto profile = new DeliveryPartnerProfileDto();
        profile.setFullName("Partner Name");

        when(deliveryPartnerAuthService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/auth/delivery-partner/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Partner Name"));
    }

    @Test
    @DisplayName("PUT /profile/{id} - Success")
    void updateProfile_Success() throws Exception {
        DeliveryPartnerUpdateProfileDto update = new DeliveryPartnerUpdateProfileDto(
                "Partner Name", "9998887776", "Bike", "MH12-1234", "url", "LIC123", true, true
        );
        DeliveryPartnerProfileDto responseProfile = new DeliveryPartnerProfileDto();
        responseProfile.setVehicleNumber("MH12-1234");

        when(deliveryPartnerAuthService.updateProfile(eq(1L), any(DeliveryPartnerUpdateProfileDto.class)))
                .thenReturn(responseProfile);

        mockMvc.perform(put("/auth/delivery-partner/profile/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleNumber").value("MH12-1234"));
    }

    @Test
    @DisplayName("POST /change-password/{id} - Success")
    void changePassword_Success() throws Exception {
        PasswordChangeRequestDto dto = new PasswordChangeRequestDto("old", "new", "new");
        ResponseDto response = new ResponseDto("Password Changed", null);

        when(deliveryPartnerAuthService.changePassword(eq(1L), any(PasswordChangeRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/auth/delivery-partner/change-password/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /deactivate/{id} - Success")
    void deactivateAccount_Success() throws Exception {
        ResponseDto response = new ResponseDto("Account Deactivated", null);
        when(deliveryPartnerAuthService.deactivateAccount(1L)).thenReturn(response);

        mockMvc.perform(post("/auth/delivery-partner/deactivate/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account Deactivated"));
    }
}
