package com.quickbite.auth.messaging.dto;

public record PasswordResetOtpEmailEvent(
    String to,
    String otp,
    int validityInMinutes
) {
}
