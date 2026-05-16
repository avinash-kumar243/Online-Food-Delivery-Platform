package com.quickbite.orderservice.realtime;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.quickbite.orderservice.client.DeliveryAgentClient;
import com.quickbite.orderservice.client.RestaurantClient;
import com.quickbite.orderservice.entity.Order;
import com.quickbite.orderservice.messaging.dto.PaymentEventDTO;

import feign.FeignException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RealtimeNotifier {

    private static final String USER_ORDER_DESTINATION = "/queue/orders";
    private static final String ADMIN_ORDER_DESTINATION = "/topic/admin/orders";
    private static final String ADMIN_PAYMENT_DESTINATION = "/topic/admin/payments";

    private final SimpMessagingTemplate messagingTemplate;
    private final RestaurantClient restaurantClient;
    private final DeliveryAgentClient deliveryAgentClient;

    private final Map<Long, Long> restaurantOwnerCache = new ConcurrentHashMap<>();
    private final Map<Long, Long> deliveryUserCache = new ConcurrentHashMap<>();

    public void publishOrderCreated(Order order) {
        publishOrderEvent(order, RealtimeEventType.ORDER_CREATED);
    }

    public void publishOrderUpdated(Order order) {
        publishOrderEvent(order, RealtimeEventType.ORDER_UPDATED);
    }

    public void publishPaymentUpdated(Order order, PaymentEventDTO paymentEvent) {
        afterCommit(() -> {
            publishOrderEventNow(order, RealtimeEventType.ORDER_UPDATED);
            messagingTemplate.convertAndSend(
                ADMIN_PAYMENT_DESTINATION,
                new PaymentRealtimeEvent(
                    RealtimeEventType.PAYMENT_UPDATED,
                    paymentEvent.orderId(),
                    paymentEvent.customerId(),
                    paymentEvent.restaurantId(),
                    paymentEvent.deliveryAgentId(),
                    paymentEvent.status(),
                    paymentEvent.amount(),
                    LocalDateTime.now()
                )
            );
        });
    }

    private void publishOrderEvent(Order order, RealtimeEventType type) {
        afterCommit(() -> publishOrderEventNow(order, type));
    }

    private void publishOrderEventNow(Order order, RealtimeEventType type) {
        OrderRealtimeEvent event = new OrderRealtimeEvent(
            type,
            order.getOrderId(),
            order.getCustomerId(),
            order.getRestaurantId(),
            order.getDeliveryAgentId(),
            order.getOrderStatus().name(),
            order.getPaymentStatus(),
            LocalDateTime.now()
        );

        messagingTemplate.convertAndSend(ADMIN_ORDER_DESTINATION, event);
        messagingTemplate.convertAndSendToUser(String.valueOf(order.getCustomerId()), USER_ORDER_DESTINATION, event);

        Long ownerId = resolveOwnerId(order.getRestaurantId());
        if (ownerId != null) {
            messagingTemplate.convertAndSendToUser(String.valueOf(ownerId), USER_ORDER_DESTINATION, event);
        }

        Long deliveryUserId = resolveDeliveryUserId(order.getDeliveryAgentId());
        if (deliveryUserId != null) {
            messagingTemplate.convertAndSendToUser(String.valueOf(deliveryUserId), USER_ORDER_DESTINATION, event);
        }
    }

    private Long resolveOwnerId(Long restaurantId) {
        if (restaurantId == null) {
            return null;
        }
        return restaurantOwnerCache.computeIfAbsent(restaurantId, id -> {
            try {
                var restaurant = restaurantClient.getRestaurantById(id);
                return restaurant != null ? restaurant.ownerId() : null;
            } catch (FeignException exception) {
                return null;
            }
        });
    }

    private Long resolveDeliveryUserId(Long deliveryAgentId) {
        if (deliveryAgentId == null) {
            return null;
        }
        return deliveryUserCache.computeIfAbsent(deliveryAgentId, id -> {
            try {
                var agent = deliveryAgentClient.getAgentById(id);
                return agent != null ? agent.userId() : null;
            } catch (FeignException exception) {
                return null;
            }
        });
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
