package com.quickbite.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewSubmissionRequest(
    @NotNull(message = "orderId is required")
    Long orderId,
    @NotNull(message = "customerId is required")
    Long customerId,
    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    int rating,
    @Size(max = 1000, message = "comment must not exceed 1000 characters")
    String comment
) {
}
