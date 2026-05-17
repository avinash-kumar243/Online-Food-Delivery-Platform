package com.quickbite.delivery.dto;

import java.time.LocalDateTime;

public record AdminDeliveryAgentResponse(
    Long agentId,
    Long userId,
    String fullName,
    String email,
    String phone,
    double avgRating,
    String vehicleType,
    String vehicleNumber,
    boolean isAvailable,
    boolean isVerified,
    String verificationStatus,
    int totalDeliveries,
    LocalDateTime submittedAt,
    String rejectionReason,
    Long reviewedByAdminId,
    LocalDateTime reviewedAt
) {
}
