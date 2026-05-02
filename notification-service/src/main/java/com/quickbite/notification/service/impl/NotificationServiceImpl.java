package com.quickbite.notification.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        for (Integer recipientId : request.getRecipientIds()) {
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
    public void sendSMS(Notification notification) {
        smsGateway.send(notification);
    }

    @Override
    public Notification markAsRead(int notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found for notificationId " + notificationId));
        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Override
    public List<Notification> markAllRead(int recipientId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdAndIsRead(recipientId, false);
        notifications.forEach(notification -> notification.setRead(true));
        return notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(int recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByRecipientId(int recipientId) {
        return notificationRepository.findByRecipientId(recipientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public void deleteNotification(int notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification not found for notificationId " + notificationId);
        }
        notificationRepository.deleteByNotificationId(notificationId);
    }

    @Override
    public Notification processEvent(NotificationEvent event) {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(event.getRecipientId());
        request.setType(event.getType());
        request.setChannel(event.getChannel());
        request.setTitle(event.getTitle());
        request.setMessage(event.getMessage());
        request.setRelatedId(event.getRelatedId());
        request.setRelatedType(event.getRelatedType());
        request.setRead(false);
        return send(request);
    }

    private NotificationRequest cloneForRecipient(NotificationRequest source, Integer recipientId) {
        NotificationRequest request = new NotificationRequest();
        request.setRecipientId(recipientId);
        request.setType(source.getType());
        request.setChannel(source.getChannel());
        request.setTitle(source.getTitle());
        request.setMessage(source.getMessage());
        request.setRelatedId(source.getRelatedId());
        request.setRelatedType(source.getRelatedType());
        request.setRead(source.isRead());
        return request;
    }

    private Notification buildNotification(NotificationRequest request) {
        return Notification.builder()
                .recipientId(request.getRecipientId())
                .type(request.getType())
                .channel(request.getChannel())
                .title(request.getTitle())
                .message(request.getMessage())
                .relatedId(request.getRelatedId())
                .relatedType(request.getRelatedType())
                .isRead(request.isRead())
                .sentAt(LocalDateTime.now())
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
