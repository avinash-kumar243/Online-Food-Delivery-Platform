package com.quickbite.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.orderservice.client.dto.CartSnapshotDto;

@FeignClient(name = "cart-service", path = "/api/v1/cart")
public interface CartClient {

    @GetMapping("/customer/{customerId}")
    CartSnapshotDto getCartByCustomerId(@PathVariable Long customerId);

    @DeleteMapping("/customer/{customerId}/clear")
    void clearCart(@PathVariable Long customerId);
}
