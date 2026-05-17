package com.quickbite.restaurant.dto;

import java.time.LocalDateTime;

public record AdminRestaurantResponse(
    Long restaurantId,
    String restaurantName,
    Long ownerId,
    String ownerName,
    String ownerEmail,
    String ownerPhone,
    String cuisine,
    String address,
    String city,
    String phone,
    Double avgRating,
    Boolean isOpen,
    Boolean isApproved,
    String approvalStatus,
    LocalDateTime submittedAt,
    String rejectionReason,
    Long reviewedByAdminId,
    LocalDateTime reviewedAt
) {
}
