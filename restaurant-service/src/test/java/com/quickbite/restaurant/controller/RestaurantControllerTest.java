package com.quickbite.restaurant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.exception.BadRequestException;
import com.quickbite.restaurant.exception.GlobalExceptionHandler;
import com.quickbite.restaurant.service.RestaurantService;

@ExtendWith(MockitoExtension.class)
class RestaurantControllerTest {

    @Mock
    private RestaurantService restaurantService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private RestaurantResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new RestaurantController(restaurantService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();

        response = new RestaurantResponse(
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
            99L,
            LocalDateTime.of(2026, 5, 1, 10, 0),
            LocalDateTime.of(2026, 4, 30, 9, 0),
            150,
            35
        );
    }

    @Test
    void registerRestaurant_ReturnsCreated() throws Exception {
        when(restaurantService.registerRestaurant(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/restaurants")
                .contentType("application/json")
                .content(validRestaurantRequestJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.restaurantId").value(1))
            .andExpect(jsonPath("$.name").value("QuickBite Cafe"));
    }

    @Test
    void registerRestaurant_InvalidBody_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants")
                .contentType("application/json")
                .content("{\"ownerId\":11}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getRestaurantById_ReturnsOk() throws Exception {
        when(restaurantService.getRestaurantById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/restaurants/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.restaurantId").value(1));
    }

    @Test
    void getRestaurantsByOwner_ReturnsOk() throws Exception {
        when(restaurantService.getRestaurantsByOwner(11L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/restaurants/owner/11"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].ownerId").value(11));
    }

    @Test
    void getApprovedRestaurants_ReturnsOk() throws Exception {
        when(restaurantService.getApprovedRestaurants()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/restaurants/approved"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].approvalStatus").value("APPROVED"));
    }

    @Test
    void searchRestaurants_ReturnsOk() throws Exception {
        when(restaurantService.searchRestaurants("quick", "Pune", "Indian")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/restaurants/search")
                .param("name", "quick")
                .param("city", "Pune")
                .param("cuisine", "Indian"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("QuickBite Cafe"));
    }

    @Test
    void getNearbyRestaurants_WithLatitudeLongitude_ReturnsOk() throws Exception {
        when(restaurantService.findNearbyRestaurants(18.5204, 73.8567, 5.0)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/restaurants/nearby")
                .param("latitude", "18.5204")
                .param("longitude", "73.8567"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].restaurantId").value(1));
    }

    @Test
    void getNearbyRestaurants_WithLatLngAliases_ReturnsOk() throws Exception {
        when(restaurantService.findNearbyRestaurants(18.5204, 73.8567, 7.5)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/restaurants/nearby")
                .param("lat", "18.5204")
                .param("lng", "73.8567")
                .param("radiusKm", "7.5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].restaurantId").value(1));
    }

    @Test
    void getNearbyRestaurants_WithoutCoordinates_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/restaurants/nearby"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("latitude/longitude or lat/lng are required"));
    }

    @Test
    void updateRestaurant_ReturnsOk() throws Exception {
        when(restaurantService.updateRestaurant(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/restaurants/1")
                .contentType("application/json")
                .content(validRestaurantRequestJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.restaurantId").value(1));
    }

    @Test
    void approveRestaurant_WhenApprovedTrue_CallsApprove() throws Exception {
        when(restaurantService.approveRestaurant(1L, null)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/restaurants/1/approve")
                .contentType("application/json")
                .content("{\"approved\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));

        verify(restaurantService).approveRestaurant(1L, null);
    }

    @Test
    void approveRestaurant_WhenApprovedFalse_CallsReject() throws Exception {
        RestaurantResponse rejected = new RestaurantResponse(
            1L, 11L, "QuickBite Cafe", "All day dining", "Indian", "12 Main Street", "Pune",
            18.5204, 73.8567, "9876543210", 4.5, 6.5, false, false, "REJECTED",
            "rejected", null, null, LocalDateTime.of(2026, 4, 30, 9, 0), 150, 35
        );
        when(restaurantService.rejectRestaurant(1L, null, null)).thenReturn(rejected);

        mockMvc.perform(patch("/api/v1/restaurants/1/approve")
                .contentType("application/json")
                .content("{\"approved\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.approvalStatus").value("REJECTED"));

        verify(restaurantService).rejectRestaurant(1L, null, null);
    }

    @Test
    void toggleRestaurantStatus_ReturnsOk() throws Exception {
        when(restaurantService.toggleRestaurantStatus(1L, true)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/restaurants/1/toggle-status")
                .contentType("application/json")
                .content("{\"open\":true}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isOpen").value(true));
    }

    @Test
    void updateAverageRating_ReturnsOk() throws Exception {
        when(restaurantService.updateAverageRating(1L, 4.8)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/restaurants/1/rating")
                .contentType("application/json")
                .content("{\"avgRating\":4.8}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.avgRating").value(4.5));
    }

    @Test
    void updateAverageRating_InvalidBody_ReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/v1/restaurants/1/rating")
                .contentType("application/json")
                .content("{\"avgRating\":5.6}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void deleteRestaurant_ReturnsOk() throws Exception {
        doNothing().when(restaurantService).deleteRestaurant(1L);

        mockMvc.perform(delete("/api/v1/restaurants/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("Restaurant deleted successfully"));
    }

    private String validRestaurantRequestJson() {
        return """
            {
              "ownerId": 11,
              "name": "QuickBite Cafe",
              "description": "All day dining",
              "cuisine": "Indian",
              "address": "12 Main Street",
              "city": "Pune",
              "latitude": 18.5204,
              "longitude": 73.8567,
              "phone": "9876543210",
              "deliveryRadius": 6.5,
              "minOrderAmount": 150,
              "estimatedDeliveryMin": 35
            }
            """;
    }
}
