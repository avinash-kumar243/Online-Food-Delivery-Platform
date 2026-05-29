package com.quickbite.cartservice.dto;

public record CartItemResponse(
    Long itemId,
    Long menuItemId,
    String name,
    Double price,
    Integer quantity,
    String customization,
    Double lineTotal
) {
}
