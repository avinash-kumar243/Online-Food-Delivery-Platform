package com.quickbite.menu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.menu.entity.MenuItem;

public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {

    List<MenuItem> findByRestaurantId(Integer restaurantId);

    List<MenuItem> findByCategory_CategoryId(Integer categoryId);

    List<MenuItem> findByIsVeg(Boolean isVeg);

    List<MenuItem> findByIsAvailable(Boolean isAvailable);

    List<MenuItem> findByNameContainingIgnoreCase(String query);

    List<MenuItem> findByPriceLessThanEqual(Double maxPrice);

    int countByRestaurantId(Integer restaurantId);

    List<MenuItem> findByRestaurantIdAndIsVeg(Integer restaurantId, Boolean isVeg);
}
