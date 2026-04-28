package com.quickbite.menu.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.menu.client.RestaurantServiceClient;
import com.quickbite.menu.dto.MenuCategoryRequest;
import com.quickbite.menu.dto.MenuCategoryResponse;
import com.quickbite.menu.dto.MenuItemRequest;
import com.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.menu.dto.RestaurantSnapshotDto;
import com.quickbite.menu.dto.RestaurantMenuResponse;
import com.quickbite.menu.entity.MenuCategory;
import com.quickbite.menu.entity.MenuItem;
import com.quickbite.menu.exception.BadRequestException;
import com.quickbite.menu.exception.NotFoundException;
import com.quickbite.menu.repository.MenuCategoryRepository;
import com.quickbite.menu.repository.MenuItemRepository;
import com.quickbite.menu.util.MenuMapper;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private static final String MENU_BY_RESTAURANT_CACHE = "menuByRestaurant";
    private static final String MENU_ITEM_CACHE = "menuItem";
    private static final String MENU_SERVICE_CB = "menuService";

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final MenuMapper mapper;
    private final CacheManager cacheManager;
    private final RestaurantServiceClient restaurantServiceClient;

    @Override
    @Transactional
    public MenuCategoryResponse addCategory(MenuCategoryRequest request) {
        ensureRestaurantApproved(request.restaurantId());
        if (categoryRepository.existsByRestaurantIdAndNameIgnoreCase(request.restaurantId(), request.name())) {
            throw new BadRequestException("Category already exists for restaurant " + request.restaurantId());
        }

        MenuCategory saved = categoryRepository.save(mapper.toEntity(request));
        evictRestaurantCache(saved.getRestaurantId());
        log.info("Created category {} for restaurant {}", saved.getCategoryId(), saved.getRestaurantId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MenuItemResponse addItem(MenuItemRequest request) {
        ensureRestaurantApproved(request.restaurantId());
        validateDiscountedPrice(request.price(), request.discountedPrice());
        MenuCategory category = getCategory(request.categoryId());
        validateRestaurantOwnership(request.restaurantId(), category.getRestaurantId());

        MenuItem saved = itemRepository.save(mapper.toEntity(request, category));
        evictRestaurantCache(saved.getRestaurantId());
        evictItemCache(saved.getItemId());
        log.info("Created menu item {} for restaurant {}", saved.getItemId(), saved.getRestaurantId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MenuCategoryResponse updateCategory(MenuCategoryRequest request) {
        if (request.categoryId() == null) {
            throw new BadRequestException("categoryId is required for update");
        }

        ensureRestaurantApproved(request.restaurantId());
        MenuCategory category = getCategory(request.categoryId());
        Integer previousRestaurantId = category.getRestaurantId();
        mapper.updateEntity(category, request);
        MenuCategory saved = categoryRepository.save(category);
        evictRestaurantCache(previousRestaurantId);
        evictRestaurantCache(saved.getRestaurantId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public MenuItemResponse updateItem(MenuItemRequest request) {
        if (request.itemId() == null) {
            throw new BadRequestException("itemId is required for update");
        }

        ensureRestaurantApproved(request.restaurantId());
        validateDiscountedPrice(request.price(), request.discountedPrice());
        MenuItem item = getItemEntity(request.itemId());
        MenuCategory category = getCategory(request.categoryId());
        validateRestaurantOwnership(request.restaurantId(), category.getRestaurantId());

        Integer previousRestaurantId = item.getRestaurantId();
        mapper.updateEntity(item, request, category);
        MenuItem saved = itemRepository.save(item);
        evictRestaurantCache(previousRestaurantId);
        evictRestaurantCache(saved.getRestaurantId());
        evictItemCache(saved.getItemId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCategory(Integer categoryId) {
        MenuCategory category = getCategory(categoryId);
        Integer restaurantId = category.getRestaurantId();
        categoryRepository.delete(category);
        evictRestaurantCache(restaurantId);
        category.getItems().forEach(item -> evictItemCache(item.getItemId()));
        log.info("Deleted category {} for restaurant {}", categoryId, restaurantId);
    }

    @Override
    @Transactional
    public void deleteItem(Integer itemId) {
        MenuItem item = getItemEntity(itemId);
        Integer restaurantId = item.getRestaurantId();
        itemRepository.delete(item);
        evictRestaurantCache(restaurantId);
        evictItemCache(itemId);
        log.info("Deleted item {} for restaurant {}", itemId, restaurantId);
    }

    @Override
    @CircuitBreaker(name = MENU_SERVICE_CB, fallbackMethod = "getMenuByRestaurantFallback")
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = MENU_BY_RESTAURANT_CACHE, key = "#restaurantId")
    public RestaurantMenuResponse getMenuByRestaurant(Integer restaurantId) {
        log.debug("Loading menu for restaurant {} from database", restaurantId);
        List<MenuCategoryResponse> categories = categoryRepository.findByRestaurantIdOrderByDisplayOrderAscNameAsc(restaurantId).stream()
            .map(category -> mapper.toResponse(category))
            .sorted(Comparator.comparing(MenuCategoryResponse::displayOrder).thenComparing(MenuCategoryResponse::name))
            .toList();

        return new RestaurantMenuResponse(restaurantId, itemRepository.countByRestaurantId(restaurantId), categories);
    }

    @Override
    @CircuitBreaker(name = MENU_SERVICE_CB, fallbackMethod = "getItemsByCategoryFallback")
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getItemsByCategory(Integer categoryId) {
        return itemRepository.findByCategory_CategoryId(categoryId).stream()
            .map(mapper::toResponse)
            .toList();
    }

    @Override
    @CircuitBreaker(name = MENU_SERVICE_CB, fallbackMethod = "getItemByIdFallback")
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = MENU_ITEM_CACHE, key = "#itemId")
    public MenuItemResponse getItemById(Integer itemId) {
        log.debug("Loading menu item {} from database", itemId);
        return mapper.toResponse(getItemEntity(itemId));
    }

    @Override
    @Transactional
    public MenuItemResponse toggleAvailability(Integer itemId, Boolean available) {
        MenuItem item = getItemEntity(itemId);
        item.setIsAvailable(available);
        MenuItem saved = itemRepository.save(item);
        evictRestaurantCache(saved.getRestaurantId());
        evictItemCache(saved.getItemId());
        return mapper.toResponse(saved);
    }

    @Override
    @CircuitBreaker(name = MENU_SERVICE_CB, fallbackMethod = "searchItemsFallback")
    @Transactional(readOnly = true)
    public List<MenuItemResponse> searchItems(String query) {
        if (query == null || query.isBlank()) {
            throw new BadRequestException("query must not be blank");
        }

        return itemRepository.findByNameContainingIgnoreCase(query.trim()).stream()
            .map(mapper::toResponse)
            .toList();
    }

    @Override
    @CircuitBreaker(name = MENU_SERVICE_CB, fallbackMethod = "getVegItemsFallback")
    @Transactional(readOnly = true)
    public List<MenuItemResponse> getVegItems(Integer restaurantId, Boolean availableOnly) {
        List<MenuItem> items = restaurantId == null
            ? itemRepository.findByIsVeg(true)
            : itemRepository.findByRestaurantIdAndIsVeg(restaurantId, true);

        return items.stream()
            .filter(item -> !Boolean.TRUE.equals(availableOnly) || Boolean.TRUE.equals(item.getIsAvailable()))
            .map(mapper::toResponse)
            .toList();
    }

    public RestaurantMenuResponse getMenuByRestaurantFallback(Integer restaurantId, Throwable throwable) {
        log.warn("Falling back for restaurant menu {} due to {}", restaurantId, throwable.getMessage());
        return new RestaurantMenuResponse(restaurantId, 0, List.of());
    }

    public List<MenuItemResponse> getItemsByCategoryFallback(Integer categoryId, Throwable throwable) {
        log.warn("Falling back for category {} due to {}", categoryId, throwable.getMessage());
        return List.of();
    }

    public MenuItemResponse getItemByIdFallback(Integer itemId, Throwable throwable) {
        throw new NotFoundException("Menu item unavailable: " + itemId);
    }

    public List<MenuItemResponse> searchItemsFallback(String query, Throwable throwable) {
        log.warn("Falling back for search query {} due to {}", query, throwable.getMessage());
        return List.of();
    }

    public List<MenuItemResponse> getVegItemsFallback(Integer restaurantId, Boolean availableOnly, Throwable throwable) {
        log.warn("Falling back for veg filter due to {}", throwable.getMessage());
        return List.of();
    }

    private MenuCategory getCategory(Integer categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new NotFoundException("Menu category not found with id: " + categoryId));
    }

    private MenuItem getItemEntity(Integer itemId) {
        return itemRepository.findById(itemId)
            .orElseThrow(() -> new NotFoundException("Menu item not found with id: " + itemId));
    }

    private void validateDiscountedPrice(Double price, Double discountedPrice) {
        if (discountedPrice != null && discountedPrice >= price) {
            throw new BadRequestException("discountedPrice must be less than price");
        }
    }

    private void validateRestaurantOwnership(Integer restaurantId, Integer categoryRestaurantId) {
        if (!Objects.equals(restaurantId, categoryRestaurantId)) {
            throw new BadRequestException("restaurantId must match the category restaurant");
        }
    }

    private void ensureRestaurantApproved(Integer restaurantId) {
        RestaurantSnapshotDto restaurant = restaurantServiceClient.getRestaurant(restaurantId.longValue());
        if (restaurant == null || !Boolean.TRUE.equals(restaurant.isApproved())) {
            throw new BadRequestException("Menu management is available only for approved restaurants");
        }
    }

    private void evictRestaurantCache(Integer restaurantId) {
        Cache cache = cacheManager.getCache(MENU_BY_RESTAURANT_CACHE);
        if (cache != null && restaurantId != null) {
            cache.evict(restaurantId);
        }
    }

    private void evictItemCache(Integer itemId) {
        Cache cache = cacheManager.getCache(MENU_ITEM_CACHE);
        if (cache != null && itemId != null) {
            cache.evict(itemId);
        }
    }
}
