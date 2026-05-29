package com.quickbite.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.auth.dto.InternalUserSummaryDto;
import com.quickbite.auth.enums.UserRole;
import com.quickbite.auth.service.UserAdministrationService;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/internal/users")
public class InternalUserController {

    private final UserAdministrationService userAdministrationService;

    @GetMapping("/summary")
    public ResponseEntity<InternalUserSummaryDto> getUserSummary(
        @RequestParam UserRole role,
        @RequestParam Long userId
    ) {
        return ResponseEntity.ok(userAdministrationService.getUserSummary(role, userId));
    }

    @GetMapping("/role")
    public ResponseEntity<List<InternalUserSummaryDto>> getUsersByRole(@RequestParam UserRole role) {
        return ResponseEntity.ok(userAdministrationService.getUsersByRoleForInternal(role));
    }
}
