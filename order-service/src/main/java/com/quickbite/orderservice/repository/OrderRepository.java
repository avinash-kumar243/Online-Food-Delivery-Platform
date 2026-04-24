package com.quickbite.orderservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.orderservice.entity.Order;
import com.quickbite.orderservice.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByRestaurantId(Long restaurantId);

    List<Order> findByOrderStatus(OrderStatus status);

    List<Order> findByDeliveryAgentId(Long agentId);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    long countByRestaurantId(Long restaurantId);
}
