package com.quickbite.auth.dto;

public record PlatformUserDto(
    Long userId,
    String fullName,
    String email,
    String phone,
    String role,
    String status,
    Boolean isActive
) {
}
