package com.quickbite.menu.util;

import java.util.List;

import org.springframework.stereotype.Component;

import com.quickbite.menu.dto.MenuCategoryRequest;
import com.quickbite.menu.dto.MenuCategoryResponse;
import com.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.menu.entity.MenuCategory;
import com.quickbite.menu.entity.MenuItem;

@Component
public class MenuMapper {

    public MenuCategory toEntity(MenuCategoryRequest request) {
        return MenuCategory.builder()
            .categoryId(request.categoryId())
            .restaurantId(request.restaurantId())
            .name(request.name())
            .description(request.description())
            .imageUrl(request.imageUrl())
            .displayOrder(request.displayOrder())
            .build();
    }

    public void updateEntity(MenuCategory category, MenuCategoryRequest request) {
        category.setRestaurantId(request.restaurantId());
        category.setName(request.name());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setDisplayOrder(request.displayOrder());
    }

    public MenuItem toEntity(MenuItemRequest request, MenuCategory category) {
        return MenuItem.builder()
            .itemId(request.itemId())
            .restaurantId(request.restaurantId())
            .name(request.name())
            .description(request.description())
            .price(request.price())
            .discountedPrice(request.discountedPrice())
            .imageUrl(request.imageUrl())
            .isVeg(request.isVeg())
            .isAvailable(request.isAvailable())
            .rating(request.rating())
            .calories(request.calories())
            .tags(request.tags())
            .category(category)
            .build();
    }

    public void updateEntity(MenuItem item, MenuItemRequest request, MenuCategory category) {
        item.setRestaurantId(request.restaurantId());
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setDiscountedPrice(request.discountedPrice());
        item.setImageUrl(request.imageUrl());
        item.setIsVeg(request.isVeg());
        item.setIsAvailable(request.isAvailable());
        item.setRating(request.rating());
        item.setCalories(request.calories());
        item.setTags(request.tags());
        item.setCategory(category);
    }

    public MenuCategoryResponse toResponse(MenuCategory category) {
        List<MenuItemResponse> items = category.getItems() == null ? List.of() : category.getItems().stream()
            .map(this::toResponse)
            .toList();

        return new MenuCategoryResponse(
            category.getCategoryId(),
            category.getRestaurantId(),
            category.getName(),
            category.getDescription(),
            category.getImageUrl(),
            category.getDisplayOrder(),
            items
        );
    }

    public MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
            item.getItemId(),
            item.getRestaurantId(),
            item.getCategory().getCategoryId(),
            item.getName(),
            item.getDescription(),
            item.getPrice(),
            item.getDiscountedPrice(),
            item.getImageUrl(),
            item.getIsVeg(),
            item.getIsAvailable(),
            item.getRating(),
            item.getCalories(),
            item.getTags()
        );
    }
}
