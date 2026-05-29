package com.quickbite.menu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.menu.entity.MenuCategory;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Integer> {

    @EntityGraph(attributePaths = "items")
    List<MenuCategory> findByRestaurantIdOrderByDisplayOrderAscNameAsc(Integer restaurantId);

    boolean existsByRestaurantIdAndNameIgnoreCase(Integer restaurantId, String name);
}
