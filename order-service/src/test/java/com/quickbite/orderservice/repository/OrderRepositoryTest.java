package com.quickbite.orderservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.quickbite.orderservice.entity.Order;
import com.quickbite.orderservice.entity.OrderItem;
import com.quickbite.orderservice.entity.OrderStatus;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private Order firstOrder;
    private Order secondOrder;

    @BeforeEach
    void setUp() {
        firstOrder = saveOrder(
            "checkout-1",
            10L,
            20L,
            70L,
            OrderStatus.PLACED,
            "PENDING",
            LocalDateTime.now().minusHours(2),
            List.of(orderItem(1001L, "Burger", "120.00", 2, "Extra cheese"))
        );

        secondOrder = saveOrder(
            "checkout-2",
            10L,
            21L,
            null,
            OrderStatus.READY_FOR_PICKUP,
            "PAID",
            LocalDateTime.now().minusMinutes(20),
            List.of(orderItem(1002L, "Pizza", "250.00", 1, "Thin crust"))
        );
    }

    @Test
    void findByCheckoutReferenceAndTopByCustomerId_ReturnExpectedOrders() {
        assertThat(orderRepository.findByCheckoutReference("checkout-1"))
            .isPresent()
            .get()
            .extracting(Order::getOrderId)
            .isEqualTo(firstOrder.getOrderId());

        assertThat(orderRepository.findTopByCustomerIdOrderByOrderDateDesc(10L))
            .isPresent()
            .get()
            .extracting(Order::getOrderId)
            .isEqualTo(secondOrder.getOrderId());
    }

    @Test
    void queryMethods_FilterOrdersByBusinessFields() {
        assertThat(orderRepository.findByCustomerId(10L)).hasSize(2);
        assertThat(orderRepository.findByRestaurantId(20L)).extracting(Order::getOrderId).containsExactly(firstOrder.getOrderId());
        assertThat(orderRepository.findByOrderStatus(OrderStatus.READY_FOR_PICKUP))
            .extracting(Order::getOrderId)
            .containsExactly(secondOrder.getOrderId());
        assertThat(orderRepository.findByDeliveryAgentId(70L))
            .extracting(Order::getOrderId)
            .containsExactly(firstOrder.getOrderId());
        assertThat(orderRepository.findByOrderDateBetween(LocalDateTime.now().minusDays(1), LocalDateTime.now()))
            .hasSize(2);
        assertThat(orderRepository.countByRestaurantId(21L)).isEqualTo(1L);
        assertThat(orderRepository.findByIdForUpdate(firstOrder.getOrderId())).isPresent();
    }

    private Order saveOrder(String checkoutReference, Long customerId, Long restaurantId, Long deliveryAgentId,
                            OrderStatus status, String paymentStatus, LocalDateTime orderDate, List<OrderItem> items) {
        Order order = Order.builder()
            .checkoutReference(checkoutReference)
            .customerId(customerId)
            .restaurantId(restaurantId)
            .deliveryAgentId(deliveryAgentId)
            .totalAmount(new BigDecimal("240.00"))
            .discount(BigDecimal.ZERO.setScale(2))
            .finalAmount(new BigDecimal("240.00"))
            .modeOfPayment("CARD")
            .paymentStatus(paymentStatus)
            .orderStatus(status)
            .orderDate(orderDate)
            .estimatedDelivery(orderDate.plusMinutes(45))
            .deliveryAddress("MG Road")
            .specialInstructions("No onions")
            .items(new ArrayList<>())
            .build();

        items.forEach(order::addItem);
        return orderRepository.save(order);
    }

    private OrderItem orderItem(Long menuItemId, String name, String price, Integer quantity, String customization) {
        return OrderItem.builder()
            .menuItemId(menuItemId)
            .name(name)
            .price(new BigDecimal(price))
            .quantity(quantity)
            .customization(customization)
            .build();
    }
}
