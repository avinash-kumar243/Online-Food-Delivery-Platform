package com.quickbite.delivery.messaging;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.MessageConverter;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    TopicExchange quickbiteOrderExchange() {
        return new TopicExchange(QuickbiteOrderMessagingConstants.ORDER_EXCHANGE, true, false);
    }

    @Bean
    TopicExchange quickbiteOrderDeadLetterExchange() {
        return new TopicExchange(QuickbiteOrderMessagingConstants.ORDER_DLX, true, false);
    }

    @Bean
    Declarables deliveryDeclarables(TopicExchange quickbiteOrderExchange, TopicExchange quickbiteOrderDeadLetterExchange) {
        Queue restaurantAcceptedQueue = QueueBuilder.durable(QuickbiteOrderMessagingConstants.RESTAURANT_ACCEPTED_QUEUE)
            .withArguments(deadLetterArguments(QuickbiteOrderMessagingConstants.RESTAURANT_ACCEPTED_QUEUE + ".dlq"))
            .build();
        Queue restaurantAcceptedDlq = QueueBuilder.durable(QuickbiteOrderMessagingConstants.RESTAURANT_ACCEPTED_QUEUE + ".dlq").build();
        return new Declarables(
            restaurantAcceptedQueue,
            restaurantAcceptedDlq,
            BindingBuilder.bind(restaurantAcceptedQueue).to(quickbiteOrderExchange).with("restaurant.accepted"),
            BindingBuilder.bind(restaurantAcceptedDlq).to(quickbiteOrderDeadLetterExchange).with(restaurantAcceptedDlq.getName())
        );
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jacksonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter);
        rabbitTemplate.setMandatory(true);
        return rabbitTemplate;
    }

    @Bean
    SimpleRabbitListenerContainerFactory manualAckRabbitListenerContainerFactory(
        SimpleRabbitListenerContainerFactoryConfigurer configurer,
        ConnectionFactory connectionFactory,
        MessageConverter jacksonMessageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(jacksonMessageConverter);
        return factory;
    }

    private Map<String, Object> deadLetterArguments(String deadLetterQueueName) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("x-dead-letter-exchange", QuickbiteOrderMessagingConstants.ORDER_DLX);
        arguments.put("x-dead-letter-routing-key", deadLetterQueueName);
        return arguments;
    }
}
