package com.quickbite.orderservice.messaging;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
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
    Declarables orderLifecycleDeclarables(TopicExchange quickbiteOrderExchange, TopicExchange quickbiteOrderDeadLetterExchange) {
        Queue paymentSuccessQueue = durableQueue(QuickbiteOrderMessagingConstants.PAYMENT_SUCCESS_QUEUE);
        Queue restaurantAcceptedQueue = durableQueue(QuickbiteOrderMessagingConstants.RESTAURANT_ACCEPTED_QUEUE);
        Queue deliveryAssignedQueue = durableQueue(QuickbiteOrderMessagingConstants.DELIVERY_ASSIGNED_QUEUE);
        Queue orderPickedUpQueue = durableQueue(QuickbiteOrderMessagingConstants.ORDER_PICKED_UP_QUEUE);
        Queue orderDeliveredQueue = durableQueue(QuickbiteOrderMessagingConstants.ORDER_DELIVERED_QUEUE);

        Queue paymentSuccessDlq = deadLetterQueue(QuickbiteOrderMessagingConstants.PAYMENT_SUCCESS_QUEUE);
        Queue restaurantAcceptedDlq = deadLetterQueue(QuickbiteOrderMessagingConstants.RESTAURANT_ACCEPTED_QUEUE);
        Queue deliveryAssignedDlq = deadLetterQueue(QuickbiteOrderMessagingConstants.DELIVERY_ASSIGNED_QUEUE);
        Queue orderPickedUpDlq = deadLetterQueue(QuickbiteOrderMessagingConstants.ORDER_PICKED_UP_QUEUE);
        Queue orderDeliveredDlq = deadLetterQueue(QuickbiteOrderMessagingConstants.ORDER_DELIVERED_QUEUE);

        return new Declarables(
            paymentSuccessQueue,
            restaurantAcceptedQueue,
            deliveryAssignedQueue,
            orderPickedUpQueue,
            orderDeliveredQueue,
            paymentSuccessDlq,
            restaurantAcceptedDlq,
            deliveryAssignedDlq,
            orderPickedUpDlq,
            orderDeliveredDlq,
            BindingBuilder.bind(paymentSuccessQueue).to(quickbiteOrderExchange).with("payment.success"),
            BindingBuilder.bind(restaurantAcceptedQueue).to(quickbiteOrderExchange).with("restaurant.accepted"),
            BindingBuilder.bind(deliveryAssignedQueue).to(quickbiteOrderExchange).with("delivery.assigned"),
            BindingBuilder.bind(orderPickedUpQueue).to(quickbiteOrderExchange).with("order.pickedup"),
            BindingBuilder.bind(orderDeliveredQueue).to(quickbiteOrderExchange).with("order.delivered"),
            deadLetterBinding(paymentSuccessDlq, quickbiteOrderDeadLetterExchange),
            deadLetterBinding(restaurantAcceptedDlq, quickbiteOrderDeadLetterExchange),
            deadLetterBinding(deliveryAssignedDlq, quickbiteOrderDeadLetterExchange),
            deadLetterBinding(orderPickedUpDlq, quickbiteOrderDeadLetterExchange),
            deadLetterBinding(orderDeliveredDlq, quickbiteOrderDeadLetterExchange)
        );
    }

    private Queue durableQueue(String queueName) {
        return QueueBuilder.durable(queueName)
            .withArguments(deadLetterArguments(queueName + ".dlq"))
            .build();
    }

    private Queue deadLetterQueue(String queueName) {
        return QueueBuilder.durable(queueName + ".dlq").build();
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

    private Binding deadLetterBinding(Queue deadLetterQueue, TopicExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
            .to(deadLetterExchange)
            .with(deadLetterQueue.getName());
    }

    private Map<String, Object> deadLetterArguments(String deadLetterQueueName) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("x-dead-letter-exchange", QuickbiteOrderMessagingConstants.ORDER_DLX);
        arguments.put("x-dead-letter-routing-key", deadLetterQueueName);
        return arguments;
    }
}
