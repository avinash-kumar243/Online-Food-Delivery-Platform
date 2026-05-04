package com.quickbite.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.quickbite.auth.config.AppRoleAwareOAuth2AuthorizationRequestResolver;
import com.quickbite.auth.entity.RestaurantOwner;
import com.quickbite.auth.repository.CustomerRepository;
import com.quickbite.auth.repository.DeliveryPartnerRepository;
import com.quickbite.auth.repository.RestaurantOwnerRepository;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private DeliveryPartnerRepository deliveryPartnerRepository;

    @Mock
    private RestaurantOwnerRepository restaurantOwnerRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationSuccessHandler(
            customerRepository,
            deliveryPartnerRepository,
            restaurantOwnerRepository,
            jwtService,
            emailService
        );
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:4200");
    }

    @Test
    void usesSessionStoredRoleForRestaurantOwnerLogin() throws Exception {
        when(restaurantOwnerRepository.findByEmail("owner@example.com")).thenReturn(Optional.empty());
        when(restaurantOwnerRepository.save(any(RestaurantOwner.class))).thenAnswer(invocation -> {
            RestaurantOwner owner = invocation.getArgument(0);
            owner.setOwnerId(42L);
            return owner;
        });
        when(jwtService.generateToken("owner@example.com", "RESTAURANT_OWNER", 42L)).thenReturn("owner-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(
            AppRoleAwareOAuth2AuthorizationRequestResolver.APP_ROLE_SESSION_ATTRIBUTE,
            "RESTAURANT_OWNER"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        var principal = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of("email", "owner@example.com", "name", "Owner Name"),
            "email"
        );
        var authentication = new OAuth2AuthenticationToken(
            principal,
            principal.getAuthorities(),
            "google"
        );

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<RestaurantOwner> ownerCaptor = ArgumentCaptor.forClass(RestaurantOwner.class);
        verify(restaurantOwnerRepository).save(ownerCaptor.capture());
        verify(emailService).sendUserCreatedEmail(42L, "Owner Name", "owner@example.com", "RESTAURANT_OWNER");

        assertThat(ownerCaptor.getValue().getProvider()).isEqualTo("GOOGLE");
        assertThat(ownerCaptor.getValue().getFullName()).isEqualTo("Owner Name");
        assertThat(ownerCaptor.getValue().getEmail()).isEqualTo("owner@example.com");
        assertThat(response.getRedirectedUrl())
            .contains("oauth2=success")
            .contains("userType=RESTAURANT_OWNER")
            .contains("userId=42")
            .contains("token=owner-token");
        assertThat(request.getSession(false).getAttribute(
            AppRoleAwareOAuth2AuthorizationRequestResolver.APP_ROLE_SESSION_ATTRIBUTE
        )).isNull();
    }
}
