package com.quickbite.cartservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.cartservice.dto.AddCartItemRequest;
import com.quickbite.cartservice.dto.ApplyPromoRequest;
import com.quickbite.cartservice.dto.CartResponse;
import com.quickbite.cartservice.dto.UpdateCartItemQuantityRequest;
import com.quickbite.cartservice.service.CartService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping({"/cart", "/api/v1/cart"})
@RequiredArgsConstructor
public class CartResource {

    private final CartService cartService;

    @GetMapping({"/{customerId}", "/customer/{customerId}"})
    public ResponseEntity<CartResponse> getCart(@PathVariable Long customerId) {
        return ResponseEntity.ok(cartService.getCartByCustomerId(customerId));
    }

    @PostMapping({"/add", "/items"})
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItemToCart(request));
    }

    @PutMapping("/update-quantity")
    public ResponseEntity<CartResponse> updateQuantity(@Valid @RequestBody UpdateCartItemQuantityRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(request));
    }

    @PutMapping("/items/{itemId}/quantity")
    public ResponseEntity<CartResponse> updateQuantityByPath(@PathVariable Long itemId,
                                                             @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        return ResponseEntity.ok(cartService.updateItemQuantity(
            new UpdateCartItemQuantityRequest(request.customerId(), itemId, request.quantity())
        ));
    }

    @DeleteMapping({"/remove-item/{itemId}", "/items/{itemId}"})
    public ResponseEntity<CartResponse> removeItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(itemId));
    }

    @PostMapping("/apply-promo")
    public ResponseEntity<CartResponse> applyPromo(@Valid @RequestBody ApplyPromoRequest request) {
        return ResponseEntity.ok(cartService.applyPromoCode(request.customerId(), request.promoCode()));
    }

    @DeleteMapping({"/clear/{customerId}", "/customer/{customerId}/clear"})
    public ResponseEntity<Void> clearCart(@PathVariable Long customerId) {
        cartService.clearCart(customerId);
        return ResponseEntity.noContent().build();
    }
}
