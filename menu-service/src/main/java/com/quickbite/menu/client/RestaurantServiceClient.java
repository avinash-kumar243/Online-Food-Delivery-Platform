package com.quickbite.menu.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.menu.dto.RestaurantSnapshotDto;

@FeignClient(name = "restaurant-service", path = "/api/v1/restaurants")
public interface RestaurantServiceClient {

    @GetMapping("/{restaurantId}")
    RestaurantSnapshotDto getRestaurant(@PathVariable Long restaurantId);
}
