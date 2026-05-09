package com.quickbite.restaurant.messaging;

import java.io.IOException;

import com.quickbite.restaurant.messaging.dto.OrderEventDTO;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.quickbite.restaurant.messaging.dto.DeliveryEventDTO;
import com.quickbite.restaurant.messaging.dto.PaymentEventDTO;
import com.rabbitmq.client.Channel;

@Component
public class RestaurantLifecycleEventListener {

    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.ORDER_CREATED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleOrderCreated(OrderEventDTO event, Message message, Channel channel) throws IOException {
        ack(channel, message);
    }

    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.PAYMENT_SUCCESS_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handlePaymentSuccess(PaymentEventDTO event, Message message, Channel channel) throws IOException {
        try {
            ack(channel, message);
        } catch (Exception exception) {
            reject(channel, message);
            throw exception;
        }
    }

    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.DELIVERY_ASSIGNED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleDeliveryAssigned(DeliveryEventDTO event, Message message, Channel channel) throws IOException {
        ack(channel, message);
    }

    private void ack(Channel channel, Message message) throws IOException {
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    private void reject(Channel channel, Message message) throws IOException {
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
    }
}
