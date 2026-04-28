package com.quickbite.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.quickbite.delivery.dto.AssignOrderRequestDto;

@FeignClient(name = "order-service", path = "/api/v1/orders")
public interface OrderServiceClient {

    @PutMapping("/{orderId}/assign-agent")
    void assignAgent(@PathVariable("orderId") Long orderId, @RequestBody AssignOrderRequestDto request);
}
