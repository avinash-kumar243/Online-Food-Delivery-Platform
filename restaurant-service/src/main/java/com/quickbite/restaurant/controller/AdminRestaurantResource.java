package com.quickbite.restaurant.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.restaurant.dto.AdminRestaurantDecisionRequest;
import com.quickbite.restaurant.dto.AdminRestaurantResponse;
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.service.RestaurantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/restaurants")
public class AdminRestaurantResource {

    private final RestaurantService restaurantService;

    @GetMapping("/pending")
    public ResponseEntity<List<AdminRestaurantResponse>> getPendingRestaurants() {
        return ResponseEntity.ok(restaurantService.getPendingRestaurants());
    }

    @GetMapping("/all")
    public ResponseEntity<List<AdminRestaurantResponse>> getAllRestaurants() {
        return ResponseEntity.ok(restaurantService.getAllRestaurantsForAdmin());
    }

    @PutMapping("/{restaurantId}/approve")
    public ResponseEntity<RestaurantResponse> approveRestaurant(
        @PathVariable Long restaurantId,
        @Valid @RequestBody AdminRestaurantDecisionRequest request
    ) {
        return ResponseEntity.ok(restaurantService.approveRestaurant(restaurantId, request.adminId()));
    }

    @PutMapping("/{restaurantId}/reject")
    public ResponseEntity<RestaurantResponse> rejectRestaurant(
        @PathVariable Long restaurantId,
        @Valid @RequestBody AdminRestaurantDecisionRequest request
    ) {
        return ResponseEntity.ok(restaurantService.rejectRestaurant(restaurantId, request.adminId(), request.feedback()));
    }
}
