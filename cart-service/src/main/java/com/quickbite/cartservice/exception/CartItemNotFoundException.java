package com.quickbite.cartservice.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long itemId) {
        super("Cart item not found with id: " + itemId);
    }
}
