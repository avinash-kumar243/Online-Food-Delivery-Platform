package com.quickbite.restaurant.service;

import java.util.List;

import com.quickbite.restaurant.dto.RestaurantRequest;
import com.quickbite.restaurant.dto.RestaurantResponse;

public interface RestaurantService {

    RestaurantResponse registerRestaurant(RestaurantRequest request);

    RestaurantResponse getRestaurantById(Long restaurantId);

    List<RestaurantResponse> getRestaurantsByOwner(Long ownerId);

    List<RestaurantResponse> searchRestaurants(String name, String city, String cuisine);

    List<RestaurantResponse> findNearbyRestaurants(double latitude, double longitude);

    RestaurantResponse updateRestaurant(Long restaurantId, RestaurantRequest request);

    RestaurantResponse approveRestaurant(Long restaurantId, boolean approved);

    RestaurantResponse toggleRestaurantStatus(Long restaurantId, boolean open);

    RestaurantResponse updateAverageRating(Long restaurantId, double avgRating);

    void deleteRestaurant(Long restaurantId);
}
