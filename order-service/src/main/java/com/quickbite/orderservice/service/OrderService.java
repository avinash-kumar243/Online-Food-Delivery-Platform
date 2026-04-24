package com.quickbite.orderservice.service;

import java.util.List;

import com.quickbite.orderservice.dto.OrderResponse;
import com.quickbite.orderservice.dto.PlaceOrderRequest;
import com.quickbite.orderservice.entity.OrderStatus;

public interface OrderService {

    OrderResponse placeOrder(PlaceOrderRequest request);

    OrderResponse getOrderById(Long orderId);

    List<OrderResponse> getOrdersByCustomerId(Long customerId);

    List<OrderResponse> getOrdersByRestaurantId(Long restaurantId);

    List<OrderResponse> getActiveOrders();

    OrderResponse updateOrderStatus(Long orderId, OrderStatus status);

    OrderResponse assignDeliveryAgent(Long orderId, Long deliveryAgentId);

    OrderResponse reorder(Long orderId);

    long countOrders(Long restaurantId);
}
