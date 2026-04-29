package com.quickbite.notification.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.notification.dto.BulkNotificationRequest;
import com.quickbite.notification.entity.Notification;
import com.quickbite.notification.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/recipient/{recipientId}")
    public List<Notification> getNotificationsByRecipient(@PathVariable int recipientId) {
        return notificationService.getNotificationsByRecipientId(recipientId);
    }

    @GetMapping("/unread-count/{recipientId}")
    public long getUnreadCount(@PathVariable int recipientId) {
        return notificationService.getUnreadCount(recipientId);
    }

    @PatchMapping("/read/{notificationId}")
    public Notification markAsRead(@PathVariable int notificationId) {
        return notificationService.markAsRead(notificationId);
    }

    @PatchMapping("/read-all/{recipientId}")
    public List<Notification> markAllRead(@PathVariable int recipientId) {
        return notificationService.markAllRead(recipientId);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Notification> sendBulk(@Valid @RequestBody BulkNotificationRequest request) {
        return notificationService.sendBulk(request);
    }

    @DeleteMapping("/{notificationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNotification(@PathVariable int notificationId) {
        notificationService.deleteNotification(notificationId);
    }

    @GetMapping("/all")
    public List<Notification> getAllNotifications() {
        return notificationService.getAllNotifications();
    }
}
