package com.quickbite.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailNotificationRequest {

    @NotBlank(message = "to is required")
    @Email(message = "to must be a valid email")
    private String to;

    @NotBlank(message = "subject is required")
    @Size(max = 150, message = "subject must not exceed 150 characters")
    private String subject;

    @NotBlank(message = "message is required")
    @Size(max = 4000, message = "message must not exceed 4000 characters")
    private String message;
}

