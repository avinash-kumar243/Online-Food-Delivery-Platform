package com.quickbite.orderservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickbite.orderservice.client.RestaurantClient;
import com.quickbite.orderservice.client.dto.RestaurantRealtimeDto;
import com.quickbite.orderservice.dto.PlaceOrderItemRequest;
import com.quickbite.orderservice.dto.PlaceOrderRequest;
import com.quickbite.orderservice.entity.Order;
import com.quickbite.orderservice.entity.OrderItem;
import com.quickbite.orderservice.entity.OrderStatus;
import com.quickbite.orderservice.exception.BadRequestException;
import com.quickbite.orderservice.exception.ConflictException;
import com.quickbite.orderservice.exception.OrderNotFoundException;
import com.quickbite.orderservice.messaging.GenericEventPublisher;
import com.quickbite.orderservice.realtime.RealtimeNotifier;
import com.quickbite.orderservice.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GenericEventPublisher eventPublisher;

    @Mock
    private RealtimeNotifier realtimeNotifier;

    @Mock
    private RestaurantClient restaurantClient;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, eventPublisher, realtimeNotifier, restaurantClient);
    }

    @Test
    void placeOrder_CreatesSnapshotAndPublishesEvents() {
        PlaceOrderRequest request = placeOrderRequest("checkout-1", 1L, 10L, new BigDecimal("15.00"));
        when(orderRepository.findByCheckoutReference("checkout-1")).thenReturn(Optional.empty());
        when(orderRepository.findTopByCustomerIdOrderByOrderDateDesc(1L)).thenReturn(Optional.empty());
        when(restaurantClient.getRestaurantById(10L)).thenReturn(new RestaurantRealtimeDto(10L, 1L, "QuickBite", true, true));

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
        assertThat(response.finalAmount()).isEqualByComparingTo("300.00");
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(response.items()).hasSize(2);
        verify(eventPublisher).send(eq("order.created"), any());
        verify(realtimeNotifier).publishOrderCreated(any(Order.class));
    }

    @Test
    void placeOrder_ReusesExistingCheckoutReference() {
        Order existingOrder = order(64L, 3L, 21L, OrderStatus.PLACED, "PENDING");
        existingOrder.setCheckoutReference("checkout-64");

        when(orderRepository.findByCheckoutReference("checkout-64")).thenReturn(Optional.of(existingOrder));

        var response = orderService.placeOrder(placeOrderRequest("checkout-64", 3L, 21L, BigDecimal.ZERO));

        assertThat(response.orderId()).isEqualTo(64L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_ReusesRecentMatchingPendingOrder() {
        Order existingOrder = order(66L, 3L, 21L, OrderStatus.PLACED, "PENDING");
        existingOrder.setCheckoutReference("checkout-old");
        existingOrder.setOrderDate(LocalDateTime.now().minusMinutes(2));
        existingOrder.setDeliveryAddress("Brigade Road");
        existingOrder.setSpecialInstructions("Ring bell");
        existingOrder.setModeOfPayment("CARD");
        existingOrder.setTotalAmount(new BigDecimal("180.00"));
        existingOrder.setDiscount(new BigDecimal("0.00"));
        existingOrder.setFinalAmount(new BigDecimal("189.00"));
        existingOrder.setItems(new ArrayList<>(List.of(orderItem(401L, 11L, "Burger", "180.00", 1, "No mayo"))));
        existingOrder.getItems().forEach(item -> item.setOrder(existingOrder));

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
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void placeOrder_RejectsClosedRestaurant() {
        when(orderRepository.findByCheckoutReference("checkout-closed")).thenReturn(Optional.empty());
        when(orderRepository.findTopByCustomerIdOrderByOrderDateDesc(1L)).thenReturn(Optional.empty());
        when(restaurantClient.getRestaurantById(10L)).thenReturn(new RestaurantRealtimeDto(10L, 1L, "QuickBite", false, true));

        assertThatThrownBy(() -> orderService.placeOrder(placeOrderRequest("checkout-closed", 1L, 10L, BigDecimal.ZERO)))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("closed");
    }

    @Test
    void placeOrder_RejectsDiscountGreaterThanTotal() {
        when(orderRepository.findByCheckoutReference("checkout-discount")).thenReturn(Optional.empty());
        when(orderRepository.findTopByCustomerIdOrderByOrderDateDesc(1L)).thenReturn(Optional.empty());
        when(restaurantClient.getRestaurantById(10L)).thenReturn(new RestaurantRealtimeDto(10L, 1L, "QuickBite", true, true));

        assertThatThrownBy(() -> orderService.placeOrder(placeOrderRequest("checkout-discount", 1L, 10L, new BigDecimal("500.00"))))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("discount cannot exceed totalAmount");
    }

    @Test
    void getOrderById_WhenMissing_ThrowsOrderNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(99L))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessage("Order not found with id: 99");
    }

    @Test
    void getActiveAndAvailableOrders_FilterByStatus() {
        Order placed = order(1L, 1L, 10L, OrderStatus.PLACED, "PENDING");
        Order readyForPickup = order(2L, 2L, 10L, OrderStatus.READY_FOR_PICKUP, "PENDING");
        Order delivered = order(3L, 3L, 10L, OrderStatus.DELIVERED, "PAID");
        placed.setOrderDate(LocalDateTime.now().minusHours(2));
        readyForPickup.setOrderDate(LocalDateTime.now().minusHours(1));
        delivered.setOrderDate(LocalDateTime.now());

        when(orderRepository.findAll()).thenReturn(List.of(delivered, readyForPickup, placed));

        assertThat(orderService.getActiveOrders()).extracting(com.quickbite.orderservice.dto.OrderResponse::orderId).containsExactly(2L, 1L);
        assertThat(orderService.getAvailableOrders()).extracting(com.quickbite.orderservice.dto.OrderResponse::orderId).containsExactly(2L);
    }

    @Test
    void getFilteredOrdersAndAllOrders_ReturnInNewestFirstOrder() {
        Order oldest = order(30L, 7L, 44L, OrderStatus.PLACED, "PENDING");
        Order newest = order(31L, 7L, 44L, OrderStatus.CONFIRMED, "PENDING");
        oldest.setDeliveryAgentId(555L);
        newest.setDeliveryAgentId(555L);
        oldest.setOrderDate(LocalDateTime.now().minusHours(3));
        newest.setOrderDate(LocalDateTime.now().minusMinutes(10));

        when(orderRepository.findByCustomerId(7L)).thenReturn(List.of(oldest, newest));
        when(orderRepository.findByRestaurantId(44L)).thenReturn(List.of(oldest, newest));
        when(orderRepository.findByDeliveryAgentId(555L)).thenReturn(List.of(oldest, newest));
        when(orderRepository.findAll()).thenReturn(List.of(oldest, newest));

        assertThat(orderService.getOrdersByCustomerId(7L))
            .extracting(com.quickbite.orderservice.dto.OrderResponse::orderId)
            .containsExactly(31L, 30L);
        assertThat(orderService.getOrdersByRestaurantId(44L))
            .extracting(com.quickbite.orderservice.dto.OrderResponse::orderId)
            .containsExactly(31L, 30L);
        assertThat(orderService.getOrdersByDeliveryAgentId(555L))
            .extracting(com.quickbite.orderservice.dto.OrderResponse::orderId)
            .containsExactly(31L, 30L);
        assertThat(orderService.getAllOrders())
            .extracting(com.quickbite.orderservice.dto.OrderResponse::orderId)
            .containsExactly(31L, 30L);
    }

    @Test
    void updateOrderStatus_RejectsInvalidTransition() {
        Order order = order(9L, 1L, 22L, OrderStatus.PLACED, "PENDING");
        when(orderRepository.findById(9L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(9L, OrderStatus.DELIVERED))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Invalid status transition");
    }

    @Test
    void updateOrderStatus_RejectsPickedUpWithoutDeliveryAgent() {
        Order order = order(10L, 1L, 22L, OrderStatus.READY_FOR_PICKUP, "PENDING");
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(10L, OrderStatus.PICKED_UP))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("deliveryAgentId must be assigned");
    }

    @Test
    void updateOrderStatus_WhenDelivered_PublishesEvent() {
        Order order = order(15L, 5L, 25L, OrderStatus.OUT_FOR_DELIVERY, "PENDING");
        order.setDeliveryAgentId(100L);
        when(orderRepository.findById(15L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.updateOrderStatus(15L, OrderStatus.DELIVERED);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(eventPublisher).send(eq("order.delivered"), any());
        verify(realtimeNotifier).publishOrderUpdated(any(Order.class));
    }

    @Test
    void updateOrderStatus_WhenStatusUnchanged_ReturnsExistingResponseWithoutSaving() {
        Order order = order(16L, 5L, 25L, OrderStatus.CONFIRMED, "PENDING");
        when(orderRepository.findById(16L)).thenReturn(Optional.of(order));

        var response = orderService.updateOrderStatus(16L, OrderStatus.CONFIRMED);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updatePaymentStatus_WhenMissing_ThrowsOrderNotFoundException() {
        when(orderRepository.findById(101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updatePaymentStatus(101L, "PAID"))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessage("Order not found with id: 101");
    }

    @Test
    void updatePaymentStatus_NormalizesBlankAndNonBlankValues() {
        Order firstOrder = order(102L, 5L, 25L, OrderStatus.PLACED, "PENDING");
        Order secondOrder = order(103L, 5L, 25L, OrderStatus.PLACED, "PENDING");

        when(orderRepository.findById(102L)).thenReturn(Optional.of(firstOrder));
        when(orderRepository.findById(103L)).thenReturn(Optional.of(secondOrder));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var blankResponse = orderService.updatePaymentStatus(102L, "   ");
        var paidResponse = orderService.updatePaymentStatus(103L, " paid ");

        assertThat(blankResponse.paymentStatus()).isEqualTo("PENDING");
        assertThat(paidResponse.paymentStatus()).isEqualTo("PAID");
        verify(realtimeNotifier).publishOrderUpdated(firstOrder);
        verify(realtimeNotifier).publishOrderUpdated(secondOrder);
    }

    @Test
    void updatePaymentStatus_WhenNull_DefaultsToPending() {
        Order order = order(104L, 5L, 25L, OrderStatus.PLACED, "PAID");

        when(orderRepository.findById(104L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.updatePaymentStatus(104L, null);

        assertThat(response.paymentStatus()).isEqualTo("PENDING");
        verify(realtimeNotifier).publishOrderUpdated(order);
    }

    @Test
    void assignDeliveryAgent_AssignsAgent() {
        Order order = order(11L, 3L, 30L, OrderStatus.READY_FOR_PICKUP, "PENDING");
        when(orderRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.assignDeliveryAgent(11L, 90L);

        assertThat(response.deliveryAgentId()).isEqualTo(90L);
        verify(realtimeNotifier).publishOrderUpdated(any(Order.class));
    }

    @Test
    void assignDeliveryAgent_RejectsAlreadyClaimedOrder() {
        Order order = order(12L, 4L, 31L, OrderStatus.READY_FOR_PICKUP, "PENDING");
        order.setDeliveryAgentId(77L);
        when(orderRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.assignDeliveryAgent(12L, 88L))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already been accepted");
    }

    @Test
    void assignDeliveryAgent_WhenAlreadyAssignedToSameAgent_ReturnsExistingOrder() {
        Order order = order(13L, 4L, 31L, OrderStatus.READY_FOR_PICKUP, "PENDING");
        order.setDeliveryAgentId(88L);
        when(orderRepository.findByIdForUpdate(13L)).thenReturn(Optional.of(order));

        var response = orderService.assignDeliveryAgent(13L, 88L);

        assertThat(response.deliveryAgentId()).isEqualTo(88L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void assignDeliveryAgent_RejectsCancelledOrder() {
        Order order = order(14L, 4L, 31L, OrderStatus.CANCELLED, "PENDING");
        when(orderRepository.findByIdForUpdate(14L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.assignDeliveryAgent(14L, 88L))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Cannot assign a delivery agent to a completed or cancelled order");
    }

    @Test
    void assignDeliveryAgent_WhenMissing_ThrowsOrderNotFoundException() {
        when(orderRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.assignDeliveryAgent(404L, 90L))
            .isInstanceOf(OrderNotFoundException.class)
            .hasMessage("Order not found with id: 404");
    }

    @Test
    void cancelOrder_UpdatesStatusAndPublishesRealtimeEvent() {
        Order order = order(24L, 8L, 44L, OrderStatus.CONFIRMED, "PENDING");
        when(orderRepository.findById(24L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orderService.cancelOrder(24L);

        assertThat(response.orderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(realtimeNotifier).publishOrderUpdated(order);
    }

    @Test
    void cancelOrder_RejectsDeliveredOrder() {
        Order order = order(25L, 8L, 44L, OrderStatus.DELIVERED, "PAID");
        when(orderRepository.findById(25L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(25L))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Delivered orders cannot be cancelled");
    }

    @Test
    void placeOrder_DoesNotReuseDeliveredOrderAndDefaultsEstimatedDelivery() {
        Order deliveredOrder = order(67L, 3L, 21L, OrderStatus.DELIVERED, "PENDING");
        deliveredOrder.setOrderDate(LocalDateTime.now().minusMinutes(2));

        when(orderRepository.findByCheckoutReference("checkout-fresh")).thenReturn(Optional.empty());
        when(orderRepository.findTopByCustomerIdOrderByOrderDateDesc(3L)).thenReturn(Optional.of(deliveredOrder));
        when(restaurantClient.getRestaurantById(21L)).thenReturn(new RestaurantRealtimeDto(21L, 1L, "QuickBite", true, true));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setOrderId(68L);
            return order;
        });

        PlaceOrderRequest request = new PlaceOrderRequest(
            "checkout-fresh",
            3L,
            21L,
            null,
            "CARD",
            null,
            "MG Road",
            null,
            List.of(new PlaceOrderItemRequest(11L, "Burger", new BigDecimal("180.00"), 1, null))
        );

        var response = orderService.placeOrder(request);

        assertThat(response.orderId()).isEqualTo(68L);
        assertThat(response.discount()).isEqualByComparingTo("0.00");
        assertThat(response.estimatedDelivery()).isAfter(LocalDateTime.now().plusMinutes(40));
    }

    @Test
    void reorder_CreatesNewPlacedOrderFromSnapshot() {
        Order existingOrder = order(20L, 8L, 44L, OrderStatus.DELIVERED, "PAID");
        existingOrder.setDeliveryAgentId(700L);
        existingOrder.setDiscount(new BigDecimal("20.00"));
        existingOrder.setFinalAmount(new BigDecimal("242.50"));
        existingOrder.setTotalAmount(new BigDecimal("250.00"));
        existingOrder.setModeOfPayment("COD");
        existingOrder.setDeliveryAddress("Church Street");
        existingOrder.setSpecialInstructions("Extra napkins");
        existingOrder.setItems(new ArrayList<>(List.of(orderItem(701L, 301L, "Pizza", "250.00", 1, "Thin crust"))));
        existingOrder.getItems().forEach(item -> item.setOrder(existingOrder));

        when(orderRepository.findById(20L)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.findByCheckoutReference(anyString())).thenReturn(Optional.empty());
        when(orderRepository.findTopByCustomerIdOrderByOrderDateDesc(8L)).thenReturn(Optional.empty());
        when(restaurantClient.getRestaurantById(44L)).thenReturn(new RestaurantRealtimeDto(44L, 1L, "QuickBite", true, true));
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
        assertThat(response.finalAmount()).isEqualByComparingTo("242.50");
    }

    @Test
    void countOrders_UsesRestaurantSpecificQueryWhenPresent() {
        when(orderRepository.count()).thenReturn(12L);
        when(orderRepository.countByRestaurantId(50L)).thenReturn(4L);

        assertThat(orderService.countOrders(null)).isEqualTo(12L);
        assertThat(orderService.countOrders(50L)).isEqualTo(4L);
    }

    private PlaceOrderRequest placeOrderRequest(String checkoutReference, Long customerId, Long restaurantId,
                                                BigDecimal discount) {
        return new PlaceOrderRequest(
            checkoutReference,
            customerId,
            restaurantId,
            discount,
            "CARD",
            LocalDateTime.now().plusMinutes(30),
            "221B Baker Street",
            "No onions",
            List.of(
                new PlaceOrderItemRequest(100L, "Burger", new BigDecimal("120.00"), 2, "Extra cheese"),
                new PlaceOrderItemRequest(101L, "Fries", new BigDecimal("60.00"), 1, null)
            )
        );
    }

    private Order order(Long orderId, Long customerId, Long restaurantId, OrderStatus status, String paymentStatus) {
        return Order.builder()
            .orderId(orderId)
            .checkoutReference("checkout-" + orderId)
            .customerId(customerId)
            .restaurantId(restaurantId)
            .deliveryAgentId(null)
            .totalAmount(new BigDecimal("100.00"))
            .discount(new BigDecimal("0.00"))
            .finalAmount(new BigDecimal("100.00"))
            .modeOfPayment("CARD")
            .paymentStatus(paymentStatus)
            .orderStatus(status)
            .orderDate(LocalDateTime.now())
            .estimatedDelivery(LocalDateTime.now().plusMinutes(45))
            .deliveryAddress("MG Road")
            .specialInstructions(null)
            .items(new ArrayList<>())
            .build();
    }

    private OrderItem orderItem(Long orderItemId, Long menuItemId, String name, String price, Integer quantity,
                                String customization) {
        return OrderItem.builder()
            .orderItemId(orderItemId)
            .menuItemId(menuItemId)
            .name(name)
            .price(new BigDecimal(price))
            .quantity(quantity)
            .customization(customization)
            .build();
    }
}
