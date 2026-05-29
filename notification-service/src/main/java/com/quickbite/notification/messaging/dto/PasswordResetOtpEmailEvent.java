package com.quickbite.notification.messaging.dto;

public record PasswordResetOtpEmailEvent(
    String to,
    String otp,
    int validityInMinutes
) {
}
