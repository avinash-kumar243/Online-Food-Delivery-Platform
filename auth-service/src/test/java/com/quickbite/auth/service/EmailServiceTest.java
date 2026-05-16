package com.quickbite.auth.service;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.quickbite.auth.messaging.QuickbiteNotificationMessagingConstants;
import com.quickbite.auth.messaging.dto.UserLifecycleEmailEvent;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendUserCreatedEmail_Success() {
        emailService.sendUserCreatedEmail(1L, "Avinash", "avi@test.com", "CUSTOMER");

        verify(rabbitTemplate).convertAndSend(
                eq(QuickbiteNotificationMessagingConstants.NOTIFICATION_EXCHANGE),
                eq(QuickbiteNotificationMessagingConstants.USER_CREATED_ROUTING_KEY),
                argThat((UserLifecycleEmailEvent event) ->
                        event.userId().equals(1L) &&
                                event.fullName().equals("Avinash") &&
                                event.email().equals("avi@test.com") &&
                                event.role().equals("CUSTOMER") &&
                                event.eventType().equals("USER_CREATED")
                )
        );
    }

    @Test
    void sendUserSuspendedEmail_Success() {
        emailService.sendUserSuspendedEmail(2L, "John", "john@test.com", "ADMIN");

        verify(rabbitTemplate).convertAndSend(
                eq(QuickbiteNotificationMessagingConstants.NOTIFICATION_EXCHANGE),
                eq(QuickbiteNotificationMessagingConstants.USER_SUSPENDED_ROUTING_KEY),
                argThat((UserLifecycleEmailEvent event) ->
                        event.userId().equals(2L) &&
                                event.eventType().equals("USER_SUSPENDED")
                )
        );
    }

    @Test
    void sendUserReactivatedEmail_Success() {
        emailService.sendUserReactivatedEmail(3L, "Jane", "jane@test.com", "CUSTOMER");

        verify(rabbitTemplate).convertAndSend(
                eq(QuickbiteNotificationMessagingConstants.NOTIFICATION_EXCHANGE),
                eq(QuickbiteNotificationMessagingConstants.USER_REACTIVATED_ROUTING_KEY),
                any(UserLifecycleEmailEvent.class) // no ambiguity here
        );
    }

    @Test
    void sendUserDeletedEmail_Success() {
        emailService.sendUserDeletedEmail(4L, "Mark", "mark@test.com", "CUSTOMER");

        verify(rabbitTemplate).convertAndSend(
                eq(QuickbiteNotificationMessagingConstants.NOTIFICATION_EXCHANGE),
                eq(QuickbiteNotificationMessagingConstants.USER_DELETED_ROUTING_KEY),
                any(UserLifecycleEmailEvent.class)
        );
    }
}