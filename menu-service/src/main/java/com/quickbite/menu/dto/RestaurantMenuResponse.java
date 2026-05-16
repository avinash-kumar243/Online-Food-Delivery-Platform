package com.quickbite.menu.dto;

import java.util.List;

public record RestaurantMenuResponse(
    Integer restaurantId,
    int totalItems,
    List<MenuCategoryResponse> categories
) {
}
