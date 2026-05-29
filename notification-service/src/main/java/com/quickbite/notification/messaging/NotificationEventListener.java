package com.quickbite.notification.messaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.quickbite.notification.config.RabbitMqConfig;
import com.quickbite.notification.dto.NotificationEvent;
import com.quickbite.notification.service.NotificationService;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private static final String CHANNEL_APP = "APP";

    private final NotificationService notificationService;

    @RabbitListener(
        queues = RabbitMqConfig.NOTIFICATION_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleLifecycleEvent(Map<String, Object> payload, Message message, Channel channel) throws IOException {
        try {
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            for (NotificationEvent event : buildEvents(routingKey, payload)) {
                notificationService.processEvent(event);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception exception) {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            throw exception;
        }
    }

    private List<NotificationEvent> buildEvents(String routingKey, Map<String, Object> payload) {
        List<NotificationEvent> events = new ArrayList<>();
        String orderId = stringValue(payload.get("orderId"));
        String restaurantId = stringValue(payload.get("restaurantId"));
        String deliveryId = stringValue(payload.get("deliveryAgentId"));

        switch (routingKey) {
            case "order.created" -> {
                addEvent(events, longValue(payload.get("customerId")), "CUSTOMER", "ORDER_CREATED",
                    "Order placed", "Your order has been placed successfully.", orderId, "ORDER", orderId, deliveryId);
                addEvent(events, longValue(payload.get("restaurantOwnerId")), "RESTAURANT_OWNER", "ORDER_CREATED",
                    "New order received", "A new order has been placed for your restaurant.", restaurantId, "RESTAURANT", orderId, deliveryId);
            }
            case "payment.completed" -> addEvent(events, longValue(payload.get("customerId")), "CUSTOMER", "PAYMENT_COMPLETED",
                "Payment completed", "Your payment was completed successfully.", orderId, "ORDER", orderId, deliveryId);
            case "delivery.assigned" -> {
                addEvent(events, longValue(payload.get("customerId")), "CUSTOMER", "DELIVERY_ASSIGNED",
                    "Delivery assigned", "A delivery partner has been assigned to your order.", orderId, "ORDER", orderId, deliveryId);
                addEvent(events, longValue(payload.get("restaurantOwnerId")), "RESTAURANT_OWNER", "DELIVERY_ASSIGNED",
                    "Delivery assigned", "A delivery partner has been assigned to pick up the order.", restaurantId, "RESTAURANT", orderId, deliveryId);
                addEvent(events, longValue(payload.get("deliveryPartnerUserId")), "DELIVERY_PARTNER", "DELIVERY_ASSIGNED",
                    "Delivery assigned", "A new delivery has been assigned to you.", deliveryId, "DELIVERY", orderId, deliveryId);
            }
            case "delivery.completed" -> {
                addEvent(events, longValue(payload.get("customerId")), "CUSTOMER", "DELIVERY_COMPLETED",
                    "Order delivered", "Your order has been delivered successfully.", orderId, "ORDER", orderId, deliveryId);
                addEvent(events, longValue(payload.get("restaurantOwnerId")), "RESTAURANT_OWNER", "DELIVERY_COMPLETED",
                    "Order delivered", "An order from your restaurant has been delivered successfully.", restaurantId, "RESTAURANT", orderId, deliveryId);
                addEvent(events, longValue(payload.get("deliveryPartnerUserId")), "DELIVERY_PARTNER", "DELIVERY_COMPLETED",
                    "Delivery completed", "Your delivery has been completed successfully.", deliveryId, "DELIVERY", orderId, deliveryId);
            }
            case "restaurant.approved" -> addEvent(events, longValue(payload.get("ownerId")), "RESTAURANT_OWNER", "RESTAURANT_APPROVED",
                "Restaurant approved", "Your restaurant has been approved and is ready to receive orders.", restaurantId, "RESTAURANT", null, null);
            default -> {
            }
        }

        return events;
    }

    private void addEvent(
        List<NotificationEvent> events,
        Long recipientId,
        String recipientRole,
        String type,
        String title,
        String message,
        String relatedId,
        String relatedType,
        String orderId,
        String deliveryId
    ) {
        if (recipientId == null) {
            return;
        }

        NotificationEvent event = new NotificationEvent();
        event.setRecipientId(recipientId);
        event.setRecipientRole(recipientRole);
        event.setType(type);
        event.setChannel(CHANNEL_APP);
        event.setTitle(title);
        event.setMessage(message);
        event.setRelatedId(relatedId);
        event.setRelatedType(relatedType);
        event.setOrderId(orderId);
        event.setDeliveryId(deliveryId);
        events.add(event);
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
}
