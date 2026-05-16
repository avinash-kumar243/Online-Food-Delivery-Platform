package com.quickbite.menu.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.quickbite.menu.entity.MenuCategory;
import com.quickbite.menu.entity.MenuItem;

@DataJpaTest
class MenuItemRepositoryTest {

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
            .displayOrder(1)
            .build());

        desserts = categoryRepository.save(MenuCategory.builder()
            .restaurantId(10)
            .name("Desserts")
            .displayOrder(2)
            .build());

        itemRepository.save(MenuItem.builder()
            .restaurantId(10)
            .name("Paneer Tikka")
            .price(250.0)
            .discountedPrice(220.0)
            .isVeg(true)
            .isAvailable(true)
            .category(starters)
            .build());

        itemRepository.save(MenuItem.builder()
            .restaurantId(10)
            .name("Chicken Wings")
            .price(300.0)
            .isVeg(false)
            .isAvailable(false)
            .category(starters)
            .build());

        itemRepository.save(MenuItem.builder()
            .restaurantId(12)
            .name("Gulab Jamun")
            .price(120.0)
            .isVeg(true)
            .isAvailable(true)
            .category(desserts)
            .build());
    }

    @Test
    void repositoryQueries_ReturnExpectedMatches() {
        assertEquals(2, itemRepository.findByRestaurantId(10).size());
        assertEquals(2, itemRepository.findByCategory_CategoryId(starters.getCategoryId()).size());
        assertEquals(2, itemRepository.findByIsVeg(true).size());
        assertEquals(2, itemRepository.findByIsAvailable(true).size());
        assertEquals(1, itemRepository.findByNameContainingIgnoreCase("paneer").size());
        assertEquals(2, itemRepository.findByPriceLessThanEqual(250.0).size());
        assertEquals(2, itemRepository.countByRestaurantId(10));
        assertEquals(1, itemRepository.findByRestaurantIdAndIsVeg(10, true).size());
    }
}
