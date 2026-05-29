package com.quickbite.restaurant.service;

import java.util.List;

import com.quickbite.restaurant.dto.AdminRestaurantResponse;
import com.quickbite.restaurant.dto.RestaurantRequest;
import com.quickbite.restaurant.dto.RestaurantResponse;

public interface RestaurantService {

    RestaurantResponse registerRestaurant(RestaurantRequest request);

    RestaurantResponse getRestaurantById(Long restaurantId);

    List<RestaurantResponse> getRestaurantsByOwner(Long ownerId);

    List<RestaurantResponse> getApprovedRestaurants();

    List<RestaurantResponse> searchRestaurants(String name, String city, String cuisine);

    List<RestaurantResponse> findNearbyRestaurants(double latitude, double longitude, double radiusKm);

    RestaurantResponse updateRestaurant(Long restaurantId, RestaurantRequest request);

    List<AdminRestaurantResponse> getPendingRestaurants();

    List<AdminRestaurantResponse> getAllRestaurantsForAdmin();

    RestaurantResponse approveRestaurant(Long restaurantId, Long adminId);

    RestaurantResponse rejectRestaurant(Long restaurantId, Long adminId, String feedback);

    RestaurantResponse toggleRestaurantStatus(Long restaurantId, boolean open);

    RestaurantResponse updateAverageRating(Long restaurantId, double avgRating);

    void deleteRestaurant(Long restaurantId);

    void deleteRestaurantsByOwnerId(Long ownerId);
}
