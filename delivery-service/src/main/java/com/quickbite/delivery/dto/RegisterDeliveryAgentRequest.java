package com.quickbite.delivery.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RegisterDeliveryAgentRequest(
	@NotNull(message = "User id is required")
	@Positive(message = "User id must be positive")
	Long userId,

	@NotBlank(message = "Full name is required")
	@Size(max = 120, message = "Full name must not exceed 120 characters")
	String fullName,

	@NotBlank(message = "Phone number is required")
	@Pattern(
		regexp = "^[0-9+()\\-\\s]{7,20}$",
		message = "Phone number format is invalid"
	)
	String phone,

	@NotBlank(message = "Vehicle type is required")
	@Size(max = 40, message = "Vehicle type must not exceed 40 characters")
	String vehicleType,

	@NotBlank(message = "Vehicle number is required")
	@Size(max = 40, message = "Vehicle number must not exceed 40 characters")
	String vehicleNumber,

	@DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
	@DecimalMax(value = "90.0", message = "Latitude must be <= 90")
	Double currentLatitude,

	@DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
	@DecimalMax(value = "180.0", message = "Longitude must be <= 180")
	Double currentLongitude
) {
}
