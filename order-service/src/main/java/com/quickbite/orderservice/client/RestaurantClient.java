package com.quickbite.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.orderservice.client.dto.RestaurantRealtimeDto;

@FeignClient(name = "restaurant-service", path = "/api/v1/restaurants")
public interface RestaurantClient {

    @GetMapping("/{restaurantId}")
    RestaurantRealtimeDto getRestaurantById(@PathVariable Long restaurantId);
}
