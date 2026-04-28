package com.quickbite.auth.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

class AppRoleAwareOAuth2AuthorizationRequestResolverTest {

    @Test
    void storesNormalizedAppRoleInSession() {
        AppRoleAwareOAuth2AuthorizationRequestResolver resolver =
            new AppRoleAwareOAuth2AuthorizationRequestResolver(
                new InMemoryClientRegistrationRepository(googleRegistration())
            );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        request.setServletPath("/oauth2/authorization/google");
        request.setParameter("appRole", "delivery_partner");

        var authorizationRequest = resolver.resolve(request);

        assertThat(authorizationRequest).isNotNull();
        assertThat(request.getSession(false)).isNotNull();
        assertThat(request.getSession(false).getAttribute(
            AppRoleAwareOAuth2AuthorizationRequestResolver.APP_ROLE_SESSION_ATTRIBUTE
        )).isEqualTo("DELIVERY_PARTNER");
    }

    private ClientRegistration googleRegistration() {
        return ClientRegistration.withRegistrationId("google")
            .clientId("client-id")
            .clientSecret("client-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
            .tokenUri("https://oauth2.googleapis.com/token")
            .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
            .userNameAttributeName("sub")
            .clientName("Google")
            .build();
    }
}
