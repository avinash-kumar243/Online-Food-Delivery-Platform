package com.quickbite.notification.client.dto;

public record InternalUserSummaryDto(
    Long userId,
    String fullName,
    String email,
    String phone,
    String role,
    String status,
    Boolean isActive
) {
}
