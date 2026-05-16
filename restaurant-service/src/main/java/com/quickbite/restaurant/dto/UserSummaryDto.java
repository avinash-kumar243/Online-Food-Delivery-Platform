package com.quickbite.restaurant.dto;

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
