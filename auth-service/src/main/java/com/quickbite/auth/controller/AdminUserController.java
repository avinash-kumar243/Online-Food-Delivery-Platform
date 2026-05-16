package com.quickbite.auth.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.auth.dto.PlatformUserDto;
import com.quickbite.auth.enums.UserRole;
import com.quickbite.auth.service.UserAdministrationService;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserAdministrationService userAdministrationService;

    @GetMapping
    public ResponseEntity<List<PlatformUserDto>> getAllUsers() {
        return ResponseEntity.ok(userAdministrationService.getAllUsers());
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<PlatformUserDto>> getUsersByRole(@PathVariable UserRole role) {
        return ResponseEntity.ok(userAdministrationService.getUsersByRole(role));
    }

    @PutMapping("/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable Long userId, @RequestParam UserRole role) {
        userAdministrationService.suspendUser(role, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}/reactivate")
    public ResponseEntity<Void> reactivateUser(@PathVariable Long userId, @RequestParam UserRole role) {
        userAdministrationService.reactivateUser(role, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId, @RequestParam UserRole role) {
        userAdministrationService.deleteUser(role, userId);
        return ResponseEntity.noContent().build();
    }
}
