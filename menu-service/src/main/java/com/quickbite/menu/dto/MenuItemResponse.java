package com.quickbite.menu.dto;

public record MenuItemResponse(
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
    String tags
) {
}
