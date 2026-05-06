package com.quickbite.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private final String secret = "myVerySecretKeyThatIsAtLeast32CharactersLongForHS256";
    private final long validity = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        // Injecting @Value fields manually
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "jwtTokenValidity", validity);
    }

    @Test
    @DisplayName("Generate Token - Basic (Email Only)")
    void generateToken_Basic() {
        String token = jwtService.generateToken("user@test.com");

        assertNotNull(token);
        assertEquals("user@test.com", jwtService.extractEmailFromToken(token));
    }

    @Test
    @DisplayName("Generate Token - Full Claims")
    void generateToken_WithClaims() {
        String token = jwtService.generateToken("admin@test.com", "ADMIN", 123L);

        assertNotNull(token);
        assertEquals("admin@test.com", jwtService.extractEmailFromToken(token));
        // Note: If you add extra claims extraction methods to your service,
        // you would assert them here as well.
    }

    @Test
    @DisplayName("Extract Expiration - Success")
    void extractExpiration_Success() {
        String token = jwtService.generateToken("test@test.com");
        Date expiration = jwtService.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    @DisplayName("Validate Token - Success")
    void validateToken_Success() {
        String email = "valid@test.com";
        String token = jwtService.generateToken(email);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn(email);

        assertTrue(jwtService.validateToken(token, userDetails));
    }

    @Test
    @DisplayName("Validate Token - Failure (Wrong User)")
    void validateToken_WrongUser() {
        String token = jwtService.generateToken("user1@test.com");

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("user2@test.com");

        assertFalse(jwtService.validateToken(token, userDetails));
    }

    @Test
    @DisplayName("Is Token Expired - Should throw error or return true if expired")
    void isTokenExpired_Check() {
        ReflectionTestUtils.setField(jwtService, "jwtTokenValidity", -60000L);
        String expiredToken = jwtService.generateToken("expired@test.com");

        UserDetails userDetails = mock(UserDetails.class);
        // REMOVE THIS LINE:
        // when(userDetails.getUsername()).thenReturn("expired@test.com");

        assertThrows(Exception.class, () -> {
            jwtService.validateToken(expiredToken, userDetails);
        });
    }
}