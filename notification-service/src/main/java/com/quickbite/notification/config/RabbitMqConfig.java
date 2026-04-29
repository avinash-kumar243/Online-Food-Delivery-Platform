package com.quickbite.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EVENTS_QUEUE = "order_events";
    public static final String DELIVERY_EVENTS_QUEUE = "delivery_events";

    @Bean
    public Queue orderEventsQueue() {
        return new Queue(ORDER_EVENTS_QUEUE, true);
    }

    @Bean
    public Queue deliveryEventsQueue() {
        return new Queue(DELIVERY_EVENTS_QUEUE, true);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
