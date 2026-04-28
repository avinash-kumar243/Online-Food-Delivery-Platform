package com.quickbite.menu.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String role = request.getHeader("X-User-Role");
            String email = request.getHeader("X-User-Email");

            if (role != null && !role.isBlank()) {
                String normalizedRole = role.trim().toUpperCase().replace('-', '_');
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    email != null && !email.isBlank() ? email : "gateway-user",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
