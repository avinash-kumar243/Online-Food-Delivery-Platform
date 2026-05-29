package com.quickbite.menu.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.menu.dto.ApiResponse;
import com.quickbite.menu.dto.MenuDeleteRequest;
import com.quickbite.menu.dto.MenuEntityType;
import com.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.menu.dto.MenuMutationRequest;
import com.quickbite.menu.dto.RestaurantMenuResponse;
import com.quickbite.menu.dto.ToggleAvailabilityRequest;
import com.quickbite.menu.exception.BadRequestException;
import com.quickbite.menu.service.MenuService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/api/v1/menu")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @PostMapping("/create")
//    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody MenuMutationRequest request) {
        return request.type() == MenuEntityType.CATEGORY
            ? ResponseEntity.status(HttpStatus.CREATED).body(menuService.addCategory(requiredCategory(request)))
            : ResponseEntity.status(HttpStatus.CREATED).body(menuService.addItem(requiredItem(request)));
    }

    @GetMapping("/restaurant/{restaurantId}")
    public ResponseEntity<RestaurantMenuResponse> getMenuByRestaurant(@PathVariable Integer restaurantId) {
        return ResponseEntity.ok(menuService.getMenuByRestaurant(restaurantId));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<MenuItemResponse>> getMenuByCategory(@PathVariable Integer categoryId) {
        return ResponseEntity.ok(menuService.getItemsByCategory(categoryId));
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<MenuItemResponse> getItem(@PathVariable Integer itemId) {
        return ResponseEntity.ok(menuService.getItemById(itemId));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseEntity<?> update(@Valid @RequestBody MenuMutationRequest request) {
        return request.type() == MenuEntityType.CATEGORY
            ? ResponseEntity.ok(menuService.updateCategory(requiredCategory(request)))
            : ResponseEntity.ok(menuService.updateItem(requiredItem(request)));
    }

    @PutMapping("/toggleAvailability")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseEntity<MenuItemResponse> toggleAvailability(@Valid @RequestBody ToggleAvailabilityRequest request) {
        return ResponseEntity.ok(menuService.toggleAvailability(request.itemId(), request.available()));
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasAnyRole('RESTAURANT_OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse> delete(@Valid @RequestBody MenuDeleteRequest request) {
        if (request.type() == MenuEntityType.CATEGORY) {
            if (request.categoryId() == null) {
                throw new BadRequestException("categoryId is required");
            }
            menuService.deleteCategory(request.categoryId());
            return ResponseEntity.ok(ApiResponse.of("Menu category deleted"));
        }

        if (request.itemId() == null) {
            throw new BadRequestException("itemId is required");
        }
        menuService.deleteItem(request.itemId());
        return ResponseEntity.ok(ApiResponse.of("Menu item deleted"));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MenuItemResponse>> search(@RequestParam("query") String query) {
        return ResponseEntity.ok(menuService.searchItems(query));
    }

    @GetMapping("/vegItems")
    public ResponseEntity<List<MenuItemResponse>> getVegItems(
        @RequestParam(value = "restaurantId", required = false) Integer restaurantId,
        @RequestParam(value = "availableOnly", defaultValue = "false") Boolean availableOnly
    ) {
        return ResponseEntity.ok(menuService.getVegItems(restaurantId, availableOnly));
    }

    private com.quickbite.menu.dto.MenuCategoryRequest requiredCategory(MenuMutationRequest request) {
        if (request.category() == null) {
            throw new BadRequestException("Category payload is required");
        }
        return request.category();
    }

    private com.quickbite.menu.dto.MenuItemRequest requiredItem(MenuMutationRequest request) {
        if (request.item() == null) {
            throw new BadRequestException("Item payload is required");
        }
        return request.item();
    }
}