package com.quickbite.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.quickbite.payment.dto.OrderSnapshotDto;
import com.quickbite.payment.dto.OrderPaymentStatusRequest;

@FeignClient(name = "order-service", path = "/api/v1/orders")
public interface OrderServiceClient {

    @GetMapping("/{orderId}")
    OrderSnapshotDto getOrderById(@PathVariable("orderId") Long orderId);

    @PutMapping("/{orderId}/payment-status")
    void updateOrderPaymentStatus(@PathVariable("orderId") Long orderId, @RequestBody OrderPaymentStatusRequest request);
}
