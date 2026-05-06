package com.quickbite.auth.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.quickbite.auth.service.RestaurantOwnerAuthServiceImpl;

@WebMvcTest(RestaurantOwnerAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class RestaurantOwnerAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantOwnerAuthServiceImpl restaurantOwnerAuthService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private AdminAuthService adminAuthService;

    @Test
    @DisplayName("POST /register - Success")
    void register_Success() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("Owner Name", "owner@test.com", "pass123", "9876543210");
        ResponseDto response = new ResponseDto("Owner Registered", "TOKEN");

        when(restaurantOwnerAuthService.register(any(RegisterRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/auth/restaurant/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Owner Registered"));
    }

    @Test
    @DisplayName("POST /login - Success")
    void login_Success() throws Exception {
        LoginRequestDTO request = new LoginRequestDTO("owner@test.com", "pass123");
        ResponseDto response = new ResponseDto("Login Success", "JWT_TOKEN");

        when(restaurantOwnerAuthService.login(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/auth/restaurant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("JWT_TOKEN"));
    }

    @Test
    @DisplayName("POST /logout - Success with Bearer")
    void logout_Success() throws Exception {
        mockMvc.perform(post("/auth/restaurant/logout")
                        .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(content().string("Restaurant owner logged out successfully"));

        verify(restaurantOwnerAuthService, times(1)).logout("valid_token");
    }

    @Test
    @DisplayName("POST /logout - Failure Missing/Invalid Header")
    void logout_Failure() throws Exception {
        mockMvc.perform(post("/auth/restaurant/logout")
                        .header("Authorization", "invalid_format"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid or missing Authorization header"));
    }

    @Test
    @DisplayName("POST /refresh - Success")
    void refresh_Success() throws Exception {
        ResponseDto response = new ResponseDto("Refreshed", "NEW_TOKEN");
        when(restaurantOwnerAuthService.refreshToken("old_token")).thenReturn(response);

        mockMvc.perform(post("/auth/restaurant/refresh")
                        .header("Authorization", "Bearer old_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("NEW_TOKEN"));
    }

    @Test
    @DisplayName("GET /profile/{id} - Success")
    void getProfile_Success() throws Exception {
        RestaurantOwnerProfileDto profile = new RestaurantOwnerProfileDto();
        // Assuming ProfileDto has these fields (set according to your actual DTO)
        when(restaurantOwnerAuthService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/auth/restaurant/profile/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /profile/{id} - Success")
    void updateProfile_Success() throws Exception {
        RestaurantOwnerUpdateProfileDto update = new RestaurantOwnerUpdateProfileDto(
                "New Name", "9999999999", "New Restaurant", "Address", "url"
        );
        RestaurantOwnerProfileDto response = new RestaurantOwnerProfileDto();

        when(restaurantOwnerAuthService.updateProfile(eq(1L), any(RestaurantOwnerUpdateProfileDto.class)))
                .thenReturn(response);

        mockMvc.perform(put("/auth/restaurant/profile/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /change-password/{id} - Success")
    void changePassword_Success() throws Exception {
        PasswordChangeRequestDto pwdDto = new PasswordChangeRequestDto("old", "new", "new");
        ResponseDto response = new ResponseDto("Changed", null);

        when(restaurantOwnerAuthService.changePassword(eq(1L), any(PasswordChangeRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/auth/restaurant/change-password/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pwdDto)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /deactivate/{id} - Success")
    void deactivateAccount_Success() throws Exception {
        ResponseDto response = new ResponseDto("Deactivated", null);
        when(restaurantOwnerAuthService.deactivateAccount(1L)).thenReturn(response);

        mockMvc.perform(post("/auth/restaurant/deactivate/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deactivated"));
    }
}