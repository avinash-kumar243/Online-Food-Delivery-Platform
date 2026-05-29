package com.quickbite.menu.dto;

import java.util.List;

public record MenuCategoryResponse(
    Integer categoryId,
    Integer restaurantId,
    String name,
    String description,
    String imageUrl,
    Integer displayOrder,
    List<MenuItemResponse> items
) {
}
