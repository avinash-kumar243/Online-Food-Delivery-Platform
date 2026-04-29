package com.quickbite.notification.service;

import java.util.List;

import com.quickbite.notification.dto.BulkNotificationRequest;
import com.quickbite.notification.dto.NotificationEvent;
import com.quickbite.notification.dto.NotificationRequest;
import com.quickbite.notification.entity.Notification;

public interface NotificationService {

    Notification send(NotificationRequest request);

    List<Notification> sendBulk(BulkNotificationRequest request);

    void sendEmail(Notification notification);

    void sendSMS(Notification notification);

    Notification markAsRead(int notificationId);

    List<Notification> markAllRead(int recipientId);

    long getUnreadCount(int recipientId);

    List<Notification> getNotificationsByRecipientId(int recipientId);

    List<Notification> getAllNotifications();

    void deleteNotification(int notificationId);

    Notification processEvent(NotificationEvent event);
}
