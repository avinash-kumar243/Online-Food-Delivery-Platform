package com.quickbite.orderservice.security;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveBearerToken(accessor);
            Claims claims = jwtService.parseClaims(token);

            String userId = resolveUserId(claims);
            String role = resolveRole(claims);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            accessor.setUser(authentication);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            String destination = accessor.getDestination();
            if (user == null || destination == null) {
                throw new IllegalArgumentException("Unauthorized WebSocket subscription");
            }

            if (destination.startsWith("/topic/admin/") && !hasRole(user, "ADMIN")) {
                throw new IllegalArgumentException("Admin subscription required");
            }
        }

        return message;
    }

    private String resolveBearerToken(StompHeaderAccessor accessor) {
        List<String> values = accessor.getNativeHeader("Authorization");
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Missing Authorization header");
        }
        String value = values.get(0);
        if (value == null || !value.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        return value.substring(7);
    }

    private String resolveUserId(Claims claims) {
        Object userId = claims.get("userId");
        if (userId == null) {
            userId = claims.get("id");
        }
        if (userId == null) {
            userId = claims.get("customerId");
        }
        if (userId == null) {
            userId = claims.get("ownerId");
        }
        if (userId == null) {
            userId = claims.get("partnerId");
        }
        if (userId == null) {
            throw new IllegalArgumentException("Missing user id in token");
        }
        return userId.toString();
    }

    private String resolveRole(Claims claims) {
        Object role = claims.get("role");
        if (role == null) {
            role = claims.get("userRole");
        }
        if (role == null) {
            role = claims.get("userType");
        }
        if (role == null) {
            throw new IllegalArgumentException("Missing role in token");
        }
        return role.toString().trim().toUpperCase().replace('-', '_');
    }

    private boolean hasRole(Principal principal, String role) {
        if (!(principal instanceof UsernamePasswordAuthenticationToken authentication)) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
