package com.quickbite.notification.messaging;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.quickbite.notification.config.RabbitMqConfig;
import com.quickbite.notification.dto.PasswordResetOtpEmailRequest;
import com.quickbite.notification.messaging.dto.PasswordResetOtpEmailEvent;
import com.quickbite.notification.service.NotificationService;
import com.rabbitmq.client.Channel;

@Component
public class PasswordResetOtpEmailListener {

    private final NotificationService notificationService;

    public PasswordResetOtpEmailListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(
        queues = RabbitMqConfig.PASSWORD_RESET_OTP_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handlePasswordResetOtpEmail(
        PasswordResetOtpEmailEvent event,
        Message message,
        Channel channel
    ) throws IOException {
        try {
            PasswordResetOtpEmailRequest request = new PasswordResetOtpEmailRequest();
            request.setTo(event.to());
            request.setOtp(event.otp());
            request.setValidityInMinutes(event.validityInMinutes());
            notificationService.sendPasswordResetOtpEmail(request);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception exception) {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            throw exception;
        }
    }
}
