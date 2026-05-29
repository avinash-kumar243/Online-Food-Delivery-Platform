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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReviewLifecycleEventListener {

    private final OrderClient orderClient;
    private final ReviewEligibilityRepository reviewEligibilityRepository;

    @Transactional
    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.ORDER_COMPLETED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleOrderCompleted(OrderEventDTO event, Message message, Channel channel) throws IOException {
        try {
            log.info("Received order completed event for orderId={}", event.orderId());
            OrderDto order = resolveOrder(event);
            if (order != null && order.getCustomerId() != null && order.getRestaurantId() != null && order.getAgentId() != null) {
                ReviewEligibility persistedEligibility = reviewEligibilityRepository.findByOrderId(event.orderId())
                    .map(existing -> updateEligibility(existing, order))
                    .orElseGet(() -> reviewEligibilityRepository.save(ReviewEligibility.builder()
                        .orderId(order.getOrderId())
                        .customerId(order.getCustomerId())
                        .restaurantId(order.getRestaurantId())
                        .agentId(order.getAgentId())
                        .eligible(true)
                        .enabledAt(LocalDateTime.now())
                        .build()));
                persistedEligibility.setEligible(true);
            }
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception exception) {
            log.error("Failed to process order delivered event for orderId={}", event.orderId(), exception);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            throw exception;
        }
    }

    // Legacy compatibility for existing tests and older publishers.
    public void handleOrderDelivered(OrderEventDTO event, Message message, Channel channel) throws IOException {
        handleOrderCompleted(event, message, channel);
    }

    @Transactional
    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.DELIVERY_COMPLETED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleDeliveryCompleted(OrderEventDTO event, Message message, Channel channel) throws IOException {
        handleOrderCompleted(event, message, channel);
    }

    private OrderDto resolveOrder(OrderEventDTO event) {
        if (event.customerId() != null && event.restaurantId() != null && event.deliveryAgentId() != null) {
            OrderDto order = new OrderDto();
            order.setOrderId(event.orderId());
            order.setCustomerId(event.customerId());
            order.setRestaurantId(event.restaurantId());
            order.setAgentId(event.deliveryAgentId());
            return order;
        }

        return orderClient.getOrderById(event.orderId());
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
