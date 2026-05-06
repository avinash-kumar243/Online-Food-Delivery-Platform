package com.quickbite.auth.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import com.quickbite.auth.service.AdminAuthService;
import com.quickbite.auth.service.JwtService;
import com.quickbite.auth.service.CustomUserDetailsService; // Ensure this import exists
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.quickbite.auth.dto.PlatformUserDto;
import com.quickbite.auth.enums.UserRole;
import com.quickbite.auth.service.UserAdministrationService;

@WebMvcTest(AdminUserController.class)
// addFilters = false bypasses the security chain, but Spring still needs to
// instantiate the beans that make up the security configuration.
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private JwtService jwtService;
    @MockBean private CustomUserDetailsService customUserDetailsService;
    @MockBean private AdminAuthService adminAuthService;

    @MockBean
    private UserAdministrationService userAdministrationService;

    private PlatformUserDto createMockUserDto(Long id, String name, String role) {
        return new PlatformUserDto(
                id,
                name,
                name.toLowerCase().replace(" ", "") + "@example.com",
                "1234567890",
                role,
                "ACTIVE",
                true
        );
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - Should return list of all users")
    void getAllUsers_Success() throws Exception {
        List<PlatformUserDto> users = List.of(
                createMockUserDto(1L, "Avinash Kumar", "ADMIN"),
                createMockUserDto(2L, "John Doe", "CUSTOMER")
        );
        when(userAdministrationService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.size()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Avinash Kumar"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    @DisplayName("GET /role/{role} - Should return users filtered by role")
    void getUsersByRole_Success() throws Exception {
        UserRole role = UserRole.ADMIN;
        List<PlatformUserDto> users = List.of(createMockUserDto(1L, "Admin User", "ADMIN"));
        when(userAdministrationService.getUsersByRole(role)).thenReturn(users);

        mockMvc.perform(get("/api/v1/admin/users/role/" + role))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    @Test
    @DisplayName("PUT /{userId}/suspend - Should call service and return 204")
    void suspendUser_Success() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/101/suspend")
                        .param("role", "ADMIN"))
                .andExpect(status().isNoContent());

        verify(userAdministrationService, times(1)).suspendUser(UserRole.ADMIN, 101L);
    }

    @Test
    @DisplayName("PUT /{userId}/reactivate - Should call service and return 204")
    void reactivateUser_Success() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/101/reactivate")
                        .param("role", "ADMIN"))
                .andExpect(status().isNoContent());

        verify(userAdministrationService, times(1)).reactivateUser(UserRole.ADMIN, 101L);
    }

    @Test
    @DisplayName("DELETE /{userId} - Should call service and return 204")
    void deleteUser_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/101")
                        .param("role", "ADMIN"))
                .andExpect(status().isNoContent());

        verify(userAdministrationService, times(1)).deleteUser(UserRole.ADMIN, 101L);
    }
}