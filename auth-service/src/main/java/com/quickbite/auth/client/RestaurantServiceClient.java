package com.quickbite.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "restaurant-service", path = "/api/v1/restaurants")
public interface RestaurantServiceClient {

    @DeleteMapping("/owner/{ownerId}")
    void deleteRestaurantsByOwnerId(@PathVariable("ownerId") Long ownerId);
}
