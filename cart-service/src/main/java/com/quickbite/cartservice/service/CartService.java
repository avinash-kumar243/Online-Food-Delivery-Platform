package com.quickbite.cartservice.service;

import com.quickbite.cartservice.dto.AddCartItemRequest;
import com.quickbite.cartservice.dto.CartResponse;
import com.quickbite.cartservice.dto.UpdateCartItemQuantityRequest;

public interface CartService {

    CartResponse getCartByCustomerId(Long customerId);

    CartResponse addItemToCart(AddCartItemRequest request);

    CartResponse updateItemQuantity(UpdateCartItemQuantityRequest request);

    CartResponse updateItemQuantity(Long customerId, Long menuItemId, Integer quantity);

    CartResponse removeItem(Long itemId);

    CartResponse removeItem(Long customerId, Long menuItemId);

    CartResponse applyPromoCode(Long customerId, String promoCode);

    void clearCart(Long customerId);
}
