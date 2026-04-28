package com.quickbite.orderservice.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.orderservice.dto.OrderResponse;
import com.quickbite.orderservice.service.OrderService;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderResource {

    private final OrderService orderService;

    public AdminOrderResource(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }
}
