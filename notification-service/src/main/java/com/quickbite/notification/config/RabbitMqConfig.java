package com.quickbite.notification.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String ORDER_EXCHANGE = "quickbite.order.exchange";
    public static final String ORDER_DLX = "quickbite.order.dlx.exchange";
    public static final String NOTIFICATION_QUEUE = "quickbite.notification-service.all-events";
    public static final String NOTIFICATION_EXCHANGE = "quickbite.notification.exchange";
    public static final String NOTIFICATION_DLX = "quickbite.notification.dlx.exchange";
    public static final String PASSWORD_RESET_OTP_QUEUE = "quickbite.notification-service.password-reset-otp";
    public static final String PASSWORD_RESET_OTP_ROUTING_KEY = "auth.password-reset-otp";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange quickbiteOrderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange quickbiteOrderDeadLetterExchange() {
        return new TopicExchange(ORDER_DLX, true, false);
    }

    @Bean
    public TopicExchange quickbiteNotificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange quickbiteNotificationDeadLetterExchange() {
        return new TopicExchange(NOTIFICATION_DLX, true, false);
    }

    @Bean
    public Declarables notificationDeclarables(TopicExchange quickbiteOrderExchange, TopicExchange quickbiteOrderDeadLetterExchange) {
        var notificationQueue = QueueBuilder.durable(NOTIFICATION_QUEUE)
            .withArguments(deadLetterArguments(NOTIFICATION_QUEUE + ".dlq"))
            .build();
        var notificationDlq = QueueBuilder.durable(NOTIFICATION_QUEUE + ".dlq").build();
        return new Declarables(
            notificationQueue,
            notificationDlq,
            BindingBuilder.bind(notificationQueue).to(quickbiteOrderExchange).with("#"),
            BindingBuilder.bind(notificationDlq).to(quickbiteOrderDeadLetterExchange).with(notificationDlq.getName())
        );
    }

    @Bean
    public Declarables passwordResetOtpDeclarables(
        TopicExchange quickbiteNotificationExchange,
        TopicExchange quickbiteNotificationDeadLetterExchange
    ) {
        var passwordResetOtpQueue = QueueBuilder.durable(PASSWORD_RESET_OTP_QUEUE)
            .withArguments(notificationDeadLetterArguments(PASSWORD_RESET_OTP_QUEUE + ".dlq"))
            .build();
        var passwordResetOtpDlq = QueueBuilder.durable(PASSWORD_RESET_OTP_QUEUE + ".dlq").build();
        return new Declarables(
            passwordResetOtpQueue,
            passwordResetOtpDlq,
            BindingBuilder.bind(passwordResetOtpQueue).to(quickbiteNotificationExchange).with(PASSWORD_RESET_OTP_ROUTING_KEY),
            BindingBuilder.bind(passwordResetOtpDlq).to(quickbiteNotificationDeadLetterExchange).with(passwordResetOtpDlq.getName())
        );
    }

    @Bean
    public SimpleRabbitListenerContainerFactory manualAckRabbitListenerContainerFactory(
        SimpleRabbitListenerContainerFactoryConfigurer configurer,
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(messageConverter);
        return factory;
    }

    private Map<String, Object> deadLetterArguments(String deadLetterQueueName) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("x-dead-letter-exchange", ORDER_DLX);
        arguments.put("x-dead-letter-routing-key", deadLetterQueueName);
        return arguments;
    }

    private Map<String, Object> notificationDeadLetterArguments(String deadLetterQueueName) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("x-dead-letter-exchange", NOTIFICATION_DLX);
        arguments.put("x-dead-letter-routing-key", deadLetterQueueName);
        return arguments;
    }
}
