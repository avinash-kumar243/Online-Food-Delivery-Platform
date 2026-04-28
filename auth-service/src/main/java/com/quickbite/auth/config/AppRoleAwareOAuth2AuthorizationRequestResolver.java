package com.quickbite.auth.config;

import java.util.Locale;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class AppRoleAwareOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String APP_ROLE_SESSION_ATTRIBUTE = "quickbite.oauth2.appRole";

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public AppRoleAwareOAuth2AuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository
    ) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
            clientRegistrationRepository,
            "/oauth2/authorization"
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customize(request, defaultResolver.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return customize(request, defaultResolver.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest customize(
            HttpServletRequest request,
            OAuth2AuthorizationRequest authorizationRequest
    ) {
        if (authorizationRequest == null) {
            return null;
        }

        String appRole = request.getParameter("appRole");
        if (StringUtils.hasText(appRole)) {
            request.getSession(true).setAttribute(
                APP_ROLE_SESSION_ATTRIBUTE,
                appRole.trim().toUpperCase(Locale.ROOT)
            );
        } else {
            request.getSession(true).removeAttribute(APP_ROLE_SESSION_ATTRIBUTE);
        }

        return authorizationRequest;
    }
}
