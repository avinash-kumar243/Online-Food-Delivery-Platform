package com.quickbite.menu.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import com.quickbite.menu.client.RestaurantServiceClient;
import com.quickbite.menu.config.MenuMapper;
import com.quickbite.menu.dto.MenuCategoryRequest;
import com.quickbite.menu.dto.MenuCategoryResponse;
import com.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.menu.dto.RestaurantMenuResponse;
import com.quickbite.menu.dto.RestaurantSnapshotDto;
import com.quickbite.menu.entity.MenuCategory;
import com.quickbite.menu.entity.MenuItem;
import com.quickbite.menu.exception.BadRequestException;
import com.quickbite.menu.exception.MenuNotFoundException;
import com.quickbite.menu.exception.NotFoundException;
import com.quickbite.menu.repository.MenuCategoryRepository;
import com.quickbite.menu.repository.MenuItemRepository;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private MenuCategoryRepository categoryRepository;

    @Mock
    private MenuItemRepository itemRepository;

    @Mock
    private RestaurantServiceClient restaurantServiceClient;

    private MenuServiceImpl menuService;

    private MenuCategoryRequest categoryRequest;
    private MenuItemRequest itemRequest;
    private MenuCategory category;
    private MenuItem item;
    private RestaurantSnapshotDto approvedRestaurant;

    @BeforeEach
    void setUp() {
        menuService = new MenuServiceImpl(
            categoryRepository,
            itemRepository,
            new MenuMapper(),
            new ConcurrentMapCacheManager("menuByRestaurant", "menuItem"),
            restaurantServiceClient
        );

        categoryRequest = new MenuCategoryRequest(1, 10, "Starters", "Starter items", "http://img", 1);
        itemRequest = new MenuItemRequest(1, 10, 1, "Paneer Tikka", "Spicy starter", 250.0, 220.0, "http://img", true, true, 4.5, 350, "starter");
        approvedRestaurant = new RestaurantSnapshotDto(
            10L, 11L, "QuickBite", "Desc", "Indian", "Addr", "Pune",
            18.52, 73.85, "9999999999", 4.4, 6.0, true, true, "APPROVED",
            null, null, null, null, 200, 30
        );

        category = MenuCategory.builder()
            .categoryId(1)
            .restaurantId(10)
            .name("Starters")
            .description("Starter items")
            .imageUrl("http://img")
            .displayOrder(1)
            .items(new ArrayList<>())
            .build();

        item = MenuItem.builder()
            .itemId(1)
            .restaurantId(10)
            .name("Paneer Tikka")
            .description("Spicy starter")
            .price(250.0)
            .discountedPrice(220.0)
            .imageUrl("http://img")
            .isVeg(true)
            .isAvailable(true)
            .rating(4.5)
            .calories(350)
            .tags("starter")
            .category(category)
            .build();
        category.setItems(new ArrayList<>(List.of(item)));
    }

    @Test
    @DisplayName("Add Category - Success")
    void addCategory_Success() {
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(approvedRestaurant);
        when(categoryRepository.existsByRestaurantIdAndNameIgnoreCase(10, "Starters")).thenReturn(false);
        when(categoryRepository.save(any(MenuCategory.class))).thenReturn(category);

        MenuCategoryResponse response = menuService.addCategory(categoryRequest);

        assertEquals("Starters", response.name());
        verify(categoryRepository).save(any(MenuCategory.class));
    }

    @Test
    @DisplayName("Add Category - Duplicate Name")
    void addCategory_Duplicate() {
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(approvedRestaurant);
        when(categoryRepository.existsByRestaurantIdAndNameIgnoreCase(10, "Starters")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> menuService.addCategory(categoryRequest));
    }

    @Test
    @DisplayName("Add Category - Restaurant Not Approved")
    void addCategory_RestaurantNotApproved() {
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(
            new RestaurantSnapshotDto(10L, 11L, "QuickBite", "Desc", "Indian", "Addr", "Pune",
                18.52, 73.85, "9999999999", 4.4, 6.0, true, false, "PENDING", null, null, null, null, 200, 30)
        );

        assertThrows(BadRequestException.class, () -> menuService.addCategory(categoryRequest));
    }

    @Test
    @DisplayName("Add Item - Success")
    void addItem_Success() {
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(approvedRestaurant);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(itemRepository.save(any(MenuItem.class))).thenReturn(item);

        MenuItemResponse response = menuService.addItem(itemRequest);

        assertEquals("Paneer Tikka", response.name());
        verify(itemRepository).save(any(MenuItem.class));
    }

    @Test
    @DisplayName("Add Item - Price Below Restaurant Minimum")
    void addItem_PriceBelowMinimum() {
        MenuItemRequest request = new MenuItemRequest(1, 10, 1, "Paneer Tikka", "Spicy starter", 150.0, 120.0, "http://img", true, true, 4.5, 350, "starter");
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(approvedRestaurant);

        assertThrows(BadRequestException.class, () -> menuService.addItem(request));
    }

    @Test
    @DisplayName("Add Item - Discounted Price Must Be Less Than Price")
    void addItem_DiscountedPriceGreaterThanPrice() {
        MenuItemRequest request = new MenuItemRequest(1, 10, 1, "Paneer Tikka", "Spicy starter", 250.0, 260.0, "http://img", true, true, 4.5, 350, "starter");
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(approvedRestaurant);

        assertThrows(BadRequestException.class, () -> menuService.addItem(request));
    }

    @Test
    @DisplayName("Add Item - Category Restaurant Mismatch")
    void addItem_RestaurantMismatch() {
        MenuCategory otherCategory = MenuCategory.builder().categoryId(1).restaurantId(99).name("Other").displayOrder(1).items(new ArrayList<>()).build();
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(approvedRestaurant);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(otherCategory));

        assertThrows(BadRequestException.class, () -> menuService.addItem(itemRequest));
    }

    @Test
    @DisplayName("Update Category - Missing Category Id")
    void updateCategory_MissingId() {
        MenuCategoryRequest request = new MenuCategoryRequest(null, 10, "Starters", "Starter items", "http://img", 1);

        assertThrows(BadRequestException.class, () -> menuService.updateCategory(request));
    }

    @Test
    @DisplayName("Update Category - Success")
    void updateCategory_Success() {
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(approvedRestaurant);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        MenuCategoryResponse response = menuService.updateCategory(categoryRequest);

        assertEquals(1, response.categoryId());
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("Update Category - Missing Category Throws MenuNotFoundException")
    void updateCategory_NotFound() {
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(approvedRestaurant);
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        MenuNotFoundException exception = assertThrows(MenuNotFoundException.class,
            () -> menuService.updateCategory(categoryRequest));

        assertEquals("Menu category not found with id: 1", exception.getMessage());
    }

    @Test
    @DisplayName("Update Item - Missing Item Id")
    void updateItem_MissingId() {
        MenuItemRequest request = new MenuItemRequest(null, 10, 1, "Paneer Tikka", "Spicy starter", 250.0, 220.0, "http://img", true, true, 4.5, 350, "starter");

        assertThrows(BadRequestException.class, () -> menuService.updateItem(request));
    }

    @Test
    @DisplayName("Update Item - Success")
    void updateItem_Success() {
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(approvedRestaurant);
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(itemRepository.save(item)).thenReturn(item);

        MenuItemResponse response = menuService.updateItem(itemRequest);

        assertEquals(1, response.itemId());
        verify(itemRepository).save(item);
    }

    @Test
    @DisplayName("Update Item - Missing Item Throws MenuNotFoundException")
    void updateItem_NotFound() {
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(approvedRestaurant);
        when(itemRepository.findById(1)).thenReturn(Optional.empty());

        MenuNotFoundException exception = assertThrows(MenuNotFoundException.class,
            () -> menuService.updateItem(itemRequest));

        assertEquals("Menu item not found with id: 1", exception.getMessage());
        verify(categoryRepository, never()).findById(anyInt());
    }

    @Test
    @DisplayName("Delete Category - Success")
    void deleteCategory_Success() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        menuService.deleteCategory(1);

        verify(categoryRepository).delete(category);
    }

    @Test
    @DisplayName("Delete Category - Missing Category Throws MenuNotFoundException")
    void deleteCategory_NotFound() {
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        MenuNotFoundException exception = assertThrows(MenuNotFoundException.class,
            () -> menuService.deleteCategory(99));

        assertEquals("Menu category not found with id: 99", exception.getMessage());
        verify(categoryRepository, never()).delete(any(MenuCategory.class));
    }

    @Test
    @DisplayName("Delete Item - Success")
    void deleteItem_Success() {
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        menuService.deleteItem(1);

        verify(itemRepository).delete(item);
    }

    @Test
    @DisplayName("Delete Item - Missing Item Throws MenuNotFoundException")
    void deleteItem_NotFound() {
        when(itemRepository.findById(99)).thenReturn(Optional.empty());

        MenuNotFoundException exception = assertThrows(MenuNotFoundException.class,
            () -> menuService.deleteItem(99));

        assertEquals("Menu item not found with id: 99", exception.getMessage());
        verify(itemRepository, never()).delete(any(MenuItem.class));
    }

    @Test
    @DisplayName("Get Menu By Restaurant - Success")
    void getMenuByRestaurant_Success() {
        when(categoryRepository.findByRestaurantIdOrderByDisplayOrderAscNameAsc(10)).thenReturn(List.of(category));
        when(itemRepository.countByRestaurantId(10)).thenReturn(1);

        RestaurantMenuResponse response = menuService.getMenuByRestaurant(10);

        assertEquals(10, response.restaurantId());
        assertEquals(1, response.totalItems());
        assertEquals(1, response.categories().size());
    }

    @Test
    @DisplayName("Get Items By Category - Success")
    void getItemsByCategory_Success() {
        when(itemRepository.findByCategory_CategoryId(1)).thenReturn(List.of(item));

        List<MenuItemResponse> response = menuService.getItemsByCategory(1);

        assertEquals(1, response.size());
        assertEquals("Paneer Tikka", response.get(0).name());
    }

    @Test
    @DisplayName("Get Item By Id - Success")
    void getItemById_Success() {
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));

        MenuItemResponse response = menuService.getItemById(1);

        assertEquals(1, response.itemId());
    }

    @Test
    @DisplayName("Get Item By Id - Missing Item Throws MenuNotFoundException")
    void getItemById_NotFound() {
        when(itemRepository.findById(99)).thenReturn(Optional.empty());

        MenuNotFoundException exception = assertThrows(MenuNotFoundException.class,
            () -> menuService.getItemById(99));

        assertEquals("Menu item not found with id: 99", exception.getMessage());
    }

    @Test
    @DisplayName("Toggle Availability - Success")
    void toggleAvailability_Success() {
        when(itemRepository.findById(1)).thenReturn(Optional.of(item));
        when(itemRepository.save(item)).thenReturn(item);

        MenuItemResponse response = menuService.toggleAvailability(1, false);

        assertFalse(item.getIsAvailable());
        assertEquals(1, response.itemId());
    }

    @Test
    @DisplayName("Toggle Availability - Missing Item Throws MenuNotFoundException")
    void toggleAvailability_NotFound() {
        when(itemRepository.findById(77)).thenReturn(Optional.empty());

        MenuNotFoundException exception = assertThrows(MenuNotFoundException.class,
            () -> menuService.toggleAvailability(77, false));

        assertEquals("Menu item not found with id: 77", exception.getMessage());
    }

    @Test
    @DisplayName("Search Items - Blank Query")
    void searchItems_BlankQuery() {
        assertThrows(BadRequestException.class, () -> menuService.searchItems(" "));
        verify(itemRepository, never()).findByNameContainingIgnoreCase(any());
    }

    @Test
    @DisplayName("Search Items - Success")
    void searchItems_Success() {
        when(itemRepository.findByNameContainingIgnoreCase("paneer")).thenReturn(List.of(item));

        List<MenuItemResponse> response = menuService.searchItems(" paneer ");

        assertEquals(1, response.size());
        assertEquals("Paneer Tikka", response.get(0).name());
    }

    @Test
    @DisplayName("Get Veg Items - For All Restaurants")
    void getVegItems_AllRestaurants() {
        when(itemRepository.findByIsVeg(true)).thenReturn(List.of(item));

        List<MenuItemResponse> response = menuService.getVegItems(null, false);

        assertEquals(1, response.size());
        verify(itemRepository).findByIsVeg(true);
    }

    @Test
    @DisplayName("Get Veg Items - Restaurant Specific Available Only")
    void getVegItems_RestaurantSpecificAvailableOnly() {
        MenuItem unavailable = MenuItem.builder()
            .itemId(2).restaurantId(10).name("Salad").price(210.0).isVeg(true).isAvailable(false).category(category)
            .build();
        when(itemRepository.findByRestaurantIdAndIsVeg(10, true)).thenReturn(List.of(item, unavailable));

        List<MenuItemResponse> response = menuService.getVegItems(10, true);

        assertEquals(1, response.size());
        assertEquals("Paneer Tikka", response.get(0).name());
    }

    @Test
    @DisplayName("Fallbacks - Return Safe Defaults")
    void fallbacks_ReturnDefaults() {
        assertEquals(0, menuService.getMenuByRestaurantFallback(10, new RuntimeException("down")).totalItems());
        assertTrue(menuService.getItemsByCategoryFallback(1, new RuntimeException("down")).isEmpty());
        assertTrue(menuService.searchItemsFallback("paneer", new RuntimeException("down")).isEmpty());
        assertTrue(menuService.getVegItemsFallback(10, true, new RuntimeException("down")).isEmpty());
        MenuNotFoundException exception = assertThrows(MenuNotFoundException.class,
            () -> menuService.getItemByIdFallback(1, new RuntimeException("down")));
        assertEquals("Menu item unavailable: 1", exception.getMessage());
    }

    @Test
    @DisplayName("Get Category And Item - Not Found")
    void entityLookup_NotFound() {
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());
        when(itemRepository.findById(99)).thenReturn(Optional.empty());
        when(restaurantServiceClient.getRestaurant(10L)).thenReturn(approvedRestaurant);

        assertThrows(MenuNotFoundException.class, () -> menuService.updateCategory(new MenuCategoryRequest(99, 10, "Starters", "desc", "img", 1)));
        assertThrows(MenuNotFoundException.class, () -> menuService.getItemById(99));
    }
}
