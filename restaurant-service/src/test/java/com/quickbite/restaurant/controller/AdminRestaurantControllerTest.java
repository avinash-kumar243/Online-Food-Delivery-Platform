package com.quickbite.restaurant.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.restaurant.dto.AdminRestaurantResponse;
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.exception.GlobalExceptionHandler;
import com.quickbite.restaurant.service.RestaurantService;

@ExtendWith(MockitoExtension.class)
class AdminRestaurantControllerTest {

    @Mock
    private RestaurantService restaurantService;

    private MockMvc mockMvc;
    private AdminRestaurantResponse adminResponse;
    private RestaurantResponse restaurantResponse;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new AdminRestaurantController(restaurantService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();

        adminResponse = new AdminRestaurantResponse(
            1L,
            "QuickBite Cafe",
            11L,
            "Owner Name",
            "owner@test.com",
            "9876543210",
            "Indian",
            "12 Main Street",
            "Pune",
            "9876543210",
            4.5,
            false,
            false,
            "PENDING",
            LocalDateTime.of(2026, 5, 1, 10, 0),
            null,
            null,
            null
        );

        restaurantResponse = new RestaurantResponse(
            1L,
            11L,
            "QuickBite Cafe",
            "All day dining",
            "Indian",
            "12 Main Street",
            "Pune",
            18.5204,
            73.8567,
            "9876543210",
            4.5,
            6.5,
            true,
            true,
            "APPROVED",
            null,
            100L,
            LocalDateTime.of(2026, 5, 2, 12, 0),
            LocalDateTime.of(2026, 5, 1, 10, 0),
            150,
            35
        );
    }

    @Test
    void getPendingRestaurants_ReturnsOk() throws Exception {
        when(restaurantService.getPendingRestaurants()).thenReturn(List.of(adminResponse));

        mockMvc.perform(get("/api/v1/admin/restaurants/pending"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].restaurantId").value(1))
            .andExpect(jsonPath("$[0].approvalStatus").value("PENDING"));
    }

    @Test
    void getAllRestaurants_ReturnsOk() throws Exception {
        when(restaurantService.getAllRestaurantsForAdmin()).thenReturn(List.of(adminResponse));

        mockMvc.perform(get("/api/v1/admin/restaurants/all"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ownerName").value("Owner Name"));
    }

    @Test
    void approveRestaurant_ReturnsOk() throws Exception {
        when(restaurantService.approveRestaurant(1L, 100L)).thenReturn(restaurantResponse);

        mockMvc.perform(put("/api/v1/admin/restaurants/1/approve")
                .contentType("application/json")
                .content("{\"adminId\":100,\"feedback\":\"ok\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

        verify(restaurantService).approveRestaurant(1L, 100L);
    }

    @Test
    void rejectRestaurant_ReturnsOk() throws Exception {
        RestaurantResponse rejected = new RestaurantResponse(
            1L, 11L, "QuickBite Cafe", "All day dining", "Indian", "12 Main Street", "Pune",
            18.5204, 73.8567, "9876543210", 4.5, 6.5, false, false, "REJECTED",
            "missing docs", 100L, LocalDateTime.of(2026, 5, 2, 12, 0), LocalDateTime.of(2026, 5, 1, 10, 0), 150, 35
        );
        when(restaurantService.rejectRestaurant(1L, 100L, "missing docs")).thenReturn(rejected);

        mockMvc.perform(put("/api/v1/admin/restaurants/1/reject")
                .contentType("application/json")
                .content("{\"adminId\":100,\"feedback\":\"missing docs\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("REJECTED"));

        verify(restaurantService).rejectRestaurant(1L, 100L, "missing docs");
    }

    @Test
    void approveRestaurant_InvalidBody_ReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/admin/restaurants/1/approve")
                .contentType("application/json")
                .content("{\"feedback\":\"ok\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }
}
