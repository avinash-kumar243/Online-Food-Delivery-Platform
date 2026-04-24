package com.quickbite.restaurant.exception;

public class RestaurantNotFoundException extends RuntimeException {

    public RestaurantNotFoundException(Long restaurantId) {
        super("Restaurant not found with id: " + restaurantId);
    }
}
