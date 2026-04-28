package com.quickbite.orderservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.orderservice.dto.OrderItemResponse;
import com.quickbite.orderservice.dto.OrderResponse;
import com.quickbite.orderservice.dto.PlaceOrderItemRequest;
import com.quickbite.orderservice.dto.PlaceOrderRequest;
import com.quickbite.orderservice.entity.Order;
import com.quickbite.orderservice.entity.OrderItem;
import com.quickbite.orderservice.entity.OrderStatus;
import com.quickbite.orderservice.exception.BadRequestException;
import com.quickbite.orderservice.exception.OrderNotFoundException;
import com.quickbite.orderservice.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final EnumSet<OrderStatus> ACTIVE_STATUSES = EnumSet.of(
        OrderStatus.PLACED,
        OrderStatus.CONFIRMED,
        OrderStatus.PREPARING,
        OrderStatus.READY_FOR_PICKUP,
        OrderStatus.PICKED_UP,
        OrderStatus.OUT_FOR_DELIVERY
    );
    private static final Map<OrderStatus, Set<OrderStatus>> VALID_STATUS_TRANSITIONS = buildTransitions();

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        BigDecimal totalAmount = calculateTotal(request.items());
        BigDecimal discount = normalizeMoney(request.discount() == null ? ZERO : request.discount());

        if (discount.compareTo(totalAmount) > 0) {
            throw new BadRequestException("discount cannot exceed totalAmount");
        }

        Order order = Order.builder()
            .customerId(request.customerId())
            .restaurantId(request.restaurantId())
            .deliveryAgentId(null)
            .totalAmount(totalAmount)
            .discount(discount)
            .finalAmount(totalAmount.subtract(discount))
            .modeOfPayment(request.modeOfPayment().trim())
            .paymentStatus("PENDING")
            .orderStatus(OrderStatus.PLACED)
            .orderDate(LocalDateTime.now())
            .estimatedDelivery(request.estimatedDelivery() != null
                ? request.estimatedDelivery()
                : LocalDateTime.now().plusMinutes(45))
            .deliveryAddress(request.deliveryAddress().trim())
            .specialInstructions(normalizeText(request.specialInstructions()))
            .build();

        request.items().stream()
            .map(this::toOrderItem)
            .forEach(order::addItem);

        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        return toResponse(fetchOrder(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
            .sorted(orderDateDesc())
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByRestaurantId(Long restaurantId) {
        return orderRepository.findByRestaurantId(restaurantId).stream()
            .sorted(orderDateDesc())
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByDeliveryAgentId(Long deliveryAgentId) {
        return orderRepository.findByDeliveryAgentId(deliveryAgentId).stream()
            .sorted(orderDateDesc())
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrders() {
        return orderRepository.findAll().stream()
            .filter(order -> ACTIVE_STATUSES.contains(order.getOrderStatus()))
            .sorted(orderDateDesc())
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAvailableOrders() {
        return orderRepository.findAll().stream()
            .filter(order -> order.getOrderStatus() == OrderStatus.READY_FOR_PICKUP)
            .filter(order -> order.getDeliveryAgentId() == null)
            .sorted(orderDateDesc())
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
            .sorted(orderDateDesc())
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = fetchOrder(orderId);
        OrderStatus currentStatus = order.getOrderStatus();

        if (currentStatus == status) {
            return toResponse(order);
        }

        if (status == OrderStatus.PICKED_UP && order.getDeliveryAgentId() == null) {
            throw new BadRequestException("deliveryAgentId must be assigned before marking an order as PICKED_UP");
        }

        Set<OrderStatus> allowedStatuses = VALID_STATUS_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowedStatuses.contains(status)) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + status);
        }

        order.setOrderStatus(status);
        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse updatePaymentStatus(Long orderId, String paymentStatus) {
        Order order = fetchOrder(orderId);
        order.setPaymentStatus(paymentStatus == null || paymentStatus.isBlank() ? "PENDING" : paymentStatus.trim().toUpperCase());
        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse assignDeliveryAgent(Long orderId, Long deliveryAgentId) {
        Order order = fetchOrder(orderId);

        if (EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED).contains(order.getOrderStatus())) {
            throw new BadRequestException("Cannot assign a delivery agent to a completed or cancelled order");
        }
        if (order.getOrderStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new BadRequestException("Delivery partners can only accept READY_FOR_PICKUP orders");
        }

        order.setDeliveryAgentId(deliveryAgentId);
        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = fetchOrder(orderId);
        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Delivered orders cannot be cancelled");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        return toResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse reorder(Long orderId) {
        Order existingOrder = fetchOrder(orderId);

        PlaceOrderRequest reorderRequest = new PlaceOrderRequest(
            existingOrder.getCustomerId(),
            existingOrder.getRestaurantId(),
            existingOrder.getDiscount(),
            existingOrder.getModeOfPayment(),
            LocalDateTime.now().plusMinutes(45),
            existingOrder.getDeliveryAddress(),
            existingOrder.getSpecialInstructions(),
            existingOrder.getItems().stream()
                .map(item -> new PlaceOrderItemRequest(
                    item.getMenuItemId(),
                    item.getName(),
                    item.getPrice(),
                    item.getQuantity(),
                    item.getCustomization()
                ))
                .toList()
        );

        return placeOrder(reorderRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOrders(Long restaurantId) {
        return restaurantId == null ? orderRepository.count() : orderRepository.countByRestaurantId(restaurantId);
    }

    private Order fetchOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private BigDecimal calculateTotal(List<PlaceOrderItemRequest> items) {
        return normalizeMoney(items.stream()
            .map(item -> normalizeMoney(item.price()).multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private OrderItem toOrderItem(PlaceOrderItemRequest item) {
        return OrderItem.builder()
            .menuItemId(item.menuItemId())
            .name(item.name().trim())
            .price(normalizeMoney(item.price()))
            .quantity(item.quantity())
            .customization(normalizeText(item.customization()))
            .build();
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
            .map(item -> new OrderItemResponse(
                item.getOrderItemId(),
                item.getMenuItemId(),
                item.getName(),
                item.getPrice(),
                item.getQuantity(),
                item.getCustomization(),
                normalizeMoney(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            ))
            .toList();

        return new OrderResponse(
            order.getOrderId(),
            order.getCustomerId(),
            order.getRestaurantId(),
            order.getDeliveryAgentId(),
            order.getTotalAmount(),
            order.getDiscount(),
            order.getFinalAmount(),
            order.getModeOfPayment(),
            order.getPaymentStatus(),
            order.getOrderStatus(),
            order.getOrderDate(),
            order.getEstimatedDelivery(),
            order.getDeliveryAddress(),
            order.getSpecialInstructions(),
            itemResponses
        );
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Comparator<Order> orderDateDesc() {
        return Comparator.comparing(Order::getOrderDate).reversed();
    }

    private static Map<OrderStatus, Set<OrderStatus>> buildTransitions() {
        Map<OrderStatus, Set<OrderStatus>> transitions = new EnumMap<>(OrderStatus.class);
        transitions.put(OrderStatus.PLACED, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.PREPARING, EnumSet.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED));
        transitions.put(OrderStatus.PICKED_UP, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED));
        transitions.put(OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED));
        transitions.put(OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));
        transitions.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        return transitions;
    }
}
