package com.quickbite.delivery.messaging;

import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.quickbite.delivery.messaging.dto.OrderEventDTO;
import com.rabbitmq.client.Channel;

@Component
public class DeliveryLifecycleEventListener {

    public DeliveryLifecycleEventListener() {
    }

    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.ORDER_CREATED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleOrderCreated(OrderEventDTO event, Message message, Channel channel) throws IOException {
        try {
            ack(channel, message);
        } catch (Exception exception) {
            reject(channel, message);
            throw exception;
        }
    }

    private void ack(Channel channel, Message message) throws IOException {
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    private void reject(Channel channel, Message message) throws IOException {
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
    }
}
