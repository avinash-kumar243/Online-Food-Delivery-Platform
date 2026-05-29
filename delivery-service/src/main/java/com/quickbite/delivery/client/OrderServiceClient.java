package com.quickbite.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.quickbite.delivery.dto.AssignOrderRequestDto;
import com.quickbite.delivery.dto.OrderSnapshotDto;
import com.quickbite.delivery.dto.UpdateOrderStatusRequest;

@FeignClient(name = "order-service", path = "/api/v1/orders")
public interface OrderServiceClient {

    @PutMapping("/{orderId}/assign-agent")
    OrderSnapshotDto assignAgent(@PathVariable Long orderId, @RequestBody AssignOrderRequestDto request);

    @PutMapping("/{orderId}/status")
    OrderSnapshotDto updateOrderStatus(@PathVariable Long orderId, @RequestBody UpdateOrderStatusRequest request);
}
