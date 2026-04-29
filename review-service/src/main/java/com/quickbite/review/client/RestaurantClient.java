package com.quickbite.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.review.client.dto.RestaurantDto;

@FeignClient(
        name = "${quickbite.clients.restaurant-service.name:restaurant-service}",
        path = "${quickbite.clients.restaurant-service.path:/restaurants}")
public interface RestaurantClient {

    @GetMapping("/{restaurantId}")
    RestaurantDto getRestaurantById(@PathVariable("restaurantId") Long restaurantId);
}
