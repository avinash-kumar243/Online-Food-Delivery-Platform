package com.quickbite.orderservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.quickbite.orderservice.entity.OrderStatus;

public record OrderResponse(
    Long orderId,
    Long customerId,
    Long restaurantId,
    Long deliveryAgentId,
    BigDecimal totalAmount,
    BigDecimal discount,
    BigDecimal finalAmount,
    String modeOfPayment,
    String paymentStatus,
    OrderStatus orderStatus,
    OffsetDateTime orderDate,
    OffsetDateTime estimatedDelivery,
    String deliveryAddress,
    String specialInstructions,
    List<OrderItemResponse> items,
    OrderRestaurantInfo restaurant,
    OrderDeliveryPartnerInfo deliveryPartner
) {
}
