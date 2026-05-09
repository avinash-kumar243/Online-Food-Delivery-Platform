package com.quickbite.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.notification.client.dto.RestaurantResponseDto;

@FeignClient(name = "RESTAURANT-SERVICE", path = "/api/v1/restaurants")
public interface RestaurantServiceClient {

    @GetMapping("/{restaurantId}")
    RestaurantResponseDto getRestaurantById(@PathVariable("restaurantId") Long restaurantId);
}
