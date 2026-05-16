package com.quickbite.cartservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.quickbite.cartservice.entity.Cart;
import com.quickbite.cartservice.entity.CartItem;

@DataJpaTest
class CartItemRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    private Long cartItemId;

    @BeforeEach
    void setUp() {
        Cart cart = Cart.builder()
            .customerId(202L)
            .restaurantId(30L)
            .totalPrice(160.0)
            .items(new ArrayList<>())
            .build();

        CartItem cartItem = CartItem.builder()
            .menuItemId(501L)
            .name("Paneer Wrap")
            .price(80.0)
            .quantity(2)
            .customization("Less spicy")
            .cart(cart)
            .build();

        cart.setItems(new ArrayList<>(List.of(cartItem)));
        Cart savedCart = cartRepository.save(cart);
        cartItemId = savedCart.getItems().get(0).getItemId();
    }

    @Test
    void findById_ReturnsPersistedCartItemWithAssociation() {
        var result = cartItemRepository.findById(cartItemId);

        assertThat(result).isPresent();
        assertThat(result.get().getMenuItemId()).isEqualTo(501L);
        assertThat(result.get().getQuantity()).isEqualTo(2);
        assertThat(result.get().getCart().getCustomerId()).isEqualTo(202L);
    }
}
