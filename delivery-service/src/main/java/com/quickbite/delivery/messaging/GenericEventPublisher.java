package com.quickbite.delivery.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class GenericEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public GenericEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void send(String routingKey, Object payload) {
        rabbitTemplate.convertAndSend(QuickbiteOrderMessagingConstants.ORDER_EXCHANGE, routingKey, payload);
    }
}
