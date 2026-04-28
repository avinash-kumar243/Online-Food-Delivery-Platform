package com.quickbite.restaurant.dto;

import java.time.LocalDateTime;

public record RestaurantResponse(
    Long restaurantId,
    Long ownerId,
    String name,
    String description,
    String cuisine,
    String address,
    String city,
    Double latitude,
    Double longitude,
    String phone,
    Double avgRating,
    Double deliveryRadius,
    Boolean isOpen,
    Boolean isApproved,
    String approvalStatus,
    String rejectionReason,
    Long reviewedByAdminId,
    LocalDateTime reviewedAt,
    LocalDateTime submittedAt,
    Integer minOrderAmount,
    Integer estimatedDeliveryMin
) {
}
