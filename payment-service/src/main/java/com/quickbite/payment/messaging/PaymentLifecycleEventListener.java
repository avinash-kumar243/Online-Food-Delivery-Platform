package com.quickbite.payment.messaging;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.enums.PaymentStatus;
import com.quickbite.payment.messaging.dto.OrderEventDTO;
import com.quickbite.payment.repository.PaymentRepository;
import com.rabbitmq.client.Channel;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class PaymentLifecycleEventListener {

    @PersistenceContext
    private EntityManager entityManager;

    private final PaymentRepository paymentRepository;

    public PaymentLifecycleEventListener(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    @RabbitListener(
        queues = QuickbiteOrderMessagingConstants.ORDER_CREATED_QUEUE,
        containerFactory = "manualAckRabbitListenerContainerFactory"
    )
    public void handleOrderCreated(OrderEventDTO event, Message message, Channel channel) throws IOException {
        try {
            paymentRepository.findByOrderId(event.orderId()).orElseGet(() -> createPendingPayment(event));
            ack(channel, message);
        } catch (Exception exception) {
            reject(channel, message);
            throw exception;
        }
    }

    // Legacy compatibility for existing tests and older order completion flows.
    public void handleOrderDelivered(OrderEventDTO event, Message message, Channel channel) throws IOException {
        try {
            paymentRepository.findByOrderId(event.orderId()).ifPresent(payment -> {
                if (payment.getMode() != null && payment.getMode().name().equals("COD") && payment.getStatus() == PaymentStatus.PENDING) {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
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

    private Payment createPendingPayment(OrderEventDTO event) {
        try {
            return paymentRepository.saveAndFlush(
                Payment.builder()
                    .orderId(event.orderId())
                    .customerId(event.customerId())
                    .amount(event.totalAmount())
                    .status(PaymentStatus.PENDING)
                    .currency("INR")
                    .build()
            );
        } catch (DataIntegrityViolationException exception) {
            entityManager.clear();
            return paymentRepository.findByOrderId(event.orderId()).orElseThrow(() -> exception);
        }
    }

    private void reject(Channel channel, Message message) throws IOException {
        channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
    }
}
