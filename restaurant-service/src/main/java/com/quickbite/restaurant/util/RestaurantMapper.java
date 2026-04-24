package com.quickbite.restaurant.util;

import org.springframework.stereotype.Component;

import com.quickbite.restaurant.dto.RestaurantRequest;
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.entity.Restaurant;

@Component
public class RestaurantMapper {

    public Restaurant toEntity(RestaurantRequest request) {
        return Restaurant.builder()
            .ownerId(request.ownerId())
            .name(request.name())
            .description(request.description())
            .cuisine(request.cuisine())
            .address(request.address())
            .city(request.city())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .phone(request.phone())
            .deliveryRadius(request.deliveryRadius())
            .minOrderAmount(request.minOrderAmount())
            .estimatedDeliveryMin(request.estimatedDeliveryMin())
            .build();
    }

    public void updateEntity(Restaurant restaurant, RestaurantRequest request) {
        restaurant.setOwnerId(request.ownerId());
        restaurant.setName(request.name());
        restaurant.setDescription(request.description());
        restaurant.setCuisine(request.cuisine());
        restaurant.setAddress(request.address());
        restaurant.setCity(request.city());
        restaurant.setLatitude(request.latitude());
        restaurant.setLongitude(request.longitude());
        restaurant.setPhone(request.phone());
        restaurant.setDeliveryRadius(request.deliveryRadius());
        restaurant.setMinOrderAmount(request.minOrderAmount());
        restaurant.setEstimatedDeliveryMin(request.estimatedDeliveryMin());
    }

    public RestaurantResponse toResponse(Restaurant restaurant) {
        return new RestaurantResponse(
            restaurant.getRestaurantId(),
            restaurant.getOwnerId(),
            restaurant.getName(),
            restaurant.getDescription(),
            restaurant.getCuisine(),
            restaurant.getAddress(),
            restaurant.getCity(),
            restaurant.getLatitude(),
            restaurant.getLongitude(),
            restaurant.getPhone(),
            restaurant.getAvgRating(),
            restaurant.getDeliveryRadius(),
            restaurant.getIsOpen(),
            restaurant.getIsApproved(),
            restaurant.getMinOrderAmount(),
            restaurant.getEstimatedDeliveryMin()
        );
    }
}
