package com.quickbite.restaurant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RestaurantRequest(
    @NotNull Long ownerId,

    @NotBlank
    @Size(max = 120)
    String name,

    @Size(max = 500)
    String description,

    @NotBlank
    @Size(max = 80)
    String cuisine,

    @NotBlank
    @Size(max = 255)
    String address,

    @NotBlank
    @Size(max = 80)
    String city,

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    Double latitude,

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    Double longitude,

    @NotBlank
    @Pattern(regexp = "^[0-9+()\\- ]{7,20}$")
    String phone,

    @NotNull
    @Positive
    Double deliveryRadius,

    @NotNull
    @PositiveOrZero
    Integer minOrderAmount,

    @NotNull
    @Positive
    Integer estimatedDeliveryMin
) {
}
