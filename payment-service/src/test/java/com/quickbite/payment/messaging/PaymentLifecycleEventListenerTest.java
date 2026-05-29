package com.quickbite.payment.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.test.util.ReflectionTestUtils;

import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.enums.PaymentMode;
import com.quickbite.payment.enums.PaymentStatus;
import com.quickbite.payment.messaging.dto.OrderEventDTO;
import com.quickbite.payment.repository.PaymentRepository;
import com.rabbitmq.client.Channel;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class PaymentLifecycleEventListenerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Channel channel;

    @InjectMocks
    private PaymentLifecycleEventListener listener;

    private Message message;

    @BeforeEach
    void setUp() {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(42L);
        message = new Message(new byte[0], properties);
        ReflectionTestUtils.setField(listener, "entityManager", entityManager);
    }

    @Test
    void handleOrderCreated_CreatesPendingPaymentAndAcknowledges() throws IOException {
        OrderEventDTO event = new OrderEventDTO(55L, 11L, 21L, 31L, new BigDecimal("340.00"), LocalDateTime.now());
        when(paymentRepository.findByOrderId(55L)).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.handleOrderCreated(event, message, channel);

        verify(paymentRepository).saveAndFlush(any(Payment.class));
        verify(channel).basicAck(42L, false);
    }

    @Test
    void handleOrderCreated_RejectsMessageWhenPersistenceFails() throws IOException {
        OrderEventDTO event = new OrderEventDTO(55L, 11L, 21L, 31L, new BigDecimal("340.00"), LocalDateTime.now());
        when(paymentRepository.findByOrderId(55L)).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenThrow(new IllegalStateException("db down"));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> listener.handleOrderCreated(event, message, channel));

        assertEquals("db down", exception.getMessage());
        verify(channel).basicNack(42L, false, false);
    }

    @Test
    void handleOrderDelivered_MarksPendingCodPaymentAsPaid() throws IOException {
        Payment payment = Payment.builder()
            .paymentId(1L)
            .orderId(55L)
            .customerId(11L)
            .amount(new BigDecimal("340.00"))
            .status(PaymentStatus.PENDING)
            .mode(PaymentMode.COD)
            .currency("INR")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(paymentRepository.findByOrderId(55L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);

        listener.handleOrderDelivered(new OrderEventDTO(55L, 11L, 21L, 31L, new BigDecimal("340.00"), LocalDateTime.now()), message, channel);

        assertEquals(PaymentStatus.PAID, payment.getStatus());
        verify(paymentRepository).save(payment);
        verify(channel).basicAck(42L, false);
    }

    @Test
    void handleOrderDelivered_IgnoresNonCodPayments() throws IOException {
        Payment payment = Payment.builder()
            .paymentId(1L)
            .orderId(55L)
            .customerId(11L)
            .amount(new BigDecimal("340.00"))
            .status(PaymentStatus.PENDING)
            .mode(PaymentMode.CARD)
            .currency("INR")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(paymentRepository.findByOrderId(55L)).thenReturn(Optional.of(payment));

        listener.handleOrderDelivered(new OrderEventDTO(55L, 11L, 21L, 31L, new BigDecimal("340.00"), LocalDateTime.now()), message, channel);

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(channel).basicAck(42L, false);
    }
}
