package com.quickbite.microservice.filter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.quickbite.microservice.util.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthenticationFilter() {
        super(Config.class);
    }

    public static class Config {}

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            String path = exchange.getRequest().getURI().getPath();
            HttpMethod method = exchange.getRequest().getMethod();

            // 1. Allow preflight CORS requests
            if (HttpMethod.OPTIONS.equals(method)) {
                return chain.filter(exchange);
            }

            // 2. Allow public endpoints without JWT
            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            // 3. Check Authorization Header for protected endpoints only
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED, path);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // 4. Validate Bearer Format
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization Header Format", HttpStatus.UNAUTHORIZED, path);
            }

            String token = authHeader.substring(7);

            try {
                // 5. Extract & Validate Token
                String email = jwtService.extractEmail(token);
                String role = jwtService.extractRole(token);
                String userId = jwtService.extractUserId(token);

                if (jwtService.isTokenExpired(token)) {
                    return onError(exchange, "Token Expired", HttpStatus.UNAUTHORIZED, path);
                }

                if (path.startsWith("/api/v1/admin/") && (role == null || !"ADMIN".equalsIgnoreCase(role))) {
                    return onError(exchange, "Admin access required", HttpStatus.FORBIDDEN, path);
                }

                // 6. Pass user info downstream
                return chain.filter(
                        exchange.mutate()
                                .request(exchange.getRequest().mutate()
                                        .header("X-User-Email", email)
                                        .header("X-User-Role", role == null ? "" : role)
                                        .header("X-User-Id", userId == null ? "" : userId)
                                        .build())
                                .build()
                );

            } catch (Exception e) {
                return onError(exchange, "Invalid Token", HttpStatus.UNAUTHORIZED, path);
            }
        };
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/")
                || path.startsWith("/actuator/")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/")
                || path.startsWith("/h2-console/")
                || path.startsWith("/api/v1/restaurants/approved")
                || path.startsWith("/api/v1/restaurants/search")
                || path.startsWith("/api/v1/restaurants/nearby")
                || path.matches("^/api/v1/restaurants/\\d+$")
                || path.startsWith("/api/v1/menu/restaurant/")
                || path.startsWith("/api/v1/menu/category/")
                || path.startsWith("/api/v1/menu/item/")
                || path.startsWith("/api/v1/menu/search")
                || path.startsWith("/api/v1/menu/vegItems");
    }

    private Mono<Void> onError(org.springframework.web.server.ServerWebExchange exchange,
                               String message,
                               HttpStatus status,
                               String path) {

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("timestamp", LocalDateTime.now().toString());
            errorResponse.put("status", status.value());
            errorResponse.put("error", status.getReasonPhrase());
            errorResponse.put("message", message);
            errorResponse.put("path", path);

            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);

            return exchange.getResponse()
                    .writeWith(Mono.just(exchange.getResponse()
                            .bufferFactory()
                            .wrap(bytes)));

        } catch (Exception e) {
            return exchange.getResponse().setComplete();
        }
    }
}
