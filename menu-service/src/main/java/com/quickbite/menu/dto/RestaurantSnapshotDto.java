package com.quickbite.menu.dto;

public record RestaurantSnapshotDto(
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
    java.time.LocalDateTime reviewedAt,
    java.time.LocalDateTime submittedAt,
    Integer minOrderAmount,
    Integer estimatedDeliveryMin
) {
}
