package com.quickbite.review.client.dto;

import lombok.Data;

@Data
public class OrderDto {
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private Long agentId;
}
