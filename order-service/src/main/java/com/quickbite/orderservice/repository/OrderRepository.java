package com.quickbite.orderservice.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quickbite.orderservice.entity.Order;
import com.quickbite.orderservice.entity.OrderStatus;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {

    java.util.Optional<Order> findByCheckoutReference(String checkoutReference);

    java.util.Optional<Order> findTopByCustomerIdOrderByOrderDateDesc(Long customerId);

    List<Order> findByCustomerId(Long customerId);

    List<Order> findByRestaurantId(Long restaurantId);

    List<Order> findByOrderStatus(OrderStatus status);

    List<Order> findByDeliveryAgentId(Long agentId);

    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    long countByRestaurantId(Long restaurantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.orderId = :orderId")
    java.util.Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);
}
