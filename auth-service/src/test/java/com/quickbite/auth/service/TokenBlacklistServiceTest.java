package com.quickbite.auth.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;
    private final String testToken = "eyJhbGciOiJIUzI1NiJ9.test.token";

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
    }

    @Test
    @DisplayName("Blacklist Token - Should add token to the set")
    void blacklistToken_ShouldAddToken() {
        // Act
        tokenBlacklistService.blacklistToken(testToken);

        // Assert
        assertTrue(tokenBlacklistService.isBlacklisted(testToken), "Token should be blacklisted after being added.");
    }

    @Test
    @DisplayName("Is Blacklisted - Should return false for unknown tokens")
    void isBlacklisted_ShouldReturnFalseForNewToken() {
        // Assert
        assertFalse(tokenBlacklistService.isBlacklisted("non.existent.token"), "Unknown tokens should not be blacklisted.");
    }

    @Test
    @DisplayName("Blacklist Token - Should handle multiple tokens independently")
    void blacklistToken_ShouldHandleMultipleTokens() {
        // Arrange
        String secondToken = "another.test.token";

        // Act
        tokenBlacklistService.blacklistToken(testToken);
        tokenBlacklistService.blacklistToken(secondToken);

        // Assert
        assertTrue(tokenBlacklistService.isBlacklisted(testToken));
        assertTrue(tokenBlacklistService.isBlacklisted(secondToken));
        assertFalse(tokenBlacklistService.isBlacklisted("third.token"));
    }
}