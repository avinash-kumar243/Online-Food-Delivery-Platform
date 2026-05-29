package com.quickbite.cartservice.dto;

public record MenuItemSnapshotDto(
    Integer itemId,
    Integer restaurantId,
    Integer categoryId,
    String name,
    String description,
    Double price,
    Double discountedPrice,
    String imageUrl,
    Boolean isVeg,
    Boolean isAvailable,
    Double rating,
    Integer calories,
    String tags,
    String categoryName
) {
}
