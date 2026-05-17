package com.quickbite.notification.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.notification.dto.BulkNotificationRequest;
import com.quickbite.notification.dto.EmailNotificationRequest;
import com.quickbite.notification.dto.NotificationEvent;
import com.quickbite.notification.dto.NotificationRequest;
import com.quickbite.notification.dto.PasswordResetOtpEmailRequest;
import com.quickbite.notification.dto.UserLifecycleEmailRequest;
import com.quickbite.notification.entity.Notification;
import com.quickbite.notification.exception.NotificationDispatchException;
import com.quickbite.notification.exception.ResourceNotFoundException;
import com.quickbite.notification.repository.NotificationRepository;
import com.quickbite.notification.service.NotificationService;
import com.quickbite.notification.service.SmsGateway;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

@Slf4j
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final JavaMailSender javaMailSender;
    private final SmsGateway smsGateway;

    @Override
    public Notification send(NotificationRequest request) {
        Notification notification = buildNotification(request);
        Notification savedNotification = notificationRepository.save(notification);
        dispatchByChannel(savedNotification);
        return savedNotification;
    }

    @Override
    public List<Notification> sendBulk(BulkNotificationRequest request) {
        List<Notification> notifications = new ArrayList<>();
        for (Long recipientId : request.getRecipientIds()) {
            NotificationRequest perRecipient = cloneForRecipient(request.getNotification(), recipientId);
            notifications.add(send(perRecipient));
        }
        return notifications;
    }

    @Override
    public void sendEmail(Notification notification) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo("recipient-" + notification.getRecipientId() + "@quickbite.local");
            mailMessage.setSubject(notification.getTitle());
            mailMessage.setText(notification.getMessage());
            javaMailSender.send(mailMessage);
            log.info("Mail Send to success");
        } catch (MailException ex) {
            log.warn("Email placeholder dispatch failed for notificationId={}: {}", notification.getNotificationId(), ex.getMessage());
        }
    }

    @Override
    public void sendEmail(EmailNotificationRequest request) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(request.getTo());
            mailMessage.setSubject(request.getSubject());
            mailMessage.setText(request.getMessage());
            javaMailSender.send(mailMessage);
        } catch (MailException ex) {
            throw new NotificationDispatchException("Failed to send email via notification-service", ex);
        }
    }

    @Override
    public void sendPasswordResetOtpEmail(PasswordResetOtpEmailRequest request) {
        sendEmail(buildPasswordResetOtpEmail(request));
    }

    @Override
    public void sendUserLifecycleEmail(UserLifecycleEmailRequest request) {
        sendEmail(buildUserLifecycleEmail(request));
    }

    @Override
    public void sendSMS(Notification notification) {
        smsGateway.send(notification);
    }

    @Override
    public Notification markAsRead(int notificationId, Long recipientId, String recipientRole) {
        Notification notification = notificationRepository
            .findByNotificationIdAndRecipientIdAndRecipientRole(notificationId, recipientId, recipientRole)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found for notificationId " + notificationId));
        notification.setRead(true);
        notification.setReadAt(Instant.now());
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> markAllRead(Long recipientId, String recipientRole) {
        List<Notification> notifications = notificationRepository
            .findByRecipientIdAndRecipientRoleAndIsReadOrderBySentAtDesc(recipientId, recipientRole, false);
        Instant readAt = Instant.now();
        notifications.forEach(notification -> {
            notification.setRead(true);
            notification.setReadAt(readAt);
        });
        return notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId, String recipientRole) {
        return notificationRepository.countByRecipientIdAndRecipientRoleAndIsRead(recipientId, recipientRole, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByRecipientId(Long recipientId, String recipientRole) {
        return notificationRepository.findByRecipientIdAndRecipientRoleOrderBySentAtDesc(recipientId, recipientRole);
    }

    @Override
    public void deleteNotification(int notificationId, Long recipientId, String recipientRole) {
        Notification notification = notificationRepository
            .findByNotificationIdAndRecipientIdAndRecipientRole(notificationId, recipientId, recipientRole)
            .orElseThrow(() -> new ResourceNotFoundException("Notification not found for notificationId " + notificationId));
        notificationRepository.delete(notification);
    }

    @Override
    public Notification processEvent(NotificationEvent event) {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(event.getRecipientId());
        request.setRecipientRole(event.getRecipientRole());
        request.setType(event.getType());
        request.setChannel(event.getChannel());
        request.setTitle(event.getTitle());
        request.setMessage(event.getMessage());
        request.setRelatedId(event.getRelatedId());
        request.setRelatedType(event.getRelatedType());
        request.setOrderId(event.getOrderId());
        request.setDeliveryId(event.getDeliveryId());
        request.setRating(event.getRating());
        request.setActorName(event.getActorName());
        request.setReviewText(event.getReviewText());
        request.setRead(false);
        return send(request);
    }

    private NotificationRequest cloneForRecipient(NotificationRequest source, Long recipientId) {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(recipientId);
        request.setRecipientRole(source.getRecipientRole());
        request.setType(source.getType());
        request.setChannel(source.getChannel());
        request.setTitle(source.getTitle());
        request.setMessage(source.getMessage());
        request.setRelatedId(source.getRelatedId());
        request.setRelatedType(source.getRelatedType());
        request.setOrderId(source.getOrderId());
        request.setDeliveryId(source.getDeliveryId());
        request.setRating(source.getRating());
        request.setActorName(source.getActorName());
        request.setReviewText(source.getReviewText());
        request.setRead(source.isRead());
        return request;
    }

    private Notification buildNotification(NotificationRequest request) {
        return Notification.builder()
                .recipientId(request.getRecipientId())
                .recipientRole(request.getRecipientRole())
                .type(request.getType())
                .channel(request.getChannel())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .orderId(request.getOrderId())
                .deliveryId(request.getDeliveryId())
                .rating(request.getRating())
                .actorName(request.getActorName())
                .reviewText(request.getReviewText())
                .isRead(request.isRead())
                .sentAt(Instant.now())
                .build();
    }

    private EmailNotificationRequest buildPasswordResetOtpEmail(PasswordResetOtpEmailRequest request) {
        EmailNotificationRequest email = new EmailNotificationRequest();
        email.setTo(request.getTo());
        email.setSubject("Password Reset OTP");
        email.setMessage("Your OTP for password reset is: " + request.getOtp()
            + ". It is valid for " + request.getValidityInMinutes() + " minute.");
        return email;
    }

    private EmailNotificationRequest buildUserLifecycleEmail(UserLifecycleEmailRequest request) {
        EmailNotificationRequest email = new EmailNotificationRequest();
        email.setTo(request.getTo());
        email.setSubject(resolveUserLifecycleSubject(request.getEventType()));
        email.setMessage(resolveUserLifecycleMessage(request));
        return email;
    }

    private String resolveUserLifecycleSubject(String eventType) {
        return switch (eventType) {
            case "USER_CREATED" -> "Welcome to QuickBite";
            case "USER_SUSPENDED" -> "QuickBite account suspension notice";
            case "USER_REACTIVATED" -> "QuickBite account reactivated";
            case "USER_DELETED" -> "QuickBite account deletion notice";
            default -> "QuickBite account update";
        };
    }

    private String resolveUserLifecycleMessage(UserLifecycleEmailRequest request) {
        return switch (request.getEventType()) {
            case "USER_CREATED" ->
                "Hello " + request.getFullName() + ", your " + request.getRole()
                    + " account has been created successfully on QuickBite.";
            case "USER_SUSPENDED" ->
                "Hello " + request.getFullName() + ", your " + request.getRole()
                    + " account has been suspended. Please contact support or admin for more details.";
            case "USER_REACTIVATED" ->
                "Hello " + request.getFullName() + ", your " + request.getRole()
                    + " account has been reactivated. You can use QuickBite again.";
            case "USER_DELETED" ->
                "Hello " + request.getFullName() + ", your " + request.getRole()
                    + " account has been deleted or closed from QuickBite.";
            default ->
                "Hello " + request.getFullName() + ", there is an update for your QuickBite account.";
        };
    }

    private void dispatchByChannel(Notification notification) {
        switch (notification.getChannel()) {
            case "EMAIL" -> sendEmail(notification);
            case "SMS" -> sendSMS(notification);
            case "APP" -> log.info("App notification stored for recipientId={}, notificationId={}",
                    notification.getRecipientId(), notification.getNotificationId());
            default -> throw new IllegalArgumentException("Unsupported channel " + notification.getChannel());
        }
    }
}
