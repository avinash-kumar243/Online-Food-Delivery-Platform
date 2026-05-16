package com.quickbite.auth.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.quickbite.auth.config.AppRoleAwareOAuth2AuthorizationRequestResolver;
import com.quickbite.auth.entity.Customer;
import com.quickbite.auth.entity.DeliveryPartner;
import com.quickbite.auth.entity.RestaurantOwner;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.repository.CustomerRepository;
import com.quickbite.auth.repository.DeliveryPartnerRepository;
import com.quickbite.auth.repository.RestaurantOwnerRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value("${frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private final CustomerRepository customerRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final RestaurantOwnerRepository restaurantOwnerRepository;
    private final JwtService jwtService;
    private final EmailService emailService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String picture = (String) attributes.get("picture");

        HttpSession session = request.getSession(false);
        String appRole = null;
        if (session != null) {
            Object storedAppRole = session.getAttribute(
                AppRoleAwareOAuth2AuthorizationRequestResolver.APP_ROLE_SESSION_ATTRIBUTE
            );
            if (storedAppRole instanceof String storedRole) {
                appRole = storedRole;
            }
            session.removeAttribute(AppRoleAwareOAuth2AuthorizationRequestResolver.APP_ROLE_SESSION_ATTRIBUTE);
        }

        if (!StringUtils.hasText(appRole)) {
            appRole = request.getParameter("appRole");
        }

        if (!StringUtils.hasText(appRole)) {
            appRole = "CUSTOMER";
        } else {
            appRole = appRole.trim().toUpperCase(Locale.ROOT);
        }

        String token;
        String userType;
        Long userId;

        switch (appRole) {
            case "CUSTOMER" -> {
                Optional<Customer> customerOpt = customerRepository.findByEmail(email);

                Customer customer;
                boolean isNewCustomer = customerOpt.isEmpty();
                if (customerOpt.isPresent()) {
                    customer = customerOpt.get();
                } else {
                    customer = new Customer();
                    customer.setEmail(email);
                    customer.setFullName(name != null ? name : "Google User");
                    customer.setPhone("TEMP_" + System.currentTimeMillis());
                    customer.setPasswordHash("GOOGLE_AUTH");
                    customer.setCreatedAt(LocalDateTime.now());
                }

                customer.setIsActive(true);
                customer.setStatus(UserStatus.ACTIVE);
                customer.setProvider("GOOGLE");
                if (picture != null) {
                    customer.setProfilePicUrl(picture);
                }
                if (name != null && !name.isBlank()) {
                    customer.setFullName(name);
                }

                customerRepository.save(customer);
                if (isNewCustomer) {
                    emailService.sendUserCreatedEmail(
                        customer.getCustomerId(),
                        customer.getFullName(),
                        customer.getEmail(),
                        "CUSTOMER"
                    );
                }

                token = jwtService.generateToken(customer.getEmail(), "CUSTOMER", customer.getCustomerId());
                userType = "CUSTOMER";
                userId = customer.getCustomerId();
            }

            case "RESTAURANT_OWNER" -> {
                Optional<RestaurantOwner> ownerOpt = restaurantOwnerRepository.findByEmail(email);

                RestaurantOwner owner;
                boolean isNewOwner = ownerOpt.isEmpty();
                if (ownerOpt.isPresent()) {
                    owner = ownerOpt.get();
                } else {
                    owner = new RestaurantOwner();
                    owner.setEmail(email);
                    owner.setFullName(name != null ? name : "Google User");
                    owner.setPhone("TEMP_" + System.currentTimeMillis());
                    owner.setPasswordHash("GOOGLE_AUTH");
                    owner.setCreatedAt(LocalDateTime.now());
                }

                owner.setIsActive(true);
                owner.setStatus(UserStatus.ACTIVE);
                owner.setProvider("GOOGLE");
                if (picture != null) {
                    owner.setProfilePicUrl(picture);
                }
                if (name != null && !name.isBlank()) {
                    owner.setFullName(name);
                }

                restaurantOwnerRepository.save(owner);
                if (isNewOwner) {
                    emailService.sendUserCreatedEmail(
                        owner.getOwnerId(),
                        owner.getFullName(),
                        owner.getEmail(),
                        "RESTAURANT_OWNER"
                    );
                }

                token = jwtService.generateToken(owner.getEmail(), "RESTAURANT_OWNER", owner.getOwnerId());
                userType = "RESTAURANT_OWNER";
                userId = owner.getOwnerId();
            }

            case "DELIVERY_PARTNER", "DELIVERY_AGENT" -> {
                Optional<DeliveryPartner> partnerOpt = deliveryPartnerRepository.findByEmail(email);

                DeliveryPartner partner;
                boolean isNewPartner = partnerOpt.isEmpty();
                if (partnerOpt.isPresent()) {
                    partner = partnerOpt.get();
                } else {
                    partner = new DeliveryPartner();
                    partner.setEmail(email);
                    partner.setFullName(name != null ? name : "Google User");
                    partner.setPhone("TEMP_" + System.currentTimeMillis());
                    partner.setPasswordHash("GOOGLE_AUTH");
                    partner.setCreatedAt(LocalDateTime.now());
                    partner.setIsOnline(false);
                }

                partner.setIsActive(true);
                partner.setStatus(UserStatus.ACTIVE);
                partner.setProvider("GOOGLE");
                if (picture != null) {
                    partner.setProfilePicUrl(picture);
                }
                if (name != null && !name.isBlank()) {
                    partner.setFullName(name);
                }

                deliveryPartnerRepository.save(partner);
                if (isNewPartner) {
                    emailService.sendUserCreatedEmail(
                        partner.getPartnerId(),
                        partner.getFullName(),
                        partner.getEmail(),
                        "DELIVERY_PARTNER"
                    );
                }

                token = jwtService.generateToken(partner.getEmail(), "DELIVERY_PARTNER", partner.getPartnerId());
                userType = "DELIVERY_PARTNER";
                userId = partner.getPartnerId();
            }

            default -> {
                Optional<Customer> customerOpt = customerRepository.findByEmail(email);

                Customer customer;
                boolean isNewCustomer = customerOpt.isEmpty();
                if (customerOpt.isPresent()) {
                    customer = customerOpt.get();
                } else {
                    customer = new Customer();
                    customer.setEmail(email);
                    customer.setFullName(name != null ? name : "Google User");
                    customer.setPhone("TEMP_" + System.currentTimeMillis());
                    customer.setPasswordHash("GOOGLE_AUTH");
                    customer.setCreatedAt(LocalDateTime.now());
                }

                customer.setIsActive(true);
                customer.setStatus(UserStatus.ACTIVE);
                customer.setProvider("GOOGLE");
                if (picture != null) {
                    customer.setProfilePicUrl(picture);
                }
                if (name != null && !name.isBlank()) {
                    customer.setFullName(name);
                }

                customerRepository.save(customer);
                if (isNewCustomer) {
                    emailService.sendUserCreatedEmail(
                        customer.getCustomerId(),
                        customer.getFullName(),
                        customer.getEmail(),
                        "CUSTOMER"
                    );
                }

                token = jwtService.generateToken(customer.getEmail(), "CUSTOMER", customer.getCustomerId());
                userType = "CUSTOMER";
                userId = customer.getCustomerId();
            }
        }

        String redirectUrl = frontendUrl + "/dashboard"
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&oauth2=success"
                + "&userType=" + URLEncoder.encode(userType, StandardCharsets.UTF_8)
                + "&userId=" + userId;

        response.sendRedirect(redirectUrl);
    }
}
