package com.quickbite.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import com.quickbite.auth.service.AdminAuthService;
import com.quickbite.auth.service.CustomUserDetailsService;
import com.quickbite.auth.service.JwtService;
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
import com.quickbite.auth.service.CustomerAuthServiceImpl;

@WebMvcTest(CustomerAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class CustomerAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerAuthServiceImpl customerAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private AdminAuthService adminAuthService;

    @Test
    @DisplayName("POST /register - Success")
    void register_Success() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("Avinash Kumar", "avi@test.com", "pass123", "9876543210");
        ResponseDto response = new ResponseDto("User Registered Successfully", "MOCK_JWT_TOKEN");

        when(customerAuthService.register(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/auth/customer/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User Registered Successfully"))
                .andExpect(jsonPath("$.token").value("MOCK_JWT_TOKEN")); // Changed from $.data
    }

    @Test
    @DisplayName("GET /profile/{id} - Success")
    void getProfile_Success() throws Exception {
        // Updated to match your CustomerProfileDto fields
        CustomerProfileDto profile = new CustomerProfileDto(
                1L,
                "Avinash Kumar",
                "avi@test.com",
                "9876543210",
                true,
                "http://images.com/profile.jpg",
                LocalDateTime.now()
        );

        when(customerAuthService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/auth/customer/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Avinash Kumar"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.email").value("avi@test.com"));
    }

    @Test
    @DisplayName("PUT /profile/{id} - Success Update Pic")
    void updateProfile_Success() throws Exception {
        CustomerUpdateProfileDto updateDto = new CustomerUpdateProfileDto();
        updateDto.setFullName("Avinash Kumar");
        updateDto.setPhone("9876543210");
        updateDto.setProfilePicUrl("http://images.com/new-pic.jpg");
        CustomerProfileDto updatedProfile = new CustomerProfileDto(
                1L, "Avinash Kumar", "avi@test.com", "9876543210", true, "http://images.com/new-pic.jpg", LocalDateTime.now()
        );

        when(customerAuthService.updateProfilePic(eq(1L), any(CustomerUpdateProfileDto.class))).thenReturn(updatedProfile);

        mockMvc.perform(put("/auth/customer/profile/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profilePicUrl").value("http://images.com/new-pic.jpg"));
    }

    @Test
    @DisplayName("POST /change-password/{id} - Success")
    void changePassword_Success() throws Exception {
        // Updated to match your PasswordChangeRequestDto fields
        PasswordChangeRequestDto pwdDto = new PasswordChangeRequestDto("OldPass123", "NewPass123", "NewPass123");
        ResponseDto response = new ResponseDto("Password updated successfully", null);

        when(customerAuthService.changePassword(eq(1L), any(PasswordChangeRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/auth/customer/change-password/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pwdDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));
    }

    @Test
    @DisplayName("POST /logout - Success with Token Truncation")
    void logout_Success() throws Exception {
        mockMvc.perform(post("/auth/customer/logout")
                        .header("Authorization", "Bearer sample_token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Customer logged out successfully"));

        verify(customerAuthService, times(1)).logout("sample_token");
    }

    @Test
    @DisplayName("POST /logout - Failure Missing Header")
    void logout_MissingHeader() throws Exception {
        mockMvc.perform(post("/auth/customer/logout"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /refresh - Success")
    void refresh_Success() throws Exception {
        ResponseDto response = new ResponseDto("Token Refreshed", "NEW_TOKEN");
        when(customerAuthService.refreshToken("old_token")).thenReturn(response);

        mockMvc.perform(post("/auth/customer/refresh")
                        .header("Authorization", "Bearer old_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token Refreshed"))
                .andExpect(jsonPath("$.token").value("NEW_TOKEN")); // Changed from $.data
    }

    @Test
    @DisplayName("POST /deactivate/{id} - Success")
    void deactivateAccount_Success() throws Exception {
        ResponseDto response = new ResponseDto("Account Deactivated", null);
        when(customerAuthService.deactivateAccount(1L)).thenReturn(response);

        mockMvc.perform(post("/auth/customer/deactivate/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account Deactivated"));
    }
}
