package com.quickbite.auth.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.quickbite.auth.messaging.QuickbiteNotificationMessagingConstants;
import com.quickbite.auth.messaging.dto.PasswordResetOtpEmailEvent;
import com.quickbite.auth.messaging.dto.UserLifecycleEmailEvent;

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

    public void sendUserCreatedEmail(Long userId, String fullName, String email, String role) {
        sendUserLifecycleEmail(
            QuickbiteNotificationMessagingConstants.USER_CREATED_ROUTING_KEY,
            userId,
            fullName,
            email,
            role,
            "USER_CREATED"
        );
    }

    public void sendUserSuspendedEmail(Long userId, String fullName, String email, String role) {
        sendUserLifecycleEmail(
            QuickbiteNotificationMessagingConstants.USER_SUSPENDED_ROUTING_KEY,
            userId,
            fullName,
            email,
            role,
            "USER_SUSPENDED"
        );
    }

    public void sendUserReactivatedEmail(Long userId, String fullName, String email, String role) {
        sendUserLifecycleEmail(
            QuickbiteNotificationMessagingConstants.USER_REACTIVATED_ROUTING_KEY,
            userId,
            fullName,
            email,
            role,
            "USER_REACTIVATED"
        );
    }

    public void sendUserDeletedEmail(Long userId, String fullName, String email, String role) {
        sendUserLifecycleEmail(
            QuickbiteNotificationMessagingConstants.USER_DELETED_ROUTING_KEY,
            userId,
            fullName,
            email,
            role,
            "USER_DELETED"
        );
    }

    private void sendUserLifecycleEmail(
        String routingKey,
        Long userId,
        String fullName,
        String email,
        String role,
        String eventType
    ) {
        rabbitTemplate.convertAndSend(
            QuickbiteNotificationMessagingConstants.NOTIFICATION_EXCHANGE,
            routingKey,
            new UserLifecycleEmailEvent(userId, fullName, email, role, eventType)
        );
    }
}
