package com.quickbite.notification.dto;

import lombok.Data;

@Data
public class NotificationEvent {
    private Long recipientId;
    private String recipientRole;
    private String type;
    private String channel;
    private String title;
    private String message;
    private String relatedId;
    private String relatedType;
    private String orderId;
    private String deliveryId;
    private Integer rating;
    private String actorName;
    private String reviewText;
}
