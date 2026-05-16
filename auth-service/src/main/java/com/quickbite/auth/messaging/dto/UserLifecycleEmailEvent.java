package com.quickbite.auth.messaging.dto;

public record UserLifecycleEmailEvent(
    Long userId,
    String fullName,
    String email,
    String role,
    String eventType
) {
}
