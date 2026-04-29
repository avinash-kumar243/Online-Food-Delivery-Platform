package com.quickbite.review.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

import lombok.Data;

@Data
public class OrderDto {
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    @JsonAlias("deliveryAgentId")
    private Long agentId;
}
