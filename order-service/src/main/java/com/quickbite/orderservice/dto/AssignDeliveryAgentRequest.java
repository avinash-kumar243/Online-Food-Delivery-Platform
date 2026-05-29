package com.quickbite.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AssignDeliveryAgentRequest(
    @NotNull @Positive Long deliveryAgentId
) {
}
