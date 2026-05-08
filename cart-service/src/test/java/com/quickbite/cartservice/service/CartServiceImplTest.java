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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickbite.cartservice.client.MenuServiceClient;
import com.quickbite.cartservice.dto.AddCartItemRequest;
import com.quickbite.cartservice.dto.MenuItemSnapshotDto;
import com.quickbite.cartservice.dto.UpdateCartItemQuantityRequest;
import com.quickbite.cartservice.entity.Cart;
import com.quickbite.cartservice.entity.CartItem;
import com.quickbite.cartservice.exception.BadRequestException;
import com.quickbite.cartservice.exception.CartItemNotFoundException;
import com.quickbite.cartservice.repository.CartItemRepository;
import com.quickbite.cartservice.repository.CartRepository;

@ExtendWith(MockitoExtension.class)
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
        cartService = new CartServiceImpl(cartRepository, cartItemRepository, menuServiceClient);
    }

    @Test
    void getCartByCustomerId_WhenCartMissing_ReturnsEmptyResponse() {
        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.empty());

        var response = cartService.getCartByCustomerId(1L);

        assertThat(response.cartId()).isNull();
        assertThat(response.customerId()).isEqualTo(1L);
        assertThat(response.restaurantId()).isNull();
        assertThat(response.totalPrice()).isEqualTo(0.0);
        assertThat(response.items()).isEmpty();
    }

    @Test
    void getCartByCustomerId_WhenSnapshotChanges_RefreshesAndSavesCart() {
        CartItem item = cartItem(11L, 100L, "Old Burger", 120.0, 2, "Extra cheese");
        Cart cart = cart(5L, 1L, 10L, List.of(item));
        cart.setTotalPrice(240.0);

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(menuServiceClient.getMenuItem(100)).thenReturn(menuItemSnapshot(100, 10, "New Burger", 150.0, 130.0, true));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cartService.getCartByCustomerId(1L);

        assertThat(response.totalPrice()).isEqualTo(260.0);
        assertThat(response.items()).singleElement().satisfies(cartItem -> {
            assertThat(cartItem.name()).isEqualTo("New Burger");
            assertThat(cartItem.price()).isEqualTo(130.0);
            assertThat(cartItem.lineTotal()).isEqualTo(260.0);
        });
        verify(cartRepository).save(cart);
    }

    @Test
    void getCartByCustomerId_WhenMenuLookupFails_KeepsPersistedSnapshot() {
        CartItem item = cartItem(11L, 100L, "Saved Burger", 120.0, 2, null);
        Cart cart = cart(5L, 1L, 10L, List.of(item));
        cart.setTotalPrice(240.0);

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(menuServiceClient.getMenuItem(100)).thenThrow(new RuntimeException("menu unavailable"));

        var response = cartService.getCartByCustomerId(1L);

        assertThat(response.totalPrice()).isEqualTo(240.0);
        assertThat(response.items()).singleElement().satisfies(cartItem -> {
            assertThat(cartItem.name()).isEqualTo("Saved Burger");
            assertThat(cartItem.price()).isEqualTo(120.0);
        });
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addItemToCart_WhenCartMissing_CreatesCartAndCalculatesTotal() {
        AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 100L, "Burger", 120.0, 2, "Extra cheese");

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.empty());
        when(menuServiceClient.getMenuItem(100)).thenReturn(menuItemSnapshot(100, 10, "Burger", 120.0, null, true));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            cart.setCartId(1L);
            cart.getItems().get(0).setItemId(11L);
            return cart;
        });

        var response = cartService.addItemToCart(request);

        assertThat(response.cartId()).isEqualTo(1L);
        assertThat(response.restaurantId()).isEqualTo(10L);
        assertThat(response.totalPrice()).isEqualTo(240.0);
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void addItemToCart_WhenSameItemAndCustomization_MergesQuantity() {
        CartItem existingItem = cartItem(11L, 100L, "Burger", 120.0, 1, "Extra cheese");
        Cart existingCart = cart(5L, 1L, 10L, List.of(existingItem));
        existingCart.setTotalPrice(120.0);
        AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 100L, "Burger", 120.0, 2, "Extra cheese");

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(existingCart));
        when(menuServiceClient.getMenuItem(100)).thenReturn(menuItemSnapshot(100, 10, "Burger", 120.0, 90.0, true));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cartService.addItemToCart(request);

        assertThat(response.totalPrice()).isEqualTo(270.0);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.quantity()).isEqualTo(3);
            assertThat(item.price()).isEqualTo(90.0);
        });
    }

    @Test
    void addItemToCart_WhenMenuItemUnavailable_ThrowsBadRequest() {
        AddCartItemRequest request = new AddCartItemRequest(1L, 10L, 100L, "Burger", 120.0, 1, null);

        when(menuServiceClient.getMenuItem(100)).thenReturn(menuItemSnapshot(100, 10, "Burger", 120.0, null, false));

        assertThatThrownBy(() -> cartService.addItemToCart(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("The selected menu item is currently unavailable");

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addItemToCart_WhenRestaurantDiffers_ThrowsBadRequest() {
        CartItem oldItem = cartItem(1L, 20L, "Pizza", 200.0, 1, "Thin crust");
        Cart existingCart = cart(5L, 2L, 99L, List.of(oldItem));
        existingCart.setTotalPrice(200.0);
        AddCartItemRequest request = new AddCartItemRequest(2L, 50L, 101L, "Pasta", 180.0, 1, null);

        when(cartRepository.findByCustomerId(2L)).thenReturn(Optional.of(existingCart));
        when(menuServiceClient.getMenuItem(101)).thenReturn(menuItemSnapshot(101, 50, "Pasta", 180.0, null, true));

        assertThatThrownBy(() -> cartService.addItemToCart(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("another restaurant");
    }

    @Test
    void updateItemQuantity_WhenItemExists_RecalculatesTotal() {
        CartItem item = cartItem(7L, 100L, "Wrap", 90.0, 1, null);
        Cart cart = cart(3L, 7L, 11L, List.of(item));
        cart.setTotalPrice(90.0);

        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cartService.updateItemQuantity(new UpdateCartItemQuantityRequest(7L, 7L, 3));

        assertThat(response.totalPrice()).isEqualTo(270.0);
        assertThat(response.items().get(0).quantity()).isEqualTo(3);
    }

    @Test
    void updateItemQuantity_WhenCartMissing_ThrowsCustomException() {
        when(cartRepository.findByCustomerId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.updateItemQuantity(new UpdateCartItemQuantityRequest(7L, 7L, 3)))
            .isInstanceOf(CartItemNotFoundException.class)
            .hasMessage("Cart item not found with id: 7");
    }

    @Test
    void updateItemQuantityByMenuItem_WhenOnlyItemIdMatches_UpdatesQuantity() {
        CartItem item = cartItem(2L, 100L, "Biryani", 180.0, 3, null);
        Cart cart = cart(6L, 1L, 10L, List.of(item));
        cart.setTotalPrice(540.0);

        when(cartRepository.findByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cartService.updateItemQuantity(1L, 2L, 2);

        assertThat(response.totalPrice()).isEqualTo(360.0);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
    }

    @Test
    void removeItem_WhenLastItemRemoved_ResetsRestaurantId() {
        CartItem item = cartItem(15L, 100L, "Fries", 80.0, 1, null);
        Cart cart = cart(9L, 4L, 10L, List.of(item));
        cart.setTotalPrice(80.0);

        when(cartItemRepository.findById(15L)).thenReturn(Optional.of(item));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = cartService.removeItem(15L);

        assertThat(response.restaurantId()).isNull();
        assertThat(response.totalPrice()).isEqualTo(0.0);
        assertThat(response.items()).isEmpty();
    }

    @Test
    void removeItemByMenuItem_WhenMissing_ThrowsCustomException() {
        when(cartRepository.findByCustomerId(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.removeItem(4L, 99L))
            .isInstanceOf(CartItemNotFoundException.class)
            .hasMessage("Cart item not found with id: 99");
    }

    @Test
    void applyPromoCode_WhenBlank_ThrowsBadRequest() {
        assertThatThrownBy(() -> cartService.applyPromoCode(1L, " "))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("promoCode");
    }

    @Test
    void clearCart_WhenExisting_DeletesCart() {
        Cart cart = cart(4L, 8L, null, List.of());
        when(cartRepository.findByCustomerId(8L)).thenReturn(Optional.of(cart));

        cartService.clearCart(8L);

        verify(cartRepository).delete(cart);
    }

    @Test
    void clearCart_WhenMissing_DoesNothing() {
        when(cartRepository.findByCustomerId(9L)).thenReturn(Optional.empty());

        cartService.clearCart(9L);

        verify(cartRepository, never()).delete(any(Cart.class));
    }

    private MenuItemSnapshotDto menuItemSnapshot(int itemId, int restaurantId, String name, double price,
                                                 Double discountedPrice, boolean available) {
        return new MenuItemSnapshotDto(itemId, restaurantId, 1, name, "desc", price, discountedPrice,
            null, false, available, null, null, null, null);
    }

    private Cart cart(Long cartId, Long customerId, Long restaurantId, List<CartItem> items) {
        Cart cart = Cart.builder()
            .cartId(cartId)
            .customerId(customerId)
            .restaurantId(restaurantId)
            .items(new ArrayList<>(items))
            .build();
        cart.getItems().forEach(item -> item.setCart(cart));
        return cart;
    }

    private CartItem cartItem(Long itemId, Long menuItemId, String name, Double price, Integer quantity,
                              String customization) {
        return CartItem.builder()
            .itemId(itemId)
            .menuItemId(menuItemId)
            .name(name)
            .price(price)
            .quantity(quantity)
            .customization(customization)
            .build();
    }
}
