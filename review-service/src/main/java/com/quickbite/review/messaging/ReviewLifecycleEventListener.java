package com.quickbite.review.messaging;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.review.client.OrderClient;
import com.quickbite.review.client.dto.OrderDto;
import com.quickbite.review.entity.ReviewEligibility;
import com.quickbite.review.messaging.dto.OrderEventDTO;
import com.quickbite.review.repository.ReviewEligibilityRepository;
import com.rabbitmq.client.Channel;

@Component
public class ReviewLifecycleEventListener {

    private final OrderClient orderClient;
    private final ReviewEligibilityRepository reviewEligibilityRepository;

    public ReviewLifecycleEventListener(OrderClient orderClient, ReviewEligibilityRepository reviewEligibilityRepository) {
        this.orderClient = orderClient;
        this.reviewEligibilityRepository = reviewEligibilityRepository;
    }

    @Transactional
    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.ORDER_DELIVERED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleOrderDelivered(OrderEventDTO event, Message message, Channel channel) throws IOException {
        try {
            OrderDto order = orderClient.getOrderById(event.orderId());
            reviewEligibilityRepository.findByOrderId(event.orderId())
                .map(existing -> updateEligibility(existing, order))
                .orElseGet(() -> reviewEligibilityRepository.save(ReviewEligibility.builder()
                    .orderId(event.orderId())
                    .customerId(order.getCustomerId())
                    .restaurantId(order.getRestaurantId())
                    .agentId(order.getAgentId())
                    .eligible(true)
                    .enabledAt(LocalDateTime.now())
                    .build()));
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception exception) {
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            throw exception;
        }
    }

    private ReviewEligibility updateEligibility(ReviewEligibility existing, OrderDto order) {
        existing.setCustomerId(order.getCustomerId());
        existing.setRestaurantId(order.getRestaurantId());
        existing.setAgentId(order.getAgentId());
        existing.setEligible(true);
        existing.setEnabledAt(LocalDateTime.now());
        return reviewEligibilityRepository.save(existing);
    }
}
