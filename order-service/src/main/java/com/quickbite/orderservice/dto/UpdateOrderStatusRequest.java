package com.quickbite.orderservice.dto;

import com.quickbite.orderservice.entity.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
    @NotNull OrderStatus orderStatus
) {
}
