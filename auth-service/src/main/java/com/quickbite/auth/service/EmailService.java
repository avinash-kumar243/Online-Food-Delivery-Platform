package com.quickbite.auth.service;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    public EmailService(@Nullable JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    public void sendOtpEmail(String to, String otp) {
        sendEmail(to, "QuickBite password reset OTP",
            "Your OTP for password reset is: " + otp + ". It is valid for 1 minute.");
    }

    public void sendUserCreatedEmail(Long userId, String fullName, String email, String role) {
        sendUserLifecycleEmail(
            userId,
            fullName,
            email,
            role,
            "USER_CREATED"
        );
    }

    public void sendUserSuspendedEmail(Long userId, String fullName, String email, String role) {
        sendUserLifecycleEmail(
            userId,
            fullName,
            email,
            role,
            "USER_SUSPENDED"
        );
    }

    public void sendUserReactivatedEmail(Long userId, String fullName, String email, String role) {
        sendUserLifecycleEmail(
            userId,
            fullName,
            email,
            role,
            "USER_REACTIVATED"
        );
    }

    public void sendUserDeletedEmail(Long userId, String fullName, String email, String role) {
        sendUserLifecycleEmail(
            userId,
            fullName,
            email,
            role,
            "USER_DELETED"
        );
    }

    private void sendUserLifecycleEmail(
        Long userId,
        String fullName,
        String email,
        String role,
        String eventType
    ) {
        sendEmail(email, resolveSubject(eventType), resolveMessage(fullName, role, eventType));
    }

    private void sendEmail(String to, String subject, String body) {
        if (javaMailSender == null) {
            log.warn("JavaMailSender is not configured. Email delivery skipped for recipient={}", to);
            return;
        }
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(to);
            mailMessage.setSubject(subject);
            mailMessage.setText(body);
            javaMailSender.send(mailMessage);
        } catch (MailException exception) {
            log.warn("Email dispatch failed for recipient={}: {}", to, exception.getMessage());
        }
    }

    private String resolveSubject(String eventType) {
        return switch (eventType) {
            case "USER_CREATED" -> "Welcome to QuickBite";
            case "USER_SUSPENDED" -> "QuickBite account suspension notice";
            case "USER_REACTIVATED" -> "QuickBite account reactivated";
            case "USER_DELETED" -> "QuickBite account deletion notice";
            default -> "QuickBite account update";
        };
    }

    private String resolveMessage(String fullName, String role, String eventType) {
        return switch (eventType) {
            case "USER_CREATED" ->
                "Hello " + fullName + ", your " + role + " account has been created successfully on QuickBite.";
            case "USER_SUSPENDED" ->
                "Hello " + fullName + ", your " + role + " account has been suspended. Please contact support.";
            case "USER_REACTIVATED" ->
                "Hello " + fullName + ", your " + role + " account has been reactivated.";
            case "USER_DELETED" ->
                "Hello " + fullName + ", your " + role + " account has been deleted from QuickBite.";
            default ->
                "Hello " + fullName + ", there is an update for your QuickBite account.";
        };
    }

}
