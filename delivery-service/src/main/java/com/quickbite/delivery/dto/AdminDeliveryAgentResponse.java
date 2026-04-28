package com.quickbite.delivery.dto;

import java.time.LocalDateTime;

public record AdminDeliveryAgentResponse(
    Long agentId,
    Long userId,
    String fullName,
    String email,
    String phone,
    String vehicleType,
    String vehicleNumber,
    boolean isAvailable,
    boolean isVerified,
    String verificationStatus,
    LocalDateTime submittedAt,
    String rejectionReason,
    Long reviewedByAdminId,
    LocalDateTime reviewedAt
) {
}
