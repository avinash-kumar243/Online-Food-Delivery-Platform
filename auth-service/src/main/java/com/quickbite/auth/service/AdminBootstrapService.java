package com.quickbite.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.quickbite.auth.entity.AdminUser;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.repository.AdminUserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminBootstrapService implements CommandLineRunner {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${quickbite.admin.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${quickbite.admin.bootstrap.full-name:QuickBite Admin}")
    private String adminFullName;

    @Value("${quickbite.admin.bootstrap.email:admin@quickbite.local}")
    private String adminEmail;

    @Value("${quickbite.admin.bootstrap.password:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled || adminUserRepository.existsByEmail(adminEmail)) {
            return;
        }

        if (!StringUtils.hasText(adminPassword)) {
            return;
        }

        AdminUser admin = new AdminUser();
        admin.setFullName(adminFullName);
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setIsActive(Boolean.TRUE);
        adminUserRepository.save(admin);
        emailService.sendUserCreatedEmail(
            admin.getAdminId(),
            admin.getFullName(),
            admin.getEmail(),
            "ADMIN"
        );
    }
}
