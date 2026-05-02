package com.quickbite.cartservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.quickbite.cartservice.dto.AddCartItemRequest;
import com.quickbite.cartservice.dto.MenuItemSnapshotDto;
import com.quickbite.cartservice.dto.UpdateCartItemQuantityRequest;
import com.quickbite.cartservice.entity.Cart;
import com.quickbite.cartservice.entity.CartItem;
import com.quickbite.cartservice.exception.BadRequestException;
import com.quickbite.cartservice.repository.CartItemRepository;
import com.quickbite.cartservice.repository.CartRepository;
import com.quickbite.cartservice.client.MenuServiceClient;

class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private MenuServiceClient menuServiceClient;

    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cartService = new CartServiceImpl(cartRepository, cartItemRepository, menuServiceClient);
    }

    @Test
    void addItemToCartShouldCreateCartAndCalculateTotal() {
        AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 100L, "Burger", 120.0, 2, "Extra cheese");

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.empty());
        when(menuServiceClient.getMenuItem(100)).thenReturn(
            new MenuItemSnapshotDto(100, 10, 1, "Burger", "Loaded burger", 120.0, null, null, false, true, null, null, null, null)
        );
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setCartId(1L);
            if (!cart.getItems().isEmpty()) {
                cart.getItems().get(0).setItemId(11L);
            }
            return cart;
        });

        var response = cartService.addItemToCart(request);

        assertThat(response.cartId()).isEqualTo(1L);
        assertThat(response.restaurantId()).isEqualTo(10L);
        assertThat(response.totalPrice()).isEqualTo(240.0);
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void addItemToCartShouldRejectDifferentRestaurantItems() {
        CartItem oldItem = CartItem.builder()
            .itemId(1L)
            .menuItemId(20L)
            .name("Pizza")
            .price(200.0)
            .quantity(1)
            .customization("Thin crust")
            .build();

        Cart existingCart = Cart.builder()
            .cartId(5L)
            .customerId(2L)
            .restaurantId(99L)
            .totalPrice(200.0)
            .items(new ArrayList<>(List.of(oldItem)))
            .build();
        oldItem.setCart(existingCart);

        AddCartItemRequest request = new AddCartItemRequest(2L, 50L, 101L, "Pasta", 180.0, 1, null);

        when(cartRepository.findByCustomerId(2L)).thenReturn(Optional.of(existingCart));
        when(menuServiceClient.getMenuItem(101)).thenReturn(
            new MenuItemSnapshotDto(101, 50, 2, "Pasta", "Fresh pasta", 180.0, null, null, false, true, null, null, null, null)
        );

        assertThatThrownBy(() -> cartService.addItemToCart(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("another restaurant");
    }

    @Test
    void updateItemQuantityShouldRecalculateTotal() {
        CartItem item = CartItem.builder()
            .itemId(7L)
            .menuItemId(100L)
            .name("Wrap")
            .price(90.0)
            .quantity(1)
            .build();

        Cart cart = Cart.builder()
            .cartId(3L)
            .customerId(7L)
            .restaurantId(11L)
            .totalPrice(90.0)
            .items(new ArrayList<>(List.of(item)))
            .build();
        item.setCart(cart);

        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cartService.updateItemQuantity(new UpdateCartItemQuantityRequest(7L, 7L, 3));

        assertThat(response.totalPrice()).isEqualTo(270.0);
        assertThat(response.items().get(0).quantity()).isEqualTo(3);
    }

    @Test
    void updateItemQuantityByMenuItemShouldFallbackToCartItemId() {
        CartItem item = CartItem.builder()
            .itemId(2L)
            .menuItemId(100L)
            .name("Biryani")
            .price(180.0)
            .quantity(3)
            .build();

        Cart cart = Cart.builder()
            .cartId(6L)
            .customerId(1L)
            .restaurantId(10L)
            .totalPrice(540.0)
            .items(new ArrayList<>(List.of(item)))
            .build();
        item.setCart(cart);

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cartService.updateItemQuantity(1L, 2L, 2);

        assertThat(response.totalPrice()).isEqualTo(360.0);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void applyPromoCodeShouldRejectBlankCode() {
        assertThatThrownBy(() -> cartService.applyPromoCode(1L, " "))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("promoCode");
    }

    @Test
    void clearCartShouldDeleteExistingCart() {
        Cart cart = Cart.builder()
            .cartId(4L)
            .customerId(8L)
            .items(new ArrayList<>())
            .build();

        when(cartRepository.findByCustomerId(8L)).thenReturn(Optional.of(cart));

        cartService.clearCart(8L);

        verify(cartRepository).delete(cart);
    }

    @Test
    void clearCartShouldDoNothingWhenCartDoesNotExist() {
        when(cartRepository.findByCustomerId(9L)).thenReturn(Optional.empty());

        cartService.clearCart(9L);

        verify(cartRepository, never()).delete(any(Cart.class));
    }
}
