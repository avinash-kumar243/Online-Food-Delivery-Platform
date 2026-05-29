package com.quickbite.menu.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.quickbite.menu.entity.MenuCategory;
import com.quickbite.menu.entity.MenuItem;

@DataJpaTest
class MenuCategoryRepositoryTest {

    @Autowired
    private MenuCategoryRepository categoryRepository;

    @Autowired
    private MenuItemRepository itemRepository;

    private MenuCategory starters;
    private MenuCategory desserts;

    @BeforeEach
    void setUp() {
        starters = categoryRepository.save(MenuCategory.builder()
            .restaurantId(10)
            .name("Starters")
            .description("Starter items")
            .imageUrl("img")
            .displayOrder(2)
            .build());

        desserts = categoryRepository.save(MenuCategory.builder()
            .restaurantId(10)
            .name("Desserts")
            .description("Dessert items")
            .imageUrl("img")
            .displayOrder(1)
            .build());

        itemRepository.save(MenuItem.builder()
            .restaurantId(10)
            .name("Paneer Tikka")
            .price(250.0)
            .isVeg(true)
            .isAvailable(true)
            .category(starters)
            .build());
    }

    @Test
    void findByRestaurantIdOrderByDisplayOrderAscNameAsc_ReturnsSortedCategories() {
        List<MenuCategory> result = categoryRepository.findByRestaurantIdOrderByDisplayOrderAscNameAsc(10);

        assertEquals(2, result.size());
        assertEquals("Desserts", result.get(0).getName());
        assertEquals("Starters", result.get(1).getName());
        assertEquals(10, result.get(0).getRestaurantId());
        assertEquals(10, result.get(1).getRestaurantId());
    }

    @Test
    void existsByRestaurantIdAndNameIgnoreCase_WorksIgnoringCase() {
        assertTrue(categoryRepository.existsByRestaurantIdAndNameIgnoreCase(10, "starters"));
        assertFalse(categoryRepository.existsByRestaurantIdAndNameIgnoreCase(10, "drinks"));
    }
}
