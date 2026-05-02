package com.quickbite.auth.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.quickbite.auth.messaging.QuickbiteNotificationMessagingConstants;
import com.quickbite.auth.messaging.dto.PasswordResetOtpEmailEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final RabbitTemplate rabbitTemplate;

    public void sendOtpEmail(String to, String otp) {
        rabbitTemplate.convertAndSend(
            QuickbiteNotificationMessagingConstants.NOTIFICATION_EXCHANGE,
            QuickbiteNotificationMessagingConstants.PASSWORD_RESET_OTP_ROUTING_KEY,
            new PasswordResetOtpEmailEvent(
            to,
            otp,
            1
            )
        );
    }
}
