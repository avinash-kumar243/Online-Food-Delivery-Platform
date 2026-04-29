package com.quickbite.notification.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.quickbite.notification.config.RabbitMqConfig;
import com.quickbite.notification.dto.NotificationEvent;
import com.quickbite.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = RabbitMqConfig.ORDER_EVENTS_QUEUE)
    public void handleOrderEvent(NotificationEvent event) {
        notificationService.processEvent(event);
    }

    @RabbitListener(queues = RabbitMqConfig.DELIVERY_EVENTS_QUEUE)
    public void handleDeliveryEvent(NotificationEvent event) {
        notificationService.processEvent(event);
    }
}
