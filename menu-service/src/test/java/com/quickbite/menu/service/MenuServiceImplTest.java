package com.quickbite.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import com.quickbite.menu.client.RestaurantServiceClient;
import com.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.menu.dto.RestaurantSnapshotDto;
import com.quickbite.menu.entity.MenuCategory;
import com.quickbite.menu.entity.MenuItem;
import com.quickbite.menu.exception.BadRequestException;
import com.quickbite.menu.repository.MenuCategoryRepository;
import com.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.menu.util.MenuMapper;

class MenuServiceImplTest {

    @Mock
    private MenuCategoryRepository categoryRepository;

    @Mock
    private MenuItemRepository itemRepository;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    private MenuServiceImpl menuService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        CacheManager cacheManager = new ConcurrentMapCacheManager("menuByRestaurant", "menuItem");
        menuService = new MenuServiceImpl(categoryRepository, itemRepository, new MenuMapper(), cacheManager, restaurantServiceClient);
        when(restaurantServiceClient.getRestaurant(1L))
            .thenReturn(new RestaurantSnapshotDto(
                1L,
                1L,
                "Test Restaurant",
                "Test description",
                "Indian",
                "Test address",
                "Bengaluru",
                12.9716,
                77.5946,
                "9999999999",
                4.5,
                5.0,
                true,
                true,
                "APPROVED",
                null,
                null,
                null,
                null,
                100,
                30
            ));
    }

    @Test
    void addItemShouldRejectDiscountedPriceGreaterThanPrice() {
        MenuItemRequest request = new MenuItemRequest(null, 1, 2, "Burger", "Desc", 100.0, 150.0, null, true, true, null, null, null);

        assertThatThrownBy(() -> menuService.addItem(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("discountedPrice");

        verify(itemRepository, never()).save(any(MenuItem.class));
    }

    @Test
    void getVegItemsShouldFilterAvailabilityWhenRequested() {
        MenuCategory category = MenuCategory.builder().categoryId(3).restaurantId(1).name("Main").displayOrder(1).build();
        MenuItem available = MenuItem.builder().itemId(1).restaurantId(1).name("Paneer").price(150.0).isVeg(true).isAvailable(true).category(category).build();
        MenuItem unavailable = MenuItem.builder().itemId(2).restaurantId(1).name("Salad").price(120.0).isVeg(true).isAvailable(false).category(category).build();

        when(itemRepository.findByRestaurantIdAndIsVeg(1, true)).thenReturn(List.of(available, unavailable));

        List<?> result = menuService.getVegItems(1, true);

        assertThat(result).hasSize(1);
    }

    @Test
    void toggleAvailabilityShouldUpdateItemAndReturnResponse() {
        MenuCategory category = MenuCategory.builder().categoryId(3).restaurantId(1).name("Main").displayOrder(1).build();
        MenuItem item = MenuItem.builder().itemId(8).restaurantId(1).name("Pizza").price(200.0).isVeg(false).isAvailable(true).category(category).build();

        when(itemRepository.findById(8)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(MenuItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = menuService.toggleAvailability(8, false);

        assertThat(response.isAvailable()).isFalse();
    }
}
