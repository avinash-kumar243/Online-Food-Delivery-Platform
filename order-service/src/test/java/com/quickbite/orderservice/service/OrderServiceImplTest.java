package com.quickbite.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.quickbite.orderservice.dto.PlaceOrderItemRequest;
import com.quickbite.orderservice.dto.PlaceOrderRequest;
import com.quickbite.orderservice.entity.Order;
import com.quickbite.orderservice.entity.OrderItem;
import com.quickbite.orderservice.entity.OrderStatus;
import com.quickbite.orderservice.exception.BadRequestException;
import com.quickbite.orderservice.exception.ConflictException;
import com.quickbite.orderservice.messaging.GenericEventPublisher;
import com.quickbite.orderservice.repository.OrderRepository;

class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GenericEventPublisher eventPublisher;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderServiceImpl(orderRepository, eventPublisher);
        when(orderRepository.findByCheckoutReference(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void placeOrderShouldSnapshotItemsAndCalculateAmounts() {
        PlaceOrderRequest request = new PlaceOrderRequest(
            "checkout-1",
            1L,
            10L,
            new BigDecimal("15.00"),
            "CARD",
            LocalDateTime.now().plusMinutes(30),
            "221B Baker Street",
            "No onions",
            List.of(
                new PlaceOrderItemRequest(100L, "Burger", new BigDecimal("120.00"), 2, "Extra cheese"),
                new PlaceOrderItemRequest(101L, "Fries", new BigDecimal("60.00"), 1, null)
            )
        );

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(55L);
            long itemId = 500L;
            for (OrderItem item : order.getItems()) {
                item.setOrderItemId(itemId++);
            }
            return order;
        });

        var response = orderService.placeOrder(request);

        assertThat(response.orderId()).isEqualTo(55L);
        assertThat(response.totalAmount()).isEqualByComparingTo("300.00");
        assertThat(response.discount()).isEqualByComparingTo("15.00");
        assertThat(response.finalAmount()).isEqualByComparingTo("285.00");
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.items()).hasSize(2);
    }

    @Test
    void placeOrderShouldReturnExistingOrderForSameCheckoutReference() {
        Order existingOrder = Order.builder()
            .orderId(64L)
            .checkoutReference("checkout-64")
            .customerId(3L)
            .restaurantId(21L)
            .totalAmount(new BigDecimal("180.00"))
            .discount(new BigDecimal("0.00"))
            .finalAmount(new BigDecimal("180.00"))
            .modeOfPayment("CARD")
            .paymentStatus("PENDING")
            .orderStatus(OrderStatus.PLACED)
            .orderDate(LocalDateTime.now())
            .estimatedDelivery(LocalDateTime.now().plusMinutes(40))
            .deliveryAddress("Brigade Road")
            .items(new ArrayList<>())
            .build();

        when(orderRepository.findByCheckoutReference("checkout-64")).thenReturn(Optional.of(existingOrder));

        PlaceOrderRequest request = new PlaceOrderRequest(
            "checkout-64",
            3L,
            21L,
            new BigDecimal("0.00"),
            "CARD",
            LocalDateTime.now().plusMinutes(40),
            "Brigade Road",
            null,
            List.of(new PlaceOrderItemRequest(11L, "Burger", new BigDecimal("180.00"), 1, null))
        );

        var response = orderService.placeOrder(request);

        assertThat(response.orderId()).isEqualTo(64L);
    }

    @Test
    void placeOrderShouldReuseLatestPendingMatchingOrder() {
        OrderItem existingItem = OrderItem.builder()
            .orderItemId(401L)
            .menuItemId(11L)
            .name("Burger")
            .price(new BigDecimal("180.00"))
            .quantity(1)
            .customization("No mayo")
            .build();

        Order existingOrder = Order.builder()
            .orderId(66L)
            .checkoutReference("checkout-old")
            .customerId(3L)
            .restaurantId(21L)
            .totalAmount(new BigDecimal("180.00"))
            .discount(new BigDecimal("0.00"))
            .finalAmount(new BigDecimal("180.00"))
            .modeOfPayment("CARD")
            .paymentStatus("PENDING")
            .orderStatus(OrderStatus.PLACED)
            .orderDate(LocalDateTime.now().minusMinutes(2))
            .estimatedDelivery(LocalDateTime.now().plusMinutes(40))
            .deliveryAddress("Brigade Road")
            .specialInstructions("Ring bell")
            .items(new ArrayList<>(List.of(existingItem)))
            .build();
        existingItem.setOrder(existingOrder);

        when(orderRepository.findByCheckoutReference("checkout-new")).thenReturn(Optional.empty());
        when(orderRepository.findTopByCustomerIdOrderByOrderDateDesc(3L)).thenReturn(Optional.of(existingOrder));

        PlaceOrderRequest request = new PlaceOrderRequest(
            "checkout-new",
            3L,
            21L,
            new BigDecimal("0.00"),
            "CARD",
            LocalDateTime.now().plusMinutes(40),
            "Brigade Road",
            "Ring bell",
            List.of(new PlaceOrderItemRequest(11L, "Burger", new BigDecimal("180.00"), 1, "No mayo"))
        );

        var response = orderService.placeOrder(request);

        assertThat(response.orderId()).isEqualTo(66L);
    }

    @Test
    void updateOrderStatusShouldRejectInvalidTransition() {
        Order order = Order.builder()
            .orderId(9L)
            .customerId(1L)
            .restaurantId(22L)
            .totalAmount(new BigDecimal("100.00"))
            .discount(new BigDecimal("0.00"))
            .finalAmount(new BigDecimal("100.00"))
            .modeOfPayment("UPI")
            .orderStatus(OrderStatus.PLACED)
            .orderDate(LocalDateTime.now())
            .estimatedDelivery(LocalDateTime.now().plusMinutes(45))
            .deliveryAddress("MG Road")
            .items(new ArrayList<>())
            .build();

        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(9L, OrderStatus.DELIVERED))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Invalid status transition");
    }

    @Test
    void assignDeliveryAgentShouldUpdateExistingOrder() {
        Order order = Order.builder()
            .orderId(11L)
            .customerId(3L)
            .restaurantId(30L)
            .totalAmount(new BigDecimal("200.00"))
            .discount(new BigDecimal("10.00"))
            .finalAmount(new BigDecimal("190.00"))
            .modeOfPayment("CARD")
            .orderStatus(OrderStatus.READY_FOR_PICKUP)
            .orderDate(LocalDateTime.now())
            .estimatedDelivery(LocalDateTime.now().plusMinutes(50))
            .deliveryAddress("Brigade Road")
            .items(new ArrayList<>())
            .build();

        when(orderRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.assignDeliveryAgent(11L, 90L);

        assertThat(response.deliveryAgentId()).isEqualTo(90L);
    }

    @Test
    void assignDeliveryAgentShouldRejectSecondPartnerClaim() {
        Order order = Order.builder()
            .orderId(12L)
            .customerId(4L)
            .restaurantId(31L)
            .deliveryAgentId(77L)
            .totalAmount(new BigDecimal("220.00"))
            .discount(new BigDecimal("0.00"))
            .finalAmount(new BigDecimal("220.00"))
            .modeOfPayment("COD")
            .orderStatus(OrderStatus.READY_FOR_PICKUP)
            .orderDate(LocalDateTime.now())
            .estimatedDelivery(LocalDateTime.now().plusMinutes(50))
            .deliveryAddress("Indiranagar")
            .items(new ArrayList<>())
            .build();

        when(orderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.assignDeliveryAgent(12L, 88L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already been accepted");
    }

    @Test
    void updateOrderStatusShouldPublishDeliveredEvent() {
        Order order = Order.builder()
            .orderId(15L)
            .customerId(5L)
            .restaurantId(25L)
            .deliveryAgentId(100L)
            .totalAmount(new BigDecimal("180.00"))
            .discount(new BigDecimal("0.00"))
            .finalAmount(new BigDecimal("180.00"))
            .modeOfPayment("COD")
            .orderStatus(OrderStatus.OUT_FOR_DELIVERY)
            .orderDate(LocalDateTime.now())
            .estimatedDelivery(LocalDateTime.now().plusMinutes(20))
            .deliveryAddress("Indore")
            .items(new ArrayList<>())
            .build();

        when(orderRepository.findById(15L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.updateOrderStatus(15L, OrderStatus.DELIVERED);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(eventPublisher).send(org.mockito.ArgumentMatchers.eq("order.delivered"), any());
    }

    @Test
    void reorderShouldCreateNewPlacedOrderFromExistingSnapshot() {
        OrderItem oldItem = OrderItem.builder()
            .orderItemId(701L)
            .menuItemId(301L)
            .name("Pizza")
            .price(new BigDecimal("250.00"))
            .quantity(1)
            .customization("Thin crust")
            .build();

        Order existingOrder = Order.builder()
            .orderId(20L)
            .customerId(8L)
            .restaurantId(44L)
            .deliveryAgentId(700L)
            .totalAmount(new BigDecimal("250.00"))
            .discount(new BigDecimal("20.00"))
            .finalAmount(new BigDecimal("230.00"))
            .modeOfPayment("COD")
            .orderStatus(OrderStatus.DELIVERED)
            .orderDate(LocalDateTime.now().minusDays(2))
            .estimatedDelivery(LocalDateTime.now().minusDays(2).plusMinutes(40))
            .deliveryAddress("Church Street")
            .specialInstructions("Extra napkins")
            .items(new ArrayList<>(List.of(oldItem)))
            .build();
        oldItem.setOrder(existingOrder);

        when(orderRepository.findById(20L)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(21L);
            order.getItems().forEach(item -> item.setOrderItemId(900L));
            return order;
        });

        var response = orderService.reorder(20L);

        assertThat(response.orderId()).isEqualTo(21L);
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.deliveryAgentId()).isNull();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).name()).isEqualTo("Pizza");
        assertThat(response.finalAmount()).isEqualByComparingTo("230.00");
    }
}
