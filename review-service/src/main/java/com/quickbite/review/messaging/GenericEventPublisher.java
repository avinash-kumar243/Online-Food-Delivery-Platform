package com.quickbite.review.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GenericEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void send(String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(QuickbiteOrderMessagingConstants.NOTIFICATION_EXCHANGE, routingKey, payload);
    }
}
