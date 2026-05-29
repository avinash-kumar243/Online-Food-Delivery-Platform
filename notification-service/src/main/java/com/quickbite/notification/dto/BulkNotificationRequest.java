package com.quickbite.notification.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BulkNotificationRequest {

    @NotEmpty(message = "recipientIds must not be empty")
    private List<@NotNull @Min(value = 1, message = "recipientId must be greater than 0") Long> recipientIds;

    @Valid
    @NotNull(message = "notification payload is required")
    private NotificationRequest notification;
}
