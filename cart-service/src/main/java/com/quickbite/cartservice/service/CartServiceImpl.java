package com.quickbite.cartservice.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.quickbite.cartservice.client.MenuServiceClient;
import com.quickbite.cartservice.client.RestaurantServiceClient;
import com.quickbite.cartservice.dto.AddCartItemRequest;
import com.quickbite.cartservice.dto.CartItemResponse;
import com.quickbite.cartservice.dto.CartResponse;
import com.quickbite.cartservice.dto.MenuItemSnapshotDto;
import com.quickbite.cartservice.dto.RestaurantSnapshotDto;
import com.quickbite.cartservice.dto.UpdateCartItemQuantityRequest;
import com.quickbite.cartservice.entity.Cart;
import com.quickbite.cartservice.entity.CartItem;
import com.quickbite.cartservice.exception.BadRequestException;
import com.quickbite.cartservice.exception.CartItemNotFoundException;
import com.quickbite.cartservice.repository.CartItemRepository;
import com.quickbite.cartservice.repository.CartRepository;

@Service
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuServiceClient menuServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;

    @Autowired
    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           MenuServiceClient menuServiceClient,
                           RestaurantServiceClient restaurantServiceClient) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.menuServiceClient = menuServiceClient;
        this.restaurantServiceClient = restaurantServiceClient;
    }

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           MenuServiceClient menuServiceClient) {
        this(cartRepository, cartItemRepository, menuServiceClient, null);
    }

    @Override
    @Transactional
    public CartResponse getCartByCustomerId(Long customerId) {
        return cartRepository.findByCustomerId(customerId)
            .map(this::refreshCartSnapshot)
            .orElseGet(() -> new CartResponse(null, customerId, null, 0.0, List.of()));
    }

    @Override
    @Transactional
    public CartResponse addItemToCart(AddCartItemRequest request) {
        MenuItemSnapshotDto menuItem = menuServiceClient.getMenuItem(request.menuItemId().intValue());
        if (!Boolean.TRUE.equals(menuItem.isAvailable())) {
            throw new BadRequestException("The selected menu item is currently unavailable");
        }

        Long restaurantId = menuItem.restaurantId().longValue();
        ensureRestaurantOpen(restaurantId);
        Cart cart = cartRepository.findByCustomerId(request.customerId())
            .orElseGet(() -> createCart(request.customerId()));

        if (cart.getRestaurantId() == null) {
            cart.setRestaurantId(restaurantId);
        } else if (!Objects.equals(cart.getRestaurantId(), restaurantId)) {
            cart.getItems().clear();
            cart.setRestaurantId(restaurantId);
            cart.setTotalPrice(0.0);
        }

        CartItem existingItem = cart.getItems().stream()
            .filter(item -> Objects.equals(item.getMenuItemId(), request.menuItemId()))
            .filter(item -> Objects.equals(normalize(item.getCustomization()), normalize(request.customization())))
            .findFirst()
            .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.quantity());
            existingItem.setPrice(resolvePrice(request, menuItem));
            existingItem.setName(resolveName(request, menuItem));
        } else {
            cart.getItems().add(CartItem.builder()
                .menuItemId(request.menuItemId())
                .name(resolveName(request, menuItem))
                .price(resolvePrice(request, menuItem))
                .quantity(request.quantity())
                .customization(request.customization())
                .cart(cart)
                .build());
        }

        recalculateTotal(cart);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(UpdateCartItemQuantityRequest request) {
        Cart cart = cartRepository.findByCustomerId(request.customerId())
            .orElseThrow(() -> new CartItemNotFoundException(request.itemId()));

        CartItem item = cart.getItems().stream()
            .filter(cartItem -> Objects.equals(cartItem.getItemId(), request.itemId()))
            .findFirst()
            .orElseThrow(() -> new CartItemNotFoundException(request.itemId()));

        item.setQuantity(request.quantity());
        recalculateTotal(cart);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(Long customerId, Long menuItemId, Integer quantity) {
        Cart cart = cartRepository.findByCustomerId(customerId)
            .orElseThrow(() -> new CartItemNotFoundException(menuItemId));

        CartItem item = cart.getItems().stream()
            .filter(cartItem -> Objects.equals(cartItem.getMenuItemId(), menuItemId)
                || Objects.equals(cartItem.getItemId(), menuItemId))
            .findFirst()
            .orElseThrow(() -> new CartItemNotFoundException(menuItemId));

        item.setQuantity(quantity);
        recalculateTotal(cart);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long itemId) {
        CartItem item = cartItemRepository.findById(itemId)
            .orElseThrow(() -> new CartItemNotFoundException(itemId));

        Cart cart = item.getCart();
        cart.getItems().remove(item);
        resetRestaurantIfEmpty(cart);
        recalculateTotal(cart);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(Long customerId, Long menuItemId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
            .orElseThrow(() -> new CartItemNotFoundException(menuItemId));

        CartItem item = cart.getItems().stream()
            .filter(cartItem -> Objects.equals(cartItem.getMenuItemId(), menuItemId))
            .findFirst()
            .orElseThrow(() -> new CartItemNotFoundException(menuItemId));

        cart.getItems().remove(item);
        resetRestaurantIfEmpty(cart);
        recalculateTotal(cart);
        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse applyPromoCode(Long customerId, String promoCode) {
        if (!StringUtils.hasText(promoCode)) {
            throw new BadRequestException("promoCode must not be blank");
        }

        Cart cart = cartRepository.findByCustomerId(customerId)
            .orElseGet(() -> createCart(customerId));

        return toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public void clearCart(Long customerId) {
        cartRepository.findByCustomerId(customerId).ifPresent(cartRepository::delete);
    }

    private Cart createCart(Long customerId) {
        return Cart.builder()
            .customerId(customerId)
            .items(new ArrayList<>())
            .totalPrice(0.0)
            .build();
    }

    private void resetRestaurantIfEmpty(Cart cart) {
        if (cart.getItems().isEmpty()) {
            cart.setRestaurantId(null);
        }
    }

    private void recalculateTotal(Cart cart) {
        double total = cart.getItems().stream()
            .mapToDouble(item -> item.getPrice() * item.getQuantity())
            .sum();

        cart.setTotalPrice(total);
    }

    private CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
            .map(item -> new CartItemResponse(
                item.getItemId(),
                item.getMenuItemId(),
                item.getName(),
                item.getPrice(),
                item.getQuantity(),
                item.getCustomization(),
                item.getPrice() * item.getQuantity()
            ))
            .toList();

        return new CartResponse(
            cart.getCartId(),
            cart.getCustomerId(),
            cart.getRestaurantId(),
            cart.getTotalPrice(),
            items
        );
    }

    private CartResponse refreshCartSnapshot(Cart cart) {
        boolean changed = false;

        for (CartItem item : cart.getItems()) {
            try {
                MenuItemSnapshotDto menuItem = menuServiceClient.getMenuItem(item.getMenuItemId().intValue());
                double resolvedPrice = resolveSnapshotPrice(menuItem);
                String resolvedName = menuItem.name();

                if (!Objects.equals(item.getName(), resolvedName)) {
                    item.setName(resolvedName);
                    changed = true;
                }

                if (Double.compare(item.getPrice(), resolvedPrice) != 0) {
                    item.setPrice(resolvedPrice);
                    changed = true;
                }
            } catch (Exception ignored) {
                // Keep the persisted snapshot if the live menu item is temporarily unavailable.
            }
        }

        if (changed) {
            recalculateTotal(cart);
            cart = cartRepository.save(cart);
        }

        return toResponse(cart);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private double resolvePrice(AddCartItemRequest request, MenuItemSnapshotDto menuItem) {
        return resolveSnapshotPrice(menuItem);
    }

    private double resolveSnapshotPrice(MenuItemSnapshotDto menuItem) {
        return menuItem.discountedPrice() != null && menuItem.discountedPrice() > 0
            ? menuItem.discountedPrice()
            : menuItem.price();
    }

    private void ensureRestaurantOpen(Long restaurantId) {
        if (restaurantServiceClient == null) {
            return;
        }
        RestaurantSnapshotDto restaurant = restaurantServiceClient.getRestaurantById(restaurantId);
        if (restaurant == null || !Boolean.TRUE.equals(restaurant.isApproved()) || !Boolean.TRUE.equals(restaurant.isOpen())) {
            throw new BadRequestException("The selected restaurant is currently unavailable");
        }
    }

    private String resolveName(AddCartItemRequest request, MenuItemSnapshotDto menuItem) {
        if (StringUtils.hasText(request.name())) {
            return request.name().trim();
        }

        return menuItem.name();
    }
}
