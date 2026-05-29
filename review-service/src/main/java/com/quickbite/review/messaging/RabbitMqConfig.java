package com.quickbite.review.messaging;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
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
    Declarables reviewDeclarables(TopicExchange quickbiteOrderExchange, TopicExchange quickbiteOrderDeadLetterExchange) {
        var orderCompletedQueue = QueueBuilder.durable(QuickbiteOrderMessagingConstants.ORDER_COMPLETED_QUEUE)
            .withArguments(deadLetterArguments(QuickbiteOrderMessagingConstants.ORDER_COMPLETED_QUEUE + ".dlq"))
            .build();
        var orderCompletedDlq = QueueBuilder.durable(QuickbiteOrderMessagingConstants.ORDER_COMPLETED_QUEUE + ".dlq").build();
        var deliveryCompletedQueue = QueueBuilder.durable(QuickbiteOrderMessagingConstants.DELIVERY_COMPLETED_QUEUE)
            .withArguments(deadLetterArguments(QuickbiteOrderMessagingConstants.DELIVERY_COMPLETED_QUEUE + ".dlq"))
            .build();
        var deliveryCompletedDlq = QueueBuilder.durable(QuickbiteOrderMessagingConstants.DELIVERY_COMPLETED_QUEUE + ".dlq").build();

        return new Declarables(
            orderCompletedQueue,
            orderCompletedDlq,
            deliveryCompletedQueue,
            deliveryCompletedDlq,
            BindingBuilder.bind(orderCompletedQueue).to(quickbiteOrderExchange).with("order.completed"),
            BindingBuilder.bind(deliveryCompletedQueue).to(quickbiteOrderExchange).with("delivery.completed"),
            BindingBuilder.bind(orderCompletedDlq).to(quickbiteOrderDeadLetterExchange).with(orderCompletedDlq.getName()),
            BindingBuilder.bind(deliveryCompletedDlq).to(quickbiteOrderDeadLetterExchange).with(deliveryCompletedDlq.getName())
        );
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
        factory.setConcurrentConsumers(2);
        factory.setMaxConcurrentConsumers(4);
        return factory;
    }

    private Map<String, Object> deadLetterArguments(String deadLetterQueueName) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("x-dead-letter-exchange", QuickbiteOrderMessagingConstants.ORDER_DLX);
        arguments.put("x-dead-letter-routing-key", deadLetterQueueName);
        return arguments;
    }
}
