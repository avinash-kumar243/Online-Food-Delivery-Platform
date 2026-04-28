package com.quickbite.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.entity.AdminUser;
import com.quickbite.auth.enums.UserRole;
import com.quickbite.auth.repository.AdminUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserStatusSupport userStatusSupport;

    public ResponseDto login(String email, String password) {
        AdminUser admin = adminUserRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Admin account not found"));

        userStatusSupport.ensureActive(userStatusSupport.resolve(admin.getStatus(), admin.getIsActive()), "Admin account");
        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(admin.getEmail(), UserRole.ADMIN.name(), admin.getAdminId());
        return new ResponseDto("Admin login successful", token, UserRole.ADMIN.name(), admin.getAdminId(), admin.getEmail());
    }
}
