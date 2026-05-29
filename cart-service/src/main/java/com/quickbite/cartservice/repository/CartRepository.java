package com.quickbite.cartservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.cartservice.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByCustomerId(Long customerId);

    boolean existsByCustomerId(Long customerId);

    void deleteByCustomerId(Long customerId);
}
