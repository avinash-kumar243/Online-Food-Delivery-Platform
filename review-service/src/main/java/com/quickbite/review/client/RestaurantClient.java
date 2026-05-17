package com.quickbite.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.quickbite.review.client.dto.RestaurantRatingRequestDto;

@FeignClient(name = "restaurant-service", path = "/api/v1/restaurants")
public interface RestaurantClient {

    @PatchMapping("/{restaurantId}/rating")
    void updateAverageRating(@PathVariable("restaurantId") Long restaurantId, @RequestBody RestaurantRatingRequestDto request);
}
