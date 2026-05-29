package com.quickbite.orderservice.client.dto;

public record CartItemSnapshotDto(
    Long itemId,
    Long menuItemId,
    String name,
    Double price,
    Integer quantity,
    String customization,
    Double lineTotal
) {
}
