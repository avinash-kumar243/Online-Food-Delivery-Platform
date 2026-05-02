package com.quickbite.delivery.messaging;

import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.delivery.messaging.dto.OrderEventDTO;
import com.quickbite.delivery.repository.DeliveryRepository;
import com.rabbitmq.client.Channel;

@Component
public class DeliveryLifecycleEventListener {

    private final DeliveryRepository deliveryRepository;

    public DeliveryLifecycleEventListener(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.RESTAURANT_ACCEPTED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleRestaurantAccepted(OrderEventDTO event, Message message, Channel channel) throws IOException {
        try {
            ack(channel, message);
        } catch (Exception exception) {
            reject(channel, message);
            throw exception;
        }
    }

    @Transactional
    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.ORDER_DELIVERED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleOrderDelivered(OrderEventDTO event, Message message, Channel channel) throws IOException {
        try {
            deliveryRepository.findByActiveOrderId(event.orderId()).ifPresent(agent -> {
                agent.setActiveOrderId(null);
                agent.setAvailable(agent.getVerificationStatus() == null || agent.isVerified());
                agent.setTotalDeliveries(agent.getTotalDeliveries() + 1);
                deliveryRepository.save(agent);
            });
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
