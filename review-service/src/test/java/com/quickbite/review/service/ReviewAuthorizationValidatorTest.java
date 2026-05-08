package com.quickbite.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickbite.review.client.OrderClient;
import com.quickbite.review.client.dto.OrderDto;
import com.quickbite.review.entity.ReviewEligibility;
import com.quickbite.review.exception.BadRequestException;
import com.quickbite.review.exception.ResourceNotFoundException;
import com.quickbite.review.repository.ReviewEligibilityRepository;

@ExtendWith(MockitoExtension.class)
class ReviewAuthorizationValidatorTest {

    @Mock
    private OrderClient orderClient;

    @Mock
    private ReviewEligibilityRepository reviewEligibilityRepository;

    private ReviewAuthorizationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ReviewAuthorizationValidator(orderClient, reviewEligibilityRepository);
    }

    @Test
    void validateDeliveredOrder_ReturnsCachedEligibilityWhenCustomerMatches() {
        ReviewEligibility cached = eligibility(1L, 20L, 30L, 40L, true, LocalDateTime.now());
        when(reviewEligibilityRepository.findByOrderId(1L)).thenReturn(Optional.of(cached));

        var result = validator.validateDeliveredOrder(1L, 20L);

        assertThat(result).isSameAs(cached);
    }

    @Test
    void validateDeliveredOrder_WhenCachedCustomerDiffers_ThrowsBadRequest() {
        ReviewEligibility cached = eligibility(1L, 20L, 30L, 40L, true, LocalDateTime.now());
        when(reviewEligibilityRepository.findByOrderId(1L)).thenReturn(Optional.of(cached));

        assertThatThrownBy(() -> validator.validateDeliveredOrder(1L, 999L))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("This order is not associated with the provided customer");
    }

    @Test
    void validateDeliveredOrder_WhenOrderMissing_ThrowsResourceNotFound() {
        when(reviewEligibilityRepository.findByOrderId(2L)).thenReturn(Optional.empty());
        when(orderClient.getOrderById(2L)).thenReturn(null);

        assertThatThrownBy(() -> validator.validateDeliveredOrder(2L, 20L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("Order not found for orderId 2");
    }

    @Test
    void validateDeliveredOrder_WhenOrderNotDelivered_ThrowsBadRequest() {
        when(reviewEligibilityRepository.findByOrderId(3L)).thenReturn(Optional.empty());
        when(orderClient.getOrderById(3L)).thenReturn(orderDto(3L, 20L, 30L, 40L, "PLACED"));

        assertThatThrownBy(() -> validator.validateDeliveredOrder(3L, 20L))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Reviews can only be submitted for delivered orders");
    }

    @Test
    void validateDeliveredOrder_WhenCustomerDoesNotMatch_ThrowsBadRequest() {
        when(reviewEligibilityRepository.findByOrderId(4L)).thenReturn(Optional.empty());
        when(orderClient.getOrderById(4L)).thenReturn(orderDto(4L, 20L, 30L, 40L, "DELIVERED"));

        assertThatThrownBy(() -> validator.validateDeliveredOrder(4L, 99L))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("This order is not associated with the provided customer");
    }

    @Test
    void validateDeliveredOrder_WhenRestaurantOrAgentMissing_ThrowsBadRequest() {
        when(reviewEligibilityRepository.findByOrderId(5L)).thenReturn(Optional.empty());
        when(orderClient.getOrderById(5L)).thenReturn(orderDto(5L, 20L, null, 40L, "DELIVERED"));

        assertThatThrownBy(() -> validator.validateDeliveredOrder(5L, 20L))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Delivered order is missing restaurant information");

        when(orderClient.getOrderById(6L)).thenReturn(orderDto(6L, 20L, 30L, null, "DELIVERED"));
        when(reviewEligibilityRepository.findByOrderId(6L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validateDeliveredOrder(6L, 20L))
            .isInstanceOf(BadRequestException.class)
            .hasMessage("Delivered order is missing delivery partner information");
    }

    @Test
    void validateDeliveredOrder_CreatesAndRefreshesEligibility() {
        when(reviewEligibilityRepository.findByOrderId(7L)).thenReturn(Optional.empty());
        when(orderClient.getOrderById(7L)).thenReturn(orderDto(7L, 20L, 30L, 40L, "DELIVERED"));
        when(reviewEligibilityRepository.save(org.mockito.ArgumentMatchers.any(ReviewEligibility.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var created = validator.validateDeliveredOrder(7L, 20L);

        assertThat(created.getOrderId()).isEqualTo(7L);
        assertThat(created.isEligible()).isTrue();

        ReviewEligibility existing = eligibility(8L, 20L, 31L, 41L, false, null);
        when(reviewEligibilityRepository.findByOrderId(8L)).thenReturn(Optional.of(existing));
        when(orderClient.getOrderById(8L)).thenReturn(orderDto(8L, 20L, 32L, 42L, "DELIVERED"));

        var refreshed = validator.validateDeliveredOrder(8L, 20L);

        assertThat(refreshed.getRestaurantId()).isEqualTo(32L);
        assertThat(refreshed.getAgentId()).isEqualTo(42L);
        assertThat(refreshed.getEnabledAt()).isNotNull();
        verify(reviewEligibilityRepository).save(existing);
    }

    private ReviewEligibility eligibility(Long orderId, Long customerId, Long restaurantId, Long agentId,
                                          boolean eligible, LocalDateTime enabledAt) {
        return ReviewEligibility.builder()
            .eligibilityId(1L)
            .orderId(orderId)
            .customerId(customerId)
            .restaurantId(restaurantId)
            .agentId(agentId)
            .eligible(eligible)
            .enabledAt(enabledAt)
            .build();
    }

    private OrderDto orderDto(Long orderId, Long customerId, Long restaurantId, Long agentId, String orderStatus) {
        OrderDto order = new OrderDto();
        order.setOrderId(orderId);
        order.setCustomerId(customerId);
        order.setRestaurantId(restaurantId);
        order.setAgentId(agentId);
        order.setOrderStatus(orderStatus);
        return order;
    }
}
