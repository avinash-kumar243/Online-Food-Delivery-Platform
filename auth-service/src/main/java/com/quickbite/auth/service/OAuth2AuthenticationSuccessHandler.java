package com.quickbite.auth.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

import com.quickbite.auth.entity.Customer;
import com.quickbite.auth.entity.DeliveryPartner;
import com.quickbite.auth.entity.RestaurantOwner;
import com.quickbite.auth.repository.CustomerRepository;
import com.quickbite.auth.repository.DeliveryPartnerRepository;
import com.quickbite.auth.repository.RestaurantOwnerRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final CustomerRepository customerRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final RestaurantOwnerRepository restaurantOwnerRepository;
    private final JwtService jwtService;

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

        String appRole = request.getParameter("appRole");

        if (appRole == null || appRole.isBlank()) {
            appRole = "CUSTOMER";
        }

        String token;
        String userType;

        switch (appRole) {
            case "CUSTOMER" -> {
                Optional<Customer> customerOpt = customerRepository.findByEmail(email);

                Customer customer;
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
                customer.setProvider("GOOGLE");
                if (picture != null) {
                    customer.setProfilePicUrl(picture);
                }
                if (name != null && !name.isBlank()) {
                    customer.setFullName(name);
                }

                customerRepository.save(customer);

                token = jwtService.generateToken(email);
                userType = "CUSTOMER";
            }

            case "RESTAURANT_OWNER" -> {
                Optional<RestaurantOwner> ownerOpt = restaurantOwnerRepository.findByEmail(email);

                RestaurantOwner owner;
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
                owner.setProvider("GOOGLE");
                if (picture != null) {
                    owner.setProfilePicUrl(picture);
                }
                if (name != null && !name.isBlank()) {
                    owner.setFullName(name);
                }

                restaurantOwnerRepository.save(owner);

                token = jwtService.generateToken(email);
                userType = "RESTAURANT_OWNER";
            }

            case "DELIVERY_AGENT" -> {
                Optional<DeliveryPartner> partnerOpt = deliveryPartnerRepository.findByEmail(email);

                DeliveryPartner partner;
                if (partnerOpt.isPresent()) {
                    partner = partnerOpt.get();
                } else {
                    partner = new DeliveryPartner();
                    partner.setEmail(email);
                    partner.setFullName(name != null ? name : "Google User");
                    partner.setPhone("TEMP_" + System.currentTimeMillis());
                    partner.setPasswordHash("GOOGLE_AUTH");
                    partner.setCreatedAt(LocalDateTime.now());
                }

                partner.setIsActive(true);
                partner.setProvider("GOOGLE");
                if (picture != null) {
                    partner.setProfilePicUrl(picture);
                }
                if (name != null && !name.isBlank()) {
                    partner.setFullName(name);
                }

                deliveryPartnerRepository.save(partner);

                token = jwtService.generateToken(email);
                userType = "DELIVERY_AGENT";
            }

            default -> {
                Optional<Customer> customerOpt = customerRepository.findByEmail(email);

                Customer customer;
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
                customer.setProvider("GOOGLE");
                if (picture != null) {
                    customer.setProfilePicUrl(picture);
                }
                if (name != null && !name.isBlank()) {
                    customer.setFullName(name);
                }

                customerRepository.save(customer);

                token = jwtService.generateToken(email);
                userType = "CUSTOMER";
            }
        }

        String redirectUrl = "http://localhost:4200/dashboard"
                + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                + "&oauth2=success"
                + "&userType=" + URLEncoder.encode(userType, StandardCharsets.UTF_8);

        response.sendRedirect(redirectUrl);
    }
}