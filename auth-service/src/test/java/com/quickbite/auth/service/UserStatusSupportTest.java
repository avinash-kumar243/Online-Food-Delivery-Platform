package com.quickbite.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.quickbite.auth.enums.UserStatus;

class UserStatusSupportTest {

    private UserStatusSupport userStatusSupport;

    @BeforeEach
    void setUp() {
        userStatusSupport = new UserStatusSupport();
    }

    @Test
    @DisplayName("Resolve Status - Should return provided status if not null")
    void resolve_ShouldReturnProvidedStatus() {
        // Act & Assert
        assertEquals(UserStatus.ACTIVE, userStatusSupport.resolve(UserStatus.ACTIVE, false));
        assertEquals(UserStatus.SUSPENDED, userStatusSupport.resolve(UserStatus.SUSPENDED, true));
        assertEquals(UserStatus.DELETED, userStatusSupport.resolve(UserStatus.DELETED, true));
    }

    @Test
    @DisplayName("Resolve Status - Should derive status from isActive when null")
    void resolve_ShouldDeriveFromIsActive() {
        // Act & Assert
        assertEquals(UserStatus.ACTIVE, userStatusSupport.resolve(null, true), "Should be ACTIVE if isActive is true");
        assertEquals(UserStatus.SUSPENDED, userStatusSupport.resolve(null, false), "Should be SUSPENDED if isActive is false");
        assertEquals(UserStatus.ACTIVE, userStatusSupport.resolve(null, null), "Should default to ACTIVE if both are null (Boolean.FALSE check)");
    }

    @Test
    @DisplayName("Ensure Active - Should throw exception for SUSPENDED status")
    void ensureActive_ShouldThrowForSuspended() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userStatusSupport.ensureActive(UserStatus.SUSPENDED, "User")
        );
        assertEquals("User is suspended", exception.getMessage());
    }

    @Test
    @DisplayName("Ensure Active - Should throw exception for DELETED status")
    void ensureActive_ShouldThrowForDeleted() {
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                userStatusSupport.ensureActive(UserStatus.DELETED, "Account")
        );
        assertEquals("Account is deleted", exception.getMessage());
    }

    @Test
    @DisplayName("Ensure Active - Should do nothing for ACTIVE status")
    void ensureActive_ShouldPassForActive() {
        // Act & Assert
        assertDoesNotThrow(() -> userStatusSupport.ensureActive(UserStatus.ACTIVE, "User"));
    }
}