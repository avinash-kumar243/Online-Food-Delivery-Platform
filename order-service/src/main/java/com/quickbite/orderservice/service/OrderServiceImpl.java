package com.quickbite.orderservice.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.quickbite.orderservice.client.CartClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.orderservice.client.DeliveryAgentClient;
import com.quickbite.orderservice.client.MenuClient;
import com.quickbite.orderservice.client.dto.CartItemSnapshotDto;
import com.quickbite.orderservice.client.dto.CartSnapshotDto;
import com.quickbite.orderservice.dto.OrderItemResponse;
import com.quickbite.orderservice.dto.OrderDeliveryPartnerInfo;
import com.quickbite.orderservice.dto.OrderRestaurantInfo;
import com.quickbite.orderservice.dto.OrderResponse;
import com.quickbite.orderservice.dto.PlaceOrderItemRequest;
import com.quickbite.orderservice.dto.PlaceOrderRequest;
import com.quickbite.orderservice.client.RestaurantClient;
import com.quickbite.orderservice.client.dto.DeliveryAgentRealtimeDto;
import com.quickbite.orderservice.client.dto.RestaurantRealtimeDto;
import com.quickbite.orderservice.entity.Order;
import com.quickbite.orderservice.entity.OrderItem;
import com.quickbite.orderservice.entity.OrderStatus;
import com.quickbite.orderservice.exception.BadRequestException;
import com.quickbite.orderservice.exception.ConflictException;
import com.quickbite.orderservice.exception.OrderNotFoundException;
import com.quickbite.orderservice.messaging.GenericEventPublisher;
import com.quickbite.orderservice.messaging.QuickbiteOrderMessagingConstants;
import com.quickbite.orderservice.messaging.dto.OrderEventDTO;
import com.quickbite.orderservice.realtime.RealtimeNotifier;
import com.quickbite.orderservice.repository.OrderRepository;

