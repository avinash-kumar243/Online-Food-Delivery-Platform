package com.quickbite.notification.dto;

import lombok.Data;

@Data
public class NotificationEvent {
    private Integer recipientId;
    private String type;
    private String channel;
    private String title;
    private String message;
    private String relatedId;
    private String relatedType;
}
