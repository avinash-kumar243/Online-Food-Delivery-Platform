package com.quickbite.menu.service;

import java.util.List;

import com.quickbite.menu.dto.MenuCategoryRequest;
import com.quickbite.menu.dto.MenuCategoryResponse;
import com.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.menu.dto.RestaurantMenuResponse;

public interface MenuService {

    MenuCategoryResponse addCategory(MenuCategoryRequest request);

    MenuItemResponse addItem(MenuItemRequest request);

    MenuCategoryResponse updateCategory(MenuCategoryRequest request);

    MenuItemResponse updateItem(MenuItemRequest request);

    void deleteCategory(Integer categoryId);

    void deleteItem(Integer itemId);

    RestaurantMenuResponse getMenuByRestaurant(Integer restaurantId);

    List<MenuItemResponse> getItemsByCategory(Integer categoryId);

    MenuItemResponse getItemById(Integer itemId);

    MenuItemResponse toggleAvailability(Integer itemId, Boolean available);

    List<MenuItemResponse> searchItems(String query);

    List<MenuItemResponse> getVegItems(Integer restaurantId, Boolean availableOnly);
}
