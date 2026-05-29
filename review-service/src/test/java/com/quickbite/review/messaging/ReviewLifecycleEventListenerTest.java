package com.quickbite.review.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.quickbite.review.client.OrderClient;
import com.quickbite.review.client.dto.OrderDto;
import com.quickbite.review.entity.ReviewEligibility;
import com.quickbite.review.messaging.dto.OrderEventDTO;
import com.quickbite.review.repository.ReviewEligibilityRepository;
import com.rabbitmq.client.Channel;

@ExtendWith(MockitoExtension.class)
class ReviewLifecycleEventListenerTest {

    @Mock
    private OrderClient orderClient;

    @Mock
    private ReviewEligibilityRepository reviewEligibilityRepository;

    @Mock
    private Channel channel;

    private ReviewLifecycleEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ReviewLifecycleEventListener(orderClient, reviewEligibilityRepository);
    }

    @Test
    void handleOrderDelivered_CreatesEligibilityFromEventPayload() throws Exception {
        OrderEventDTO event = new OrderEventDTO(10L, 20L, 30L, 40L, BigDecimal.TEN, LocalDateTime.now());
        Message message = messageWithTag(7L);

        when(reviewEligibilityRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(reviewEligibilityRepository.save(any(ReviewEligibility.class))).thenAnswer(invocation -> invocation.getArgument(0));

        listener.handleOrderDelivered(event, message, channel);

        verify(reviewEligibilityRepository).save(any(ReviewEligibility.class));
        verify(channel).basicAck(7L, false);
    }

    @Test
    void handleOrderDelivered_RefreshesExistingEligibilityUsingOrderClientFallback() throws Exception {
        OrderEventDTO event = new OrderEventDTO(11L, null, null, null, BigDecimal.ONE, LocalDateTime.now());
        Message message = messageWithTag(8L);
        ReviewEligibility existing = ReviewEligibility.builder()
            .eligibilityId(1L)
            .orderId(11L)
            .customerId(20L)
            .restaurantId(30L)
            .agentId(40L)
            .eligible(false)
            .enabledAt(LocalDateTime.now().minusDays(1))
            .build();

        when(orderClient.getOrderById(11L)).thenReturn(orderDto(11L, 21L, 31L, 41L));
        when(reviewEligibilityRepository.findByOrderId(11L)).thenReturn(Optional.of(existing));
        when(reviewEligibilityRepository.save(existing)).thenReturn(existing);

        listener.handleOrderDelivered(event, message, channel);

        verify(orderClient).getOrderById(11L);
        verify(reviewEligibilityRepository).save(existing);
        verify(channel).basicAck(8L, false);
    }

    @Test
    void handleOrderDelivered_WhenProcessingFails_NacksAndRethrows() throws Exception {
        OrderEventDTO event = new OrderEventDTO(12L, null, null, null, BigDecimal.ONE, LocalDateTime.now());
        Message message = messageWithTag(9L);

        when(orderClient.getOrderById(12L)).thenThrow(new IllegalStateException("downstream unavailable"));

        assertThatThrownBy(() -> listener.handleOrderDelivered(event, message, channel))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("downstream unavailable");

        verify(channel).basicNack(9L, false, false);
    }

    private Message messageWithTag(long deliveryTag) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(new byte[0], properties);
    }

    private OrderDto orderDto(Long orderId, Long customerId, Long restaurantId, Long agentId) {
        OrderDto order = new OrderDto();
        order.setOrderId(orderId);
        order.setCustomerId(customerId);
        order.setRestaurantId(restaurantId);
        order.setAgentId(agentId);
        return order;
    }
}
