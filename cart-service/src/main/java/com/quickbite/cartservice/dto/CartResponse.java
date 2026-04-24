package com.quickbite.cartservice.dto;

import java.util.List;

public record CartResponse(
    Long cartId,
    Long customerId,
    Long restaurantId,
    Double totalPrice,
    List<CartItemResponse> items
) {
}
