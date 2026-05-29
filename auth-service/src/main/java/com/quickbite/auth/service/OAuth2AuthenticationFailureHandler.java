package com.quickbite.auth.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.quickbite.auth.config.OAuth2RequestContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Value("${frontend-url:https://main.d38xhvu2bosgry.amplifyapp.com}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String appRole = resolveAppRole(request);
        OAuth2RequestContext.clearAppRole(request);
        String targetPath = resolveFrontendAuthPath(appRole);
        String error = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);

        response.sendRedirect(frontendUrl + targetPath + "?oauth2=failed&reason=" + error);
    }

    private String resolveAppRole(HttpServletRequest request) {
        String appRole = OAuth2RequestContext.resolveAppRole(request);
        if (StringUtils.hasText(appRole)) {
            return OAuth2RequestContext.normalizeRole(appRole);
        }

        return "CUSTOMER";
    }

    private String resolveFrontendAuthPath(String appRole) {
        return switch (appRole) {
            case "RESTAURANT_OWNER" -> "/restaurant/auth";
            case "DELIVERY_AGENT", "DELIVERY_PARTNER" -> "/delivery-partner/auth";
            default -> "/customer/auth";
        };
    }
}
