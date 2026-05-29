package com.quickbite.auth.config;

import java.time.Duration;
import java.util.Locale;

import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class OAuth2RequestContext {

    public static final String APP_ROLE_SESSION_ATTRIBUTE = "quickbite.oauth2.appRole";
    public static final String APP_ROLE_COOKIE = "quickbite_oauth2_app_role";

    private OAuth2RequestContext() {
    }

    public static void storeAppRole(HttpServletRequest request, String appRole) {
        if (!StringUtils.hasText(appRole)) {
            clearAppRole(request);
            return;
        }

        String normalizedRole = normalizeRole(appRole);
        request.getSession(true).setAttribute(APP_ROLE_SESSION_ATTRIBUTE, normalizedRole);

        HttpServletResponse response = currentResponse();
        if (response != null) {
            response.addHeader("Set-Cookie", buildCookie(request, normalizedRole, Duration.ofMinutes(10)).toString());
        }
    }

    public static String resolveAppRole(HttpServletRequest request) {
        Object sessionValue = request.getSession(false) == null
            ? null
            : request.getSession(false).getAttribute(APP_ROLE_SESSION_ATTRIBUTE);
        if (sessionValue instanceof String storedRole && StringUtils.hasText(storedRole)) {
            return normalizeRole(storedRole);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (APP_ROLE_COOKIE.equals(cookie.getName()) && StringUtils.hasText(cookie.getValue())) {
                return normalizeRole(cookie.getValue());
            }
        }

        return null;
    }

    public static void clearAppRole(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession(false).removeAttribute(APP_ROLE_SESSION_ATTRIBUTE);
        }

        HttpServletResponse response = currentResponse();
        if (response != null) {
            response.addHeader("Set-Cookie", buildCookie(request, "", Duration.ZERO).toString());
        }
    }

    public static String normalizeRole(String appRole) {
        if (!StringUtils.hasText(appRole)) {
            return null;
        }

        return appRole.trim().toUpperCase(Locale.ROOT);
    }

    private static ResponseCookie buildCookie(HttpServletRequest request, String value, Duration maxAge) {
        return ResponseCookie.from(APP_ROLE_COOKIE, value)
            .httpOnly(true)
            .secure(request.isSecure())
            .path("/")
            .sameSite("Lax")
            .maxAge(maxAge)
            .build();
    }

    private static HttpServletResponse currentResponse() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes == null ? null : attributes.getResponse();
    }
}
