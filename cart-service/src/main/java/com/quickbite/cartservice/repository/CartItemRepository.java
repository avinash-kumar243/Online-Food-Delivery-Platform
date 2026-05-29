package com.quickbite.cartservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.cartservice.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
}
