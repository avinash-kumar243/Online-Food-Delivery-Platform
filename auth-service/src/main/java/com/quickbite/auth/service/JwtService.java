package com.quickbite.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.quickbite.auth.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts; 
import io.jsonwebtoken.security.Keys; 


@Component
public class JwtService {  // JWT Token related operations
	
	@Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long jwtTokenValidity;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String email) {
        return Jwts.builder()
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