@Service
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal TAX_RATE = new BigDecimal("0.05");
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
    private final GenericEventPublisher eventPublisher;
    private final RealtimeNotifier realtimeNotifier;
    private final RestaurantClient restaurantClient;
    private final DeliveryAgentClient deliveryAgentClient;
    private final CartClient cartClient;
    private final MenuClient menuClient;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository,
                            GenericEventPublisher eventPublisher,
                            RealtimeNotifier realtimeNotifier,
                            RestaurantClient restaurantClient,
                            DeliveryAgentClient deliveryAgentClient,
                            CartClient cartClient,
                            MenuClient menuClient) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.realtimeNotifier = realtimeNotifier;
        this.restaurantClient = restaurantClient;
        this.deliveryAgentClient = deliveryAgentClient;
        this.cartClient = cartClient;
        this.menuClient = menuClient;
    }

    public OrderServiceImpl(OrderRepository orderRepository,
                            GenericEventPublisher eventPublisher,
                            RealtimeNotifier realtimeNotifier,
                            RestaurantClient restaurantClient,
                            DeliveryAgentClient deliveryAgentClient) {
        this(orderRepository, eventPublisher, realtimeNotifier, restaurantClient, deliveryAgentClient, null, null);
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        var existingOrder = orderRepository.findByCheckoutReference(request.checkoutReference().trim());
        if (existingOrder.isPresent()) {
            return toResponse(existingOrder.get());
        }

        var latestCustomerOrder = orderRepository.findTopByCustomerIdOrderByOrderDateDesc(request.customerId());
        if (latestCustomerOrder.isPresent() && isDuplicatePlacement(latestCustomerOrder.get(), request)) {
            return toResponse(latestCustomerOrder.get());
        }

        ensureRestaurantAcceptingOrders(request.restaurantId());
        validateCartAndMenuState(request);

        BigDecimal totalAmount = calculateTotal(request.items());
        BigDecimal taxAmount = calculateTax(totalAmount);
        BigDecimal discount = normalizeMoney(request.discount() == null ? ZERO : request.discount());

        if (discount.compareTo(totalAmount.add(taxAmount)) > 0) {
            throw new BadRequestException("discount cannot exceed totalAmount");
        }

        Order order = Order.builder()
            .checkoutReference(request.checkoutReference().trim())
            .customerId(request.customerId())
            .restaurantId(request.restaurantId())
            .deliveryAgentId(null)
            .totalAmount(totalAmount)
            .discount(discount)
            .finalAmount(totalAmount.add(taxAmount).subtract(discount))
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

        Order savedOrder = orderRepository.save(order);
        eventPublisher.send(QuickbiteOrderMessagingConstants.ORDER_CREATED_ROUTING_KEY, toOrderEvent(savedOrder));
        clearCustomerCartSafely(savedOrder.getCustomerId());
        realtimeNotifier.publishOrderCreated(savedOrder);
        return toResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = fetchOrder(orderId);
        return toResponse(
            order,
            fetchRestaurants(Set.of(order.getRestaurantId())),
            fetchDeliveryAgents(order.getDeliveryAgentId() == null ? Set.of() : Set.of(order.getDeliveryAgentId()))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        return toResponses(orderRepository.findByCustomerId(customerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByRestaurantId(Long restaurantId) {
        return toResponses(orderRepository.findByRestaurantId(restaurantId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByDeliveryAgentId(Long deliveryAgentId) {
        return toResponses(orderRepository.findByDeliveryAgentId(deliveryAgentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveOrders() {
        return toResponses(orderRepository.findAll().stream()
            .filter(order -> ACTIVE_STATUSES.contains(order.getOrderStatus()))
            .sorted(orderDateDesc())
            .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAvailableOrders() {
        return toResponses(orderRepository.findAll().stream()
            .filter(order -> order.getOrderStatus() == OrderStatus.READY_FOR_PICKUP)
            .filter(order -> order.getDeliveryAgentId() == null)
            .sorted(orderDateDesc())
            .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return toResponses(orderRepository.findAll());
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
        Order savedOrder = orderRepository.save(order);

        String routingKey = routingKeyForStatus(status);
        if (routingKey != null) {
            eventPublisher.send(routingKey, toOrderEvent(savedOrder));
            if (status == OrderStatus.DELIVERED) {
                eventPublisher.send("order.delivered", toOrderEvent(savedOrder));
            }
        }
        realtimeNotifier.publishOrderUpdated(savedOrder);

        return toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updatePaymentStatus(Long orderId, String paymentStatus) {
        Order order = fetchOrder(orderId);
        String normalizedPaymentStatus = normalizePaymentStatus(paymentStatus);
        order.setPaymentStatus(normalizedPaymentStatus);
        Order savedOrder = orderRepository.save(order);
        realtimeNotifier.publishOrderUpdated(savedOrder);
        return toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse assignDeliveryAgent(Long orderId, Long deliveryAgentId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED).contains(order.getOrderStatus())) {
            throw new BadRequestException("Cannot assign a delivery agent to a completed or cancelled order");
        }
        if (order.getOrderStatus() != OrderStatus.READY_FOR_PICKUP) {
            throw new BadRequestException("Delivery partners can only accept READY_FOR_PICKUP orders");
        }
        if (order.getDeliveryAgentId() != null && !order.getDeliveryAgentId().equals(deliveryAgentId)) {
            throw new ConflictException("Order has already been accepted by another delivery partner");
        }
        if (order.getDeliveryAgentId() != null) {
            return toResponse(order);
        }

        order.setDeliveryAgentId(deliveryAgentId);
        Order savedOrder = orderRepository.save(order);
        realtimeNotifier.publishOrderUpdated(savedOrder);
        return toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = fetchOrder(orderId);
        if (order.getOrderStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Delivered orders cannot be cancelled");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        Order savedOrder = orderRepository.save(order);
        eventPublisher.send(QuickbiteOrderMessagingConstants.ORDER_CANCELLED_ROUTING_KEY, toOrderEvent(savedOrder));
        realtimeNotifier.publishOrderUpdated(savedOrder);
        return toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse reorder(Long orderId) {
        Order existingOrder = fetchOrder(orderId);

        PlaceOrderRequest reorderRequest = new PlaceOrderRequest(
            "reorder-" + existingOrder.getOrderId() + "-" + System.currentTimeMillis(),
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

    private BigDecimal calculateTax(BigDecimal amount) {
        return normalizeMoney(amount.multiply(TAX_RATE));
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

    private List<OrderResponse> toResponses(List<Order> orders) {
        List<Order> sortedOrders = orders.stream()
            .sorted(orderDateDesc())
            .toList();

        Map<Long, RestaurantRealtimeDto> restaurants = fetchRestaurants(sortedOrders.stream()
            .map(Order::getRestaurantId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet()));
        Map<Long, DeliveryAgentRealtimeDto> deliveryPartners = fetchDeliveryAgents(sortedOrders.stream()
            .map(Order::getDeliveryAgentId)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toSet()));

        return sortedOrders.stream()
            .map(order -> toResponse(order, restaurants, deliveryPartners))
            .toList();
    }

    private OrderResponse toResponse(Order order) {
        return toResponse(
            order,
            fetchRestaurants(Set.of(order.getRestaurantId())),
            fetchDeliveryAgents(order.getDeliveryAgentId() == null ? Set.of() : Set.of(order.getDeliveryAgentId()))
        );
    }

    private OrderResponse toResponse(
        Order order,
        Map<Long, RestaurantRealtimeDto> restaurants,
        Map<Long, DeliveryAgentRealtimeDto> deliveryPartners
    ) {
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

        RestaurantRealtimeDto restaurant = restaurants.get(order.getRestaurantId());
        DeliveryAgentRealtimeDto deliveryPartner = order.getDeliveryAgentId() == null
            ? null
            : deliveryPartners.get(order.getDeliveryAgentId());

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
            order.getOrderDate().atOffset(ZoneOffset.UTC),
            order.getEstimatedDelivery().atOffset(ZoneOffset.UTC),
            order.getDeliveryAddress(),
            order.getSpecialInstructions(),
            itemResponses,
            restaurant == null ? null : new OrderRestaurantInfo(
                restaurant.restaurantId(),
                restaurant.name(),
                restaurant.phone(),
                restaurant.address(),
                restaurant.city(),
                restaurant.isOpen(),
                restaurant.isApproved()
            ),
            deliveryPartner == null ? null : new OrderDeliveryPartnerInfo(
                deliveryPartner.agentId(),
                deliveryPartner.userId(),
                deliveryPartner.fullName(),
                deliveryPartner.phone(),
                deliveryPartner.verificationStatus()
            )
        );
    }

    private Map<Long, RestaurantRealtimeDto> fetchRestaurants(Set<Long> restaurantIds) {
        Map<Long, RestaurantRealtimeDto> restaurants = new HashMap<>();
        for (Long restaurantId : restaurantIds) {
            try {
                RestaurantRealtimeDto restaurant = restaurantClient.getRestaurantById(restaurantId);
                if (restaurant != null) {
                    restaurants.put(restaurantId, restaurant);
                }
            } catch (RuntimeException ignored) {
                // Preserve order visibility when restaurant metadata is temporarily unavailable.
            }
        }
        return restaurants;
    }

    private Map<Long, DeliveryAgentRealtimeDto> fetchDeliveryAgents(Set<Long> deliveryAgentIds) {
        Map<Long, DeliveryAgentRealtimeDto> deliveryAgents = new HashMap<>();
        for (Long deliveryAgentId : deliveryAgentIds) {
            try {
                DeliveryAgentRealtimeDto deliveryAgent = deliveryAgentClient.getAgentById(deliveryAgentId);
                if (deliveryAgent != null) {
                    deliveryAgents.put(deliveryAgentId, deliveryAgent);
                }
            } catch (RuntimeException ignored) {
                // Preserve order visibility when delivery metadata is temporarily unavailable.
            }
        }
        return deliveryAgents;
    }

    private String normalizeText(String value) {
        return value == null ? null : value.trim();
    }

    private String normalizePaymentStatus(String paymentStatus) {
        if (paymentStatus == null) {
            return "PENDING";
        }

        String trimmedPaymentStatus = paymentStatus.trim();
        return trimmedPaymentStatus.isBlank() ? "PENDING" : trimmedPaymentStatus.toUpperCase();
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Comparator<Order> orderDateDesc() {
        return Comparator.comparing(Order::getOrderDate).reversed();
    }

    private OrderEventDTO toOrderEvent(Order order) {
        return new OrderEventDTO(
            order.getOrderId(),
            order.getCustomerId(),
            order.getRestaurantId(),
            resolveRestaurantOwnerId(order.getRestaurantId()),
            order.getDeliveryAgentId(),
            order.getFinalAmount(),
            LocalDateTime.now()
        );
    }

    private boolean isDuplicatePlacement(Order existingOrder, PlaceOrderRequest request) {
        if (!ACTIVE_STATUSES.contains(existingOrder.getOrderStatus())) {
            return false;
        }
        if (!"PENDING".equalsIgnoreCase(existingOrder.getPaymentStatus())) {
            return false;
        }
        if (existingOrder.getOrderDate() == null
            || ChronoUnit.MINUTES.between(existingOrder.getOrderDate(), LocalDateTime.now()) > 10) {
            return false;
        }
        if (!existingOrder.getRestaurantId().equals(request.restaurantId())) {
            return false;
        }
        if (!existingOrder.getModeOfPayment().equalsIgnoreCase(request.modeOfPayment().trim())) {
            return false;
        }
        if (!existingOrder.getDeliveryAddress().equals(request.deliveryAddress().trim())) {
            return false;
        }
        if (!Objects.equals(normalizeText(existingOrder.getSpecialInstructions()), normalizeText(request.specialInstructions()))) {
            return false;
        }

        BigDecimal totalAmount = calculateTotal(request.items());
        BigDecimal taxAmount = calculateTax(totalAmount);
        BigDecimal discount = normalizeMoney(request.discount() == null ? ZERO : request.discount());
        BigDecimal finalAmount = totalAmount.add(taxAmount).subtract(discount);

        if (existingOrder.getTotalAmount().compareTo(totalAmount) != 0
            || existingOrder.getDiscount().compareTo(discount) != 0
            || existingOrder.getFinalAmount().compareTo(finalAmount) != 0) {
            return false;
        }

        return sameItems(existingOrder.getItems(), request.items());
    }

    private boolean sameItems(List<OrderItem> existingItems, List<PlaceOrderItemRequest> requestItems) {
        if (existingItems.size() != requestItems.size()) {
            return false;
        }

        for (int index = 0; index < existingItems.size(); index++) {
            OrderItem existingItem = existingItems.get(index);
            PlaceOrderItemRequest requestItem = requestItems.get(index);

            if (!existingItem.getMenuItemId().equals(requestItem.menuItemId())) {
                return false;
            }
            if (!existingItem.getName().equals(requestItem.name().trim())) {
                return false;
            }
            if (existingItem.getPrice().compareTo(normalizeMoney(requestItem.price())) != 0) {
                return false;
            }
            if (!Objects.equals(existingItem.getQuantity(), requestItem.quantity())) {
                return false;
            }
            if (!Objects.equals(normalizeText(existingItem.getCustomization()), normalizeText(requestItem.customization()))) {
                return false;
            }
        }

        return true;
    }

    private void validateCartAndMenuState(PlaceOrderRequest request) {
        validateAgainstCart(request);
        validateAgainstCurrentMenu(request);
    }

    private void validateAgainstCart(PlaceOrderRequest request) {
        if (cartClient == null) {
            return;
        }
        CartSnapshotDto cart = null;
        try {
            cart = cartClient.getCartByCustomerId(request.customerId());
        } catch (RuntimeException ignored) {
            return;
        }

        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            return;
        }

        if (!Objects.equals(cart.restaurantId(), request.restaurantId())) {
            throw new BadRequestException("Cart restaurant does not match the restaurant selected for checkout");
        }

        List<CartItemSnapshotDto> cartItems = cart.items();
        if (cartItems.size() != request.items().size()) {
            throw new BadRequestException("Checkout items do not match the current cart state");
        }

        Map<String, CartItemSnapshotDto> cartBySignature = cartItems.stream()
            .collect(Collectors.toMap(this::cartSignature, item -> item, (left, right) -> left));

        for (PlaceOrderItemRequest requestItem : request.items()) {
            CartItemSnapshotDto cartItem = cartBySignature.get(requestSignature(requestItem));
            if (cartItem == null || !Objects.equals(cartItem.quantity(), requestItem.quantity())) {
                throw new BadRequestException("Checkout items do not match the current cart state");
            }
        }
    }

    private void validateAgainstCurrentMenu(PlaceOrderRequest request) {
        if (menuClient == null) {
            return;
        }
        for (PlaceOrderItemRequest requestItem : request.items()) {
            var menuItem = menuClient.getItemById(requestItem.menuItemId().intValue());
            if (menuItem == null || !Boolean.TRUE.equals(menuItem.isAvailable())) {
                throw new BadRequestException("One or more menu items are currently unavailable");
            }
            if (!Objects.equals(menuItem.restaurantId().longValue(), request.restaurantId())) {
                throw new BadRequestException("One or more menu items do not belong to the selected restaurant");
            }

            BigDecimal currentPrice = resolveMenuPrice(menuItem);
            if (currentPrice.compareTo(normalizeMoney(requestItem.price())) != 0) {
                throw new BadRequestException("One or more menu item prices have changed. Please review your cart and try again");
            }
        }
    }

    private BigDecimal resolveMenuPrice(com.quickbite.orderservice.client.dto.MenuItemSnapshotDto menuItem) {
        Double discountedPrice = menuItem.discountedPrice();
        double resolvedPrice = discountedPrice != null && discountedPrice > 0 ? discountedPrice : menuItem.price();
        return normalizeMoney(BigDecimal.valueOf(resolvedPrice));
    }

    private String cartSignature(CartItemSnapshotDto item) {
        return item.menuItemId() + "|" + normalizeText(item.customization());
    }

    private String requestSignature(PlaceOrderItemRequest item) {
        return item.menuItemId() + "|" + normalizeText(item.customization());
    }

    private void clearCustomerCartSafely(Long customerId) {
        if (cartClient == null) {
            return;
        }
        try {
            cartClient.clearCart(customerId);
        } catch (RuntimeException ignored) {
            // Preserve order placement success even if cart cleanup is temporarily unavailable.
        }
    }

    private String routingKeyForStatus(OrderStatus status) {
        return switch (status) {
            case CONFIRMED -> QuickbiteOrderMessagingConstants.ORDER_CONFIRMED_ROUTING_KEY;
            case PREPARING -> "order.preparing";
            case READY_FOR_PICKUP -> "order.ready_for_pickup";
            case PICKED_UP -> "order.pickedup";
            case OUT_FOR_DELIVERY -> "order.out_for_delivery";
            case DELIVERED -> QuickbiteOrderMessagingConstants.ORDER_COMPLETED_ROUTING_KEY;
            default -> null;
        };
    }

    private void ensureRestaurantAcceptingOrders(Long restaurantId) {
        RestaurantRealtimeDto restaurant = restaurantClient.getRestaurantById(restaurantId);
        if (restaurant == null || !Boolean.TRUE.equals(restaurant.isApproved()) || !Boolean.TRUE.equals(restaurant.isOpen())) {
            throw new BadRequestException("This restaurant is currently closed and not accepting orders");
        }
    }

    private Long resolveRestaurantOwnerId(Long restaurantId) {
        try {
            RestaurantRealtimeDto restaurant = restaurantClient.getRestaurantById(restaurantId);
            return restaurant == null ? null : restaurant.ownerId();
        } catch (RuntimeException exception) {
            return null;
        }
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
