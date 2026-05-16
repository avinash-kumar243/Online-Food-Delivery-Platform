package com.quickbite.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.HashMap;
import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts; 
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;


@Service
public class JwtService {  // JWT Token related operations
	
	@Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long jwtTokenValidity;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return generateToken(email, null, null);
    }

    public String generateToken(String email, String role, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        if (role != null && !role.isBlank()) {
            claims.put("role", role);
        }
        if (userId != null) {
            claims.put("userId", userId);
        }

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtTokenValidity))
                .signWith(getSigningKey())
                .compact(); 
    }

    public String extractEmailFromToken(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Date extractExpirationDate(String token) {
        return extractAllClaims(token).getExpiration();
    }
 
    public boolean validateToken(String token, UserDetails userDetails) {
        String username = extractEmailFromToken(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token); 
    }

    private boolean isTokenExpired(String token) {
        return extractExpirationDate(token).before(new Date());
    }
    
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    private Claims extractAllClaims(String token) {    	
        return Jwts.parserBuilder()
        		.setSigningKey(getSigningKey())
        		.build()
        		.parseClaimsJws(token)
				.getBody(); 
    }
	
}
