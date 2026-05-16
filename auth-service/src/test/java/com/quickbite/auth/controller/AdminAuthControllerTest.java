package com.quickbite.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.quickbite.auth.dto.AdminLoginRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.service.AdminAuthService;

@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Disables Spring Security for unit testing the controller logic
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminAuthService adminAuthService;

    // Security Trinity: Required to prevent ApplicationContext load failure
    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("Should return 200 OK when admin login is successful")
    void login_Success() throws Exception {
        // Arrange
        AdminLoginRequestDto request = new AdminLoginRequestDto("admin@quickbite.com", "password123");
        // Ensure ResponseDto fields match your JSON structure (message and token)
        ResponseDto responseDto = new ResponseDto("Login Successful", "JWT_TOKEN_HERE");

        when(adminAuthService.login(anyString(), anyString())).thenReturn(responseDto);

        // Act & Assert
        mockMvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login Successful"))
                // FIX: Changed "$.data" to "$.token" to match your actual response logs
                .andExpect(jsonPath("$.token").value("JWT_TOKEN_HERE"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when request body is invalid")
    void login_InvalidRequest() throws Exception {
        // Arrange: Providing invalid data to trigger @Valid
        AdminLoginRequestDto invalidRequest = new AdminLoginRequestDto("", "");

        // Act & Assert
        mockMvc.perform(post("/auth/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}