package com.quickbite.restaurant.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.restaurant.dto.RestaurantApprovalRequest;
import com.quickbite.restaurant.dto.RestaurantRatingRequest;
import com.quickbite.restaurant.dto.RestaurantRequest;
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.dto.RestaurantStatusRequest;
import com.quickbite.restaurant.exception.BadRequestException;
import com.quickbite.restaurant.service.RestaurantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping({"/restaurants", "/api/v1/restaurants"})
@RequiredArgsConstructor
public class RestaurantResource {

    private final RestaurantService restaurantService;

    @PostMapping
    public ResponseEntity<RestaurantResponse> registerRestaurant(@Valid @RequestBody RestaurantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(restaurantService.registerRestaurant(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestaurantResponse> getRestaurantById(@PathVariable Long id) {
        return ResponseEntity.ok(restaurantService.getRestaurantById(id));
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<RestaurantResponse>> getRestaurantsByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(restaurantService.getRestaurantsByOwner(ownerId));
    }

    @GetMapping("/approved")
    public ResponseEntity<List<RestaurantResponse>> getApprovedRestaurants() {
        return ResponseEntity.ok(restaurantService.getApprovedRestaurants());
    }

    @GetMapping("/search")
    public ResponseEntity<List<RestaurantResponse>> searchRestaurants(
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String city,
        @RequestParam(required = false) String cuisine
    ) {
        return ResponseEntity.ok(restaurantService.searchRestaurants(name, city, cuisine));
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<RestaurantResponse>> getNearbyRestaurants(
        @RequestParam(required = false) Double latitude,
        @RequestParam(required = false, name = "lat") Double lat,
        @RequestParam(required = false) Double longitude,
        @RequestParam(required = false, name = "lng") Double lng,
        @RequestParam(required = false, defaultValue = "5") Double radiusKm
    ) {
        Double resolvedLatitude = latitude != null ? latitude : lat;
        Double resolvedLongitude = longitude != null ? longitude : lng;
        if (resolvedLatitude == null || resolvedLongitude == null) {
            throw new BadRequestException("latitude/longitude or lat/lng are required");
        }
        return ResponseEntity.ok(restaurantService.findNearbyRestaurants(resolvedLatitude, resolvedLongitude, radiusKm));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestaurantResponse> updateRestaurant(
        @PathVariable Long id,
        @Valid @RequestBody RestaurantRequest request
    ) {
        return ResponseEntity.ok(restaurantService.updateRestaurant(id, request));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<RestaurantResponse> approveRestaurant(
        @PathVariable Long id,
        @Valid @RequestBody RestaurantApprovalRequest request
    ) {
        return request.approved()
            ? ResponseEntity.ok(restaurantService.approveRestaurant(id, null))
            : ResponseEntity.ok(restaurantService.rejectRestaurant(id, null, null));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<RestaurantResponse> toggleRestaurantStatus(
        @PathVariable Long id,
        @Valid @RequestBody RestaurantStatusRequest request
    ) {
        return ResponseEntity.ok(restaurantService.toggleRestaurantStatus(id, request.open()));
    }

    @PatchMapping("/{id}/rating")
    public ResponseEntity<RestaurantResponse> updateAverageRating(
        @PathVariable Long id,
        @Valid @RequestBody RestaurantRatingRequest request
    ) {
        return ResponseEntity.ok(restaurantService.updateAverageRating(id, request.avgRating()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestaurant(@PathVariable Long id) {
        restaurantService.deleteRestaurant(id);
        return ResponseEntity.noContent().build();
    }
}
