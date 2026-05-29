package com.quickbite.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailService emailService;

    private ArgumentCaptor<SimpleMailMessage> mailCaptor;

    @BeforeEach
    void setUp() {
        mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    }

    @Test
    void sendUserCreatedEmail_Success() {
        emailService.sendUserCreatedEmail(1L, "Avinash", "avi@test.com", "CUSTOMER");

        verify(javaMailSender).send(mailCaptor.capture());
        SimpleMailMessage mail = mailCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("avi@test.com", mail.getTo()[0]);
        org.junit.jupiter.api.Assertions.assertEquals("Welcome to QuickBite", mail.getSubject());
    }

    @Test
    void sendUserSuspendedEmail_Success() {
        emailService.sendUserSuspendedEmail(2L, "John", "john@test.com", "ADMIN");

        verify(javaMailSender).send(mailCaptor.capture());
        SimpleMailMessage mail = mailCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("john@test.com", mail.getTo()[0]);
        org.junit.jupiter.api.Assertions.assertEquals("QuickBite account suspension notice", mail.getSubject());
    }

    @Test
    void sendUserReactivatedEmail_Success() {
        emailService.sendUserReactivatedEmail(3L, "Jane", "jane@test.com", "CUSTOMER");

        verify(javaMailSender).send(mailCaptor.capture());
        SimpleMailMessage mail = mailCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("jane@test.com", mail.getTo()[0]);
        org.junit.jupiter.api.Assertions.assertEquals("QuickBite account reactivated", mail.getSubject());
    }

    @Test
    void sendUserDeletedEmail_Success() {
        emailService.sendUserDeletedEmail(4L, "Mark", "mark@test.com", "CUSTOMER");

        verify(javaMailSender).send(mailCaptor.capture());
        SimpleMailMessage mail = mailCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("mark@test.com", mail.getTo()[0]);
        org.junit.jupiter.api.Assertions.assertEquals("QuickBite account deletion notice", mail.getSubject());
    }
}
