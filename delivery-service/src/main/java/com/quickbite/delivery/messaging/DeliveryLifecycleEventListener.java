package com.quickbite.delivery.messaging;

import java.io.IOException;
import java.util.Comparator;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.quickbite.delivery.dto.OrderAssignmentRequest;
import com.quickbite.delivery.entity.VerificationStatus;
import com.quickbite.delivery.messaging.dto.OrderEventDTO;
import com.quickbite.delivery.repository.DeliveryRepository;
import com.quickbite.delivery.service.DeliveryService;
import com.rabbitmq.client.Channel;

@Component
public class DeliveryLifecycleEventListener {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryService deliveryService;

    public DeliveryLifecycleEventListener(DeliveryRepository deliveryRepository, DeliveryService deliveryService) {
        this.deliveryRepository = deliveryRepository;
        this.deliveryService = deliveryService;
    }

    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.RESTAURANT_ACCEPTED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleRestaurantAccepted(OrderEventDTO event, Message message, Channel channel) throws IOException {
        try {
            deliveryRepository.findByIsAvailableTrue().stream()
                .filter(agent -> agent.getVerificationStatus() == VerificationStatus.VERIFIED)
                .filter(agent -> agent.getActiveOrderId() == null)
                .sorted(Comparator.comparingInt(agent -> agent.getTotalDeliveries()))
                .findFirst()
                .ifPresent(agent -> deliveryService.assignOrder(new OrderAssignmentRequest(agent.getAgentId(), event.orderId())));
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
