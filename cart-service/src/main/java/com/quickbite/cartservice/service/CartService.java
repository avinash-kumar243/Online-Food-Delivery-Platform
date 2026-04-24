package com.quickbite.cartservice.service;

import com.quickbite.cartservice.dto.AddCartItemRequest;
import com.quickbite.cartservice.dto.CartResponse;
import com.quickbite.cartservice.dto.UpdateCartItemQuantityRequest;

public interface CartService {

    CartResponse getCartByCustomerId(Long customerId);

    CartResponse addItemToCart(AddCartItemRequest request);

    CartResponse updateItemQuantity(UpdateCartItemQuantityRequest request);

    CartResponse removeItem(Long itemId);

    CartResponse applyPromoCode(Long customerId, String promoCode);

    void clearCart(Long customerId);
}
