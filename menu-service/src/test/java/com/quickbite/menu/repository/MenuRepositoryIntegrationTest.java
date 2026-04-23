package com.quickbite.menu.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.quickbite.menu.entity.MenuCategory;
import com.quickbite.menu.entity.MenuItem;

@DataJpaTest
class MenuRepositoryIntegrationTest {

    @Autowired
    private MenuCategoryRepository categoryRepository;

    @Autowired
    private MenuItemRepository itemRepository;

    @Test
    void findByNameContainingIgnoreCaseShouldReturnMatchingItems() {
        MenuCategory category = categoryRepository.save(MenuCategory.builder()
            .restaurantId(11)
            .name("Starters")
            .displayOrder(1)
            .build());

        itemRepository.save(MenuItem.builder()
            .restaurantId(11)
            .name("Cheese Garlic Bread")
            .price(180.0)
            .isVeg(true)
            .isAvailable(true)
            .category(category)
            .build());

        List<MenuItem> result = itemRepository.findByNameContainingIgnoreCase("garlic");

        assertThat(result).hasSize(1);
    }
}
