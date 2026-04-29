package com.quickbite.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByRecipientId(int recipientId);

    List<Notification> findByRecipientIdAndIsRead(int recipientId, boolean isRead);

    long countByRecipientIdAndIsRead(int recipientId, boolean isRead);

    List<Notification> findByType(String type);

    void deleteByNotificationId(int notificationId);
}
