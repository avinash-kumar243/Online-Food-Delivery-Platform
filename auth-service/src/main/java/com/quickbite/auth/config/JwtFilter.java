package com.quickbite.auth.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.auth.exception.AccountAccessException;
import com.quickbite.auth.service.TokenBlacklistService;
import com.quickbite.auth.service.CustomUserDetailsService;
import com.quickbite.auth.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {  // JWTFilter = It ask JWT token (from JWTSevice) for every request, and authenticate user

	private final JwtService jwtService;
	private final CustomUserDetailsService customUserDetailsService;
	private final TokenBlacklistService tokenBlacklistService;
	private final ObjectMapper objectMapper = new ObjectMapper();
	
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		String path = request.getServletPath();

		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}

		if(path.startsWith("/auth/")
		        || path.startsWith("/oauth2/")
		        || path.startsWith("/login/")
		        || path.equals("/error")
		        || path.startsWith("/swagger-ui/")
		        || path.equals("/swagger-ui.html")
		        || path.startsWith("/v3/api-docs")
		        || path.startsWith("/h2-console/")
		        || path.startsWith("/actuator/health")
		        || path.startsWith("/actuator/info")) {
		    filterChain.doFilter(request, response);
		    return;
		}
		
		String authHeader = request.getHeader("Authorization");
        String token = null;
        String email = null;
        
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        token = authHeader.substring(7);
        email = jwtService.extractEmailFromToken(token);

        if (tokenBlacklistService.isBlacklisted(token)) {
            SecurityContextHolder.clearContext();
            writeAuthFailure(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized", "Session is no longer valid", request);
            return;
        }
        
        if(email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);

                if(jwtService.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            } catch (AccountAccessException ex) {
                SecurityContextHolder.clearContext();
                writeAuthFailure(response, ex.getHttpStatus().value(), ex.getHttpStatus().getReasonPhrase(), ex.getMessage(), request);
                return;
            } catch (RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        
        filterChain.doFilter(request, response);
	}

    private void writeAuthFailure(HttpServletResponse response, int status, String error, String message, HttpServletRequest request) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getWriter(), body);
    }
	
}
