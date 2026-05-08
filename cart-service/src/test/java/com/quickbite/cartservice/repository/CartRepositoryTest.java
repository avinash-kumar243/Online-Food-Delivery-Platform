package com.quickbite.cartservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.quickbite.cartservice.entity.Cart;

@DataJpaTest
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    private Cart existingCart;

    @BeforeEach
    void setUp() {
        existingCart = cartRepository.save(Cart.builder()
            .customerId(101L)
            .restaurantId(20L)
            .totalPrice(180.0)
            .items(new ArrayList<>())
            .build());
    }

    @Test
    void findByCustomerIdAndExistsByCustomerId_ReturnExpectedCart() {
        var result = cartRepository.findByCustomerId(101L);

        assertThat(result).isPresent();
        assertThat(result.get().getCartId()).isEqualTo(existingCart.getCartId());
        assertThat(result.get().getRestaurantId()).isEqualTo(20L);
        assertThat(cartRepository.existsByCustomerId(101L)).isTrue();
        assertThat(cartRepository.existsByCustomerId(999L)).isFalse();
    }

    @Test
    void deleteByCustomerId_RemovesCart() {
        cartRepository.deleteByCustomerId(101L);

        assertThat(cartRepository.findByCustomerId(101L)).isEmpty();
        assertThat(cartRepository.existsByCustomerId(101L)).isFalse();
    }
}
