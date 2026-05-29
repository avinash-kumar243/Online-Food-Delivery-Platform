package com.quickbite.delivery.dto;

public record UserSummaryDto(
    Long userId,
    String fullName,
    String email,
    String phone,
    String role,
    String status,
    Boolean isActive
) {
}
