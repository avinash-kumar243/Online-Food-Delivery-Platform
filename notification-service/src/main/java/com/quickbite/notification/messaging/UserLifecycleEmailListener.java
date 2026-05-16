package com.quickbite.notification.messaging;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.quickbite.notification.config.RabbitMqConfig;
import com.quickbite.notification.dto.UserLifecycleEmailRequest;
import com.quickbite.notification.messaging.dto.UserLifecycleEmailEvent;
import com.quickbite.notification.service.NotificationService;
import com.rabbitmq.client.Channel;

@Component
public class UserLifecycleEmailListener {

    private final NotificationService notificationService;

    public UserLifecycleEmailListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(
        queues = RabbitMqConfig.USER_LIFECYCLE_EMAIL_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleUserLifecycleEmail(
        UserLifecycleEmailEvent event,
        Message message,
        Channel channel
    ) throws IOException {
        try {
            UserLifecycleEmailRequest request = new UserLifecycleEmailRequest();
            request.setUserId(event.userId());
            request.setFullName(event.fullName());
            request.setTo(event.email());
            request.setRole(event.role());
            request.setEventType(event.eventType());
            notificationService.sendUserLifecycleEmail(request);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception exception) {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            throw exception;
        }
    }
}
