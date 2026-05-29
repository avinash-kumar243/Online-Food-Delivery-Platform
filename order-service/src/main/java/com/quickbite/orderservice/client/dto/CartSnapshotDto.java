package com.quickbite.orderservice.client.dto;

import java.util.List;

public record CartSnapshotDto(
    Long cartId,
    Long customerId,
    Long restaurantId,
    Double totalPrice,
    List<CartItemSnapshotDto> items
) {
}
