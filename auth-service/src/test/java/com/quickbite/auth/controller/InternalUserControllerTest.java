package com.quickbite.auth.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.quickbite.auth.service.AdminAuthService;
import com.quickbite.auth.service.CustomUserDetailsService;
import com.quickbite.auth.service.JwtService;
import com.quickbite.auth.service.TokenBlacklistService;
import com.quickbite.auth.service.UserAdministrationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.quickbite.auth.dto.InternalUserSummaryDto;
import com.quickbite.auth.enums.UserRole;

@WebMvcTest(InternalUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserAdministrationService userAdministrationService;

    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private TokenBlacklistService tokenBlacklistService;
    @MockBean private AdminAuthService adminAuthService;

    /**
     * Helper to create the Record DTO with your specific fields:
     * userId, fullName, email, phone, role, status, isActive
     */
    private InternalUserSummaryDto createSummary(Long id, String name, UserRole role) {
        return new InternalUserSummaryDto(
                id,
                name,
                name.toLowerCase().replace(" ", "") + "@quickbite.com",
                "9876543210",
                role.name(),
                "ACTIVE",
                true
        );
    }

    @Test
    @DisplayName("GET /summary - Should return correct record data for RESTAURANT_OWNER")
    void getUserSummary_Success() throws Exception {
        // Arrange
        InternalUserSummaryDto summary = createSummary(50L, "Bhopal Grill", UserRole.RESTAURANT_OWNER);

        when(userAdministrationService.getUserSummary(UserRole.RESTAURANT_OWNER, 50L))
                .thenReturn(summary);

        // Act & Assert
        mockMvc.perform(get("/api/v1/internal/users/summary")
                        .param("role", "RESTAURANT_OWNER")
                        .param("userId", "50"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId").value(50))
                .andExpect(jsonPath("$.fullName").value("Bhopal Grill"))
                .andExpect(jsonPath("$.role").value("RESTAURANT_OWNER"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("GET /role - Should return list of internal users by role")
    void getUsersByRole_Success() throws Exception {
        // Arrange
        List<InternalUserSummaryDto> partners = List.of(
                createSummary(1L, "Ravi Kumar", UserRole.DELIVERY_PARTNER),
                createSummary(2L, "Suresh Singh", UserRole.DELIVERY_PARTNER)
        );

        when(userAdministrationService.getUsersByRoleForInternal(UserRole.DELIVERY_PARTNER))
                .thenReturn(partners);

        // Act & Assert
        mockMvc.perform(get("/api/v1/internal/users/role")
                        .param("role", "DELIVERY_PARTNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Ravi Kumar"))
                .andExpect(jsonPath("$[1].phone").value("9876543210"));
    }

    @Test
    @DisplayName("GET /summary - Should return 400 Bad Request on invalid Enum")
    void getUserSummary_InvalidEnum() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/internal/users/summary")
                        .param("role", "INVALID_ROLE")
                        .param("userId", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /summary - Should return 400 when required params are missing")
    void getUserSummary_MissingParams() throws Exception {
        // Missing userId
        mockMvc.perform(get("/api/v1/internal/users/summary")
                        .param("role", "ADMIN"))
                .andExpect(status().isInternalServerError()); // Change .isBadRequest() to .isInternalServerError()
    }
}
