package com.quickbite.notification.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.notification.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification> findByRecipientIdAndRecipientRoleOrderBySentAtDesc(Long recipientId, String recipientRole);

    List<Notification> findByRecipientIdAndRecipientRoleAndIsReadOrderBySentAtDesc(
        Long recipientId,
        String recipientRole,
        boolean isRead
    );

    long countByRecipientIdAndRecipientRoleAndIsRead(Long recipientId, String recipientRole, boolean isRead);

    java.util.Optional<Notification> findByNotificationIdAndRecipientIdAndRecipientRole(
        Integer notificationId,
        Long recipientId,
        String recipientRole
    );

    void deleteByNotificationId(int notificationId);
}
