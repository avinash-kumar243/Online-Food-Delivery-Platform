package com.quickbite.notification.service;

import java.util.List;

import com.quickbite.notification.dto.BulkNotificationRequest;
import com.quickbite.notification.dto.EmailNotificationRequest;
import com.quickbite.notification.dto.NotificationEvent;
import com.quickbite.notification.dto.NotificationRequest;
import com.quickbite.notification.dto.PasswordResetOtpEmailRequest;
import com.quickbite.notification.dto.UserLifecycleEmailRequest;
import com.quickbite.notification.entity.Notification;

public interface NotificationService {

    Notification send(NotificationRequest request);

    List<Notification> sendBulk(BulkNotificationRequest request);

    void sendEmail(Notification notification);

    void sendSMS(Notification notification);

    Notification markAsRead(int notificationId, Long recipientId, String recipientRole);

    List<Notification> markAllRead(Long recipientId, String recipientRole);

    long getUnreadCount(Long recipientId, String recipientRole);

    List<Notification> getNotificationsByRecipientId(Long recipientId, String recipientRole);

    void deleteNotification(int notificationId, Long recipientId, String recipientRole);

    Notification processEvent(NotificationEvent event);

    void sendEmail(EmailNotificationRequest request);

    void sendPasswordResetOtpEmail(PasswordResetOtpEmailRequest request);

    void sendUserLifecycleEmail(UserLifecycleEmailRequest request);
}
