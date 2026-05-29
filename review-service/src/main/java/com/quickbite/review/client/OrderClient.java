package com.quickbite.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.review.client.dto.OrderDto;

@FeignClient(
        name = "${quickbite.clients.order-service.name:order-service}",
        path = "${quickbite.clients.order-service.path:/orders}")
public interface OrderClient {

    @GetMapping("/{orderId}")
    OrderDto getOrderById(@PathVariable Long orderId);
}
