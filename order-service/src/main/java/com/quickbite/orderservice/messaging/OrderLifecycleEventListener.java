package com.quickbite.orderservice.messaging;

import java.io.IOException;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.orderservice.entity.Order;
import com.quickbite.orderservice.entity.OrderStatus;
import com.quickbite.orderservice.messaging.dto.DeliveryEventDTO;
import com.quickbite.orderservice.messaging.dto.OrderEventDTO;
import com.quickbite.orderservice.messaging.dto.PaymentEventDTO;
import com.quickbite.orderservice.realtime.RealtimeNotifier;
import com.quickbite.orderservice.repository.OrderRepository;
import com.rabbitmq.client.Channel;

@Component
public class OrderLifecycleEventListener {

    private final OrderRepository orderRepository;
    private final RealtimeNotifier realtimeNotifier;

    public OrderLifecycleEventListener(OrderRepository orderRepository, RealtimeNotifier realtimeNotifier) {
        this.orderRepository = orderRepository;
        this.realtimeNotifier = realtimeNotifier;
    }

    @Transactional
    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.PAYMENT_SUCCESS_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handlePaymentSuccess(PaymentEventDTO event, Message message, Channel channel) throws IOException {
        try {
            orderRepository.findById(event.orderId()).ifPresent(order -> {
                order.setPaymentStatus(normalizeStatus(event.status(), "PAID"));
                if (order.getOrderStatus() == OrderStatus.PLACED) {
                    order.setOrderStatus(OrderStatus.CONFIRMED);
                }
                Order savedOrder = orderRepository.save(order);
                realtimeNotifier.publishPaymentUpdated(savedOrder, event);
            });
            ack(channel, message);
        } catch (Exception exception) {
            reject(channel, message);
            throw exception;
        }
    }

    @Transactional
    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.RESTAURANT_ACCEPTED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleRestaurantAccepted(OrderEventDTO event, Message message, Channel channel) throws IOException {
        try {
            orderRepository.findById(event.orderId()).ifPresent(order -> {
                if (order.getOrderStatus().ordinal() < OrderStatus.PREPARING.ordinal()) {
                    order.setOrderStatus(OrderStatus.PREPARING);
                    realtimeNotifier.publishOrderUpdated(orderRepository.save(order));
                }
            });
            ack(channel, message);
        } catch (Exception exception) {
            reject(channel, message);
            throw exception;
        }
    }

    @Transactional
    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.DELIVERY_ASSIGNED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handlePartnerAssigned(DeliveryEventDTO event, Message message, Channel channel) throws IOException {
        try {
            orderRepository.findById(event.orderId()).ifPresent(order -> {
                order.setDeliveryAgentId(event.agentId());
                if (order.getOrderStatus() != OrderStatus.DELIVERED && order.getOrderStatus() != OrderStatus.CANCELLED) {
                    order.setOrderStatus(OrderStatus.READY_FOR_PICKUP);
                }
                realtimeNotifier.publishOrderUpdated(orderRepository.save(order));
            });
            ack(channel, message);
        } catch (Exception exception) {
            reject(channel, message);
            throw exception;
        }
    }

    @Transactional
    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.ORDER_PICKED_UP_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleOrderPickedUp(DeliveryEventDTO event, Message message, Channel channel) throws IOException {
        try {
            orderRepository.findById(event.orderId()).ifPresent(order -> {
                order.setOrderStatus(OrderStatus.PICKED_UP);
                if (event.agentId() != null) {
                    order.setDeliveryAgentId(event.agentId());
                }
                realtimeNotifier.publishOrderUpdated(orderRepository.save(order));
            });
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
            orderRepository.findById(event.orderId()).ifPresent(order -> {
                order.setOrderStatus(OrderStatus.DELIVERED);
                realtimeNotifier.publishOrderUpdated(orderRepository.save(order));
            });
            ack(channel, message);
        } catch (Exception exception) {
            reject(channel, message);
            throw exception;
        }
    }

    private String normalizeStatus(String status, String fallback) {
        return status == null || status.isBlank() ? fallback : status.trim().toUpperCase();
    }

    private void ack(Channel channel, Message message) throws IOException {
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }

    private void reject(Channel channel, Message message) throws IOException {
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
    }
}
