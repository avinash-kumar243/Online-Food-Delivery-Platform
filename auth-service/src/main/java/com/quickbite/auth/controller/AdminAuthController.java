package com.quickbite.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.auth.dto.AdminLoginRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.service.AdminAuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/admin")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<ResponseDto> login(@Valid @RequestBody AdminLoginRequestDto request) {
        return ResponseEntity.ok(adminAuthService.login(request.email(), request.password()));
    }
}
