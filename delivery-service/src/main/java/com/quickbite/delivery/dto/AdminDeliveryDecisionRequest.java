package com.quickbite.delivery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminDeliveryDecisionRequest(
    @NotNull Long adminId,
    @Size(max = 500) String feedback
) {
}
