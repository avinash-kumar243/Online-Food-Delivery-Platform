package com.quickbite.notification.messaging;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.quickbite.notification.client.AuthServiceClient;
import com.quickbite.notification.client.DeliveryServiceClient;
import com.quickbite.notification.client.RestaurantServiceClient;
import com.quickbite.notification.client.dto.DeliveryAgentResponseDto;
import com.quickbite.notification.client.dto.InternalUserSummaryDto;
import com.quickbite.notification.client.dto.RestaurantResponseDto;
import com.quickbite.notification.config.RabbitMqConfig;
import com.quickbite.notification.dto.NotificationEvent;
import com.quickbite.notification.service.NotificationService;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final String CHANNEL_APP = "APP";
    private static final String ROLE_CUSTOMER = "CUSTOMER";
    private static final String ROLE_OWNER = "RESTAURANT_OWNER";
    private static final String ROLE_PARTNER = "DELIVERY_PARTNER";
    private static final String ROLE_ADMIN = "ADMIN";

    private final NotificationService notificationService;
    private final AuthServiceClient authServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final DeliveryServiceClient deliveryServiceClient;

    @RabbitListener(
        queues = RabbitMqConfig.NOTIFICATION_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleLifecycleEvent(Map<String, Object> payload, Message message, Channel channel) throws IOException {
        try {
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            for (NotificationPayload notification : buildNotifications(routingKey, payload)) {
                notificationService.processEvent(notification.toEvent());
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception exception) {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            throw exception;
        }
    }

    private List<NotificationPayload> buildNotifications(String routingKey, Map<String, Object> payload) {
        Map<String, NotificationPayload> notifications = new LinkedHashMap<>();
        String referenceId = stringValue(payload.get("orderId"));

        switch (routingKey) {
            case "order.created" -> {
                addCustomerNotification(
                    notifications,
                    payload,
                    "ORDER_PLACED",
                    "Order Placed",
                    "Your order has been placed successfully and sent to the restaurant.",
                    referenceId
                );
                addRestaurantOwnerNotification(
                    notifications,
                    payload,
                    "ORDER_PLACED",
                    "New Order Received",
                    "A new order has been placed for your restaurant. Please review and accept the order.",
                    referenceId
                );
                addAdminNotifications(
                    notifications,
                    "ORDER_PLACED",
                    "New Order Placed",
                    "A new customer order has been placed on the platform.",
                    referenceId
                );
            }
            case "payment.success" -> {
                addCustomerNotification(
                    notifications,
                    payload,
                    "PAYMENT_SUCCESS",
                    "Payment Successful",
                    "Your payment was completed successfully. Your order has been placed and sent to the restaurant.",
                    referenceId
                );
                addAdminNotifications(
                    notifications,
                    "PAYMENT_SUCCESS",
                    "Payment Successful",
                    "A customer payment was completed successfully.",
                    referenceId
                );
            }
            case "payment.failed" -> {
                addCustomerNotification(
                    notifications,
                    payload,
                    "PAYMENT_FAILED",
                    "Payment Failed",
                    "Your payment could not be completed. Please try again to place the order.",
                    referenceId
                );
                addAdminNotifications(
                    notifications,
                    "PAYMENT_FAILED",
                    "Payment Failed",
                    "A customer payment failed and may need attention.",
                    referenceId
                );
            }
            case "restaurant.accepted" -> {
                addCustomerNotification(
                    notifications,
                    payload,
                    "ORDER_ACCEPTED",
                    "Order Accepted",
                    "Your order has been accepted by the restaurant and will be prepared soon.",
                    referenceId
                );
                addAdminNotifications(
                    notifications,
                    "ORDER_ACCEPTED",
                    "Restaurant Accepted Order",
                    "A restaurant has accepted a customer order.",
                    referenceId
                );
            }
            case "order.preparing" -> {
                addCustomerNotification(
                    notifications,
                    payload,
                    "ORDER_PREPARING",
                    "Order Preparing",
                    "Your food is being prepared by the restaurant.",
                    referenceId
                );
                addAdminNotifications(
                    notifications,
                    "ORDER_PREPARING",
                    "Order Preparing",
                    "A restaurant has started preparing an order.",
                    referenceId
                );
            }
            case "order.ready_for_pickup" -> {
                addCustomerNotification(
                    notifications,
                    payload,
                    "ORDER_READY_FOR_PICKUP",
                    "Order Ready for Pickup",
                    "Your order is ready and waiting for a delivery partner.",
                    referenceId
                );
                addAvailableDeliveryPartnerNotifications(
                    notifications,
                    "ORDER_READY_FOR_PICKUP",
                    "Order Ready for Pickup",
                    "A new order is ready for pickup. Accept it to start delivery.",
                    referenceId
                );
                addAdminNotifications(
                    notifications,
                    "ORDER_READY_FOR_PICKUP",
                    "Order Ready for Pickup",
                    "An order is ready for pickup and waiting for delivery assignment.",
                    referenceId
                );
            }
            case "delivery.assigned" -> {
                addCustomerNotification(
                    notifications,
                    payload,
                    "DELIVERY_PARTNER_ASSIGNED",
                    "Delivery Partner Assigned",
                    "A delivery partner has accepted your order and will pick it up soon.",
                    referenceId
                );
                addRestaurantOwnerNotification(
                    notifications,
                    payload,
                    "DELIVERY_PARTNER_ASSIGNED",
                    "Delivery Partner Assigned",
                    "A delivery partner has accepted the order and will arrive for pickup.",
                    referenceId
                );
                addAdminNotifications(
                    notifications,
                    "DELIVERY_PARTNER_ASSIGNED",
                    "Order Assigned",
                    "A delivery partner has accepted an order for delivery.",
                    referenceId
                );
            }
            case "order.out_for_delivery" -> {
                addCustomerNotification(
                    notifications,
                    payload,
                    "OUT_FOR_DELIVERY",
                    "Out for Delivery",
                    "Your order has been picked up and is on the way.",
                    referenceId
                );
                addRestaurantOwnerNotification(
                    notifications,
                    payload,
                    "OUT_FOR_DELIVERY",
                    "Order Picked Up",
                    "The delivery partner has picked up the order and is out for delivery.",
                    referenceId
                );
                addAdminNotifications(
                    notifications,
                    "OUT_FOR_DELIVERY",
                    "Order Out for Delivery",
                    "An order has been picked up and is now out for delivery.",
                    referenceId
                );
            }
            case "order.delivered" -> {
                addCustomerNotification(
                    notifications,
                    payload,
                    "ORDER_DELIVERED",
                    "Order Delivered",
                    "Your order has been delivered successfully. Enjoy your meal!",
                    referenceId
                );
                addRestaurantOwnerNotification(
                    notifications,
                    payload,
                    "ORDER_DELIVERED",
                    "Order Delivered",
                    "The order from your restaurant has been delivered successfully.",
                    referenceId
                );
                addAdminNotifications(
                    notifications,
                    "ORDER_DELIVERED",
                    "Order Completed",
                    "An order has been successfully delivered.",
                    referenceId
                );
            }
            case "order.cancelled" -> {
                addCustomerNotification(
                    notifications,
                    payload,
                    "ORDER_CANCELLED",
                    "Order Cancelled",
                    "Your order has been cancelled.",
                    referenceId
                );
                addRestaurantOwnerNotification(
                    notifications,
                    payload,
                    "ORDER_CANCELLED",
                    "Order Cancelled",
                    "A restaurant order has been cancelled.",
                    referenceId
                );
                addAdminNotifications(
                    notifications,
                    "ORDER_CANCELLED",
                    "Order Cancelled",
                    "An order has been cancelled on the platform.",
                    referenceId
                );
            }
            default -> {
            }
        }

        return List.copyOf(notifications.values());
    }

    private void addCustomerNotification(
        Map<String, NotificationPayload> notifications,
        Map<String, Object> payload,
        String type,
        String title,
        String message,
        String referenceId
    ) {
        Long customerId = longValue(payload.get("customerId"));
        if (customerId != null) {
            putNotification(notifications, customerId, ROLE_CUSTOMER, type, title, message, referenceId);
        }
    }

    private void addRestaurantOwnerNotification(
        Map<String, NotificationPayload> notifications,
        Map<String, Object> payload,
        String type,
        String title,
        String message,
        String referenceId
    ) {
        Long restaurantId = longValue(payload.get("restaurantId"));
        if (restaurantId == null) {
            return;
        }

        try {
            RestaurantResponseDto restaurant = restaurantServiceClient.getRestaurantById(restaurantId);
            if (restaurant != null && restaurant.ownerId() != null) {
                putNotification(notifications, restaurant.ownerId(), ROLE_OWNER, type, title, message, referenceId);
            }
        } catch (RuntimeException ignored) {
            // Notification fan-out should not stop the whole event if a downstream lookup fails.
        }
    }

    private void addAvailableDeliveryPartnerNotifications(
        Map<String, NotificationPayload> notifications,
        String type,
        String title,
        String message,
        String referenceId
    ) {
        try {
            for (DeliveryAgentResponseDto agent : deliveryServiceClient.getAvailableAgents()) {
                if (agent != null && agent.userId() != null) {
                    putNotification(notifications, agent.userId(), ROLE_PARTNER, type, title, message, referenceId);
                }
            }
        } catch (RuntimeException ignored) {
            // Ignore transient delivery lookup failures and continue processing the event.
        }
    }

    private void addAdminNotifications(
        Map<String, NotificationPayload> notifications,
        String type,
        String title,
        String message,
        String referenceId
    ) {
        try {
            for (InternalUserSummaryDto admin : authServiceClient.getUsersByRole(ROLE_ADMIN)) {
                if (admin != null && admin.userId() != null && Boolean.TRUE.equals(admin.isActive())) {
                    putNotification(notifications, admin.userId(), ROLE_ADMIN, type, title, message, referenceId);
                }
            }
        } catch (RuntimeException ignored) {
            // Ignore transient auth lookup failures and continue processing the event.
        }
    }

    private void putNotification(
        Map<String, NotificationPayload> notifications,
        Long recipientId,
        String recipientRole,
        String type,
        String title,
        String message,
        String referenceId
    ) {
        String key = recipientRole + ":" + recipientId + ":" + type + ":" + referenceId;
        notifications.putIfAbsent(key, new NotificationPayload(
            recipientId,
            recipientRole,
            type,
            title,
            message,
            referenceId,
            "ORDER"
        ));
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private record NotificationPayload(
        Long recipientId,
        String recipientRole,
        String type,
        String title,
        String message,
        String relatedId,
        String relatedType
    ) {
        private NotificationEvent toEvent() {
            NotificationEvent event = new NotificationEvent();
            event.setRecipientId(recipientId);
            event.setRecipientRole(recipientRole);
            event.setType(type);
            event.setChannel(CHANNEL_APP);
            event.setTitle(title);
            event.setMessage(message);
            event.setRelatedId(relatedId);
            event.setRelatedType(relatedType);
            return event;
        }
    }
}
