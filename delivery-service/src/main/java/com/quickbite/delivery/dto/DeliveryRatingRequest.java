package com.quickbite.delivery.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record DeliveryRatingRequest(
    @DecimalMin(value = "0.0", message = "avgRating must be at least 0.0")
    @DecimalMax(value = "5.0", message = "avgRating must be at most 5.0")
    double avgRating
) {
}
