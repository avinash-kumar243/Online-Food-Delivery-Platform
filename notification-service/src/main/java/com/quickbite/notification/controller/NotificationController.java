package com.quickbite.notification.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.notification.dto.BulkNotificationRequest;
import com.quickbite.notification.dto.EmailNotificationRequest;
import com.quickbite.notification.dto.PasswordResetOtpEmailRequest;
import com.quickbite.notification.entity.Notification;
import com.quickbite.notification.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<Notification> getNotificationsForCurrentUser(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-User-Role") String userRole
    ) {
        return notificationService.getNotificationsByRecipientId(userId, normalizeRole(userRole));
    }

    @GetMapping("/unread-count")
    public long getUnreadCount(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-User-Role") String userRole
    ) {
        return notificationService.getUnreadCount(userId, normalizeRole(userRole));
    }

    @PatchMapping("/{notificationId}/read")
    public Notification markAsRead(
        @PathVariable int notificationId,
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-User-Role") String userRole
    ) {
        return notificationService.markAsRead(notificationId, userId, normalizeRole(userRole));
    }

    @PatchMapping("/read-all")
    public List<Notification> markAllRead(
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-User-Role") String userRole
    ) {
        return notificationService.markAllRead(userId, normalizeRole(userRole));
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Notification> sendBulk(@Valid @RequestBody BulkNotificationRequest request) {
        return notificationService.sendBulk(request);
    }

    @PostMapping("/email")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendEmail(@Valid @RequestBody EmailNotificationRequest request) {
        notificationService.sendEmail(request);
    }

    @PostMapping("/email/password-reset-otp")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendPasswordResetOtpEmail(@Valid @RequestBody PasswordResetOtpEmailRequest request) {
        notificationService.sendPasswordResetOtpEmail(request);
    }

    @DeleteMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotification(
        @PathVariable int notificationId,
        @RequestHeader("X-User-Id") Long userId,
        @RequestHeader("X-User-Role") String userRole
    ) {
        notificationService.deleteNotification(notificationId, userId, normalizeRole(userRole));
    }

    private String normalizeRole(String userRole) {
        return userRole == null ? "" : userRole.trim().toUpperCase();
    }
}
