package com.quickbite.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetOtpEmailRequest {

    @NotBlank(message = "to is required")
    @Email(message = "to must be a valid email")
    private String to;

    @NotBlank(message = "otp is required")
    private String otp;

    @Min(value = 1, message = "validityInMinutes must be at least 1")
    private int validityInMinutes = 1;
}
