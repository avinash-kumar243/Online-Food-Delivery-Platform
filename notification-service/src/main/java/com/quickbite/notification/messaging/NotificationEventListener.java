package com.quickbite.notification.messaging;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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

    private final NotificationService notificationService;

    @RabbitListener(
        queues = RabbitMqConfig.NOTIFICATION_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleLifecycleEvent(Map<String, Object> payload, Message message, Channel channel) throws IOException {
        try {
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();
            for (Integer recipientId : resolveRecipients(payload)) {
                for (String channelType : Set.of("APP", "EMAIL", "SMS")) {
                    NotificationEvent event = new NotificationEvent();
                    event.setRecipientId(recipientId);
                    event.setType(resolveType(routingKey));
                    event.setChannel(channelType);
                    event.setTitle(buildTitle(routingKey));
                    event.setMessage(buildMessage(routingKey, payload));
                    event.setRelatedId(String.valueOf(payload.getOrDefault("orderId", "")));
                    event.setRelatedType("ORDER_EVENT");
                    notificationService.processEvent(event);
                }
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception exception) {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            throw exception;
        }
    }

    private Set<Integer> resolveRecipients(Map<String, Object> payload) {
        Set<Integer> recipients = new LinkedHashSet<>();
        addRecipient(recipients, payload.get("customerId"));
        addRecipient(recipients, payload.get("restaurantId"));
        addRecipient(recipients, payload.get("agentId"));
        if (recipients.isEmpty()) {
            recipients.add(1);
        }
        return recipients;
    }

    private void addRecipient(Set<Integer> recipients, Object value) {
        if (value instanceof Number number) {
            recipients.add(number.intValue());
        }
    }

    private String resolveType(String routingKey) {
        if (routingKey.startsWith("payment.")) {
            return "PAYMENT";
        }
        if (routingKey.startsWith("delivery.") || routingKey.startsWith("order.pickedup")) {
            return "DELIVERY";
        }
        return "ORDER";
    }

    private String buildTitle(String routingKey) {
        return switch (routingKey) {
            case "order.created" -> "Order created";
            case "payment.success" -> "Payment confirmed";
            case "restaurant.accepted" -> "Restaurant accepted your order";
            case "delivery.assigned" -> "Delivery partner assigned";
            case "order.pickedup" -> "Order picked up";
            case "order.delivered" -> "Order delivered";
            default -> "QuickBite update";
        };
    }

    private String buildMessage(String routingKey, Map<String, Object> payload) {
        return buildTitle(routingKey) + " for order " + payload.getOrDefault("orderId", "unknown");
    }
}
