package com.quickbite.notification.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NotificationRequest {

    @Min(value = 1, message = "recipientId must be greater than 0")
    private Long recipientId;

    @Pattern(
        regexp = "CUSTOMER|RESTAURANT_OWNER|DELIVERY_PARTNER|ADMIN",
        message = "recipientRole must be CUSTOMER, RESTAURANT_OWNER, DELIVERY_PARTNER, or ADMIN"
    )
    private String recipientRole;

    @NotBlank(message = "type is required")
    @Pattern(regexp = "[A-Z_]+", message = "type must contain uppercase letters and underscores only")
    private String type;

    @NotBlank(message = "channel is required")
    @Pattern(regexp = "APP|EMAIL|SMS", message = "channel must be APP, EMAIL, or SMS")
    private String channel;

    @NotBlank(message = "title is required")
    @Size(max = 150, message = "title must not exceed 150 characters")
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 2000, message = "message must not exceed 2000 characters")
    private String message;

    @Size(max = 100, message = "relatedId must not exceed 100 characters")
    private String relatedId;

    @Size(max = 100, message = "relatedType must not exceed 100 characters")
    private String relatedType;

    @Size(max = 40, message = "orderId must not exceed 40 characters")
    private String orderId;

    @Size(max = 40, message = "deliveryId must not exceed 40 characters")
    private String deliveryId;

    private Integer rating;

    @Size(max = 160, message = "actorName must not exceed 160 characters")
    private String actorName;

    @Size(max = 2000, message = "reviewText must not exceed 2000 characters")
    private String reviewText;

    private boolean isRead;
}
