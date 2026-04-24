package com.quickbite.orderservice.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.orderservice.dto.AssignDeliveryAgentRequest;
import com.quickbite.orderservice.dto.OrderCountResponse;
import com.quickbite.orderservice.dto.OrderResponse;
import com.quickbite.orderservice.dto.PlaceOrderRequest;
import com.quickbite.orderservice.dto.UpdateOrderStatusRequest;
import com.quickbite.orderservice.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderResource {

    private final OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request));
    }

    @GetMapping("/active")
    public ResponseEntity<List<OrderResponse>> getActiveOrders() {
        return ResponseEntity.ok(orderService.getActiveOrders());
    }

    @GetMapping("/count")
    public ResponseEntity<OrderCountResponse> countOrders(@RequestParam(required = false) Long restaurantId) {
        return ResponseEntity.ok(new OrderCountResponse(restaurantId, orderService.countOrders(restaurantId)));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getCustomerOrders(@PathVariable Long customerId) {
        return ResponseEntity.ok(orderService.getOrdersByCustomerId(customerId));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<List<OrderResponse>> getRestaurantOrders(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(orderService.getOrdersByRestaurantId(restaurantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.orderStatus()));
    }

    @PutMapping("/{id}/assign-agent")
    public ResponseEntity<OrderResponse> assignDeliveryAgent(@PathVariable Long id,
                                                             @Valid @RequestBody AssignDeliveryAgentRequest request) {
        return ResponseEntity.ok(orderService.assignDeliveryAgent(id, request.deliveryAgentId()));
    }

    @PostMapping("/{id}/reorder")
    public ResponseEntity<OrderResponse> reorder(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.reorder(id));
    }
}
