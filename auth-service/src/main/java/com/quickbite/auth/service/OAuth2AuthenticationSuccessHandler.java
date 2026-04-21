package com.quickbite.auth.service;

import java.io.IOException;
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
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
		
		Map<String, Object> attributes = oAuth2User.getAttributes();
		
		String email = (String) attributes.get("email");
		String name = (String) attributes.get("name");
		String picture = (String) attributes.get("picture");
		
		// Try to find and update existing user
		String token = null;
		String userType = null;
		
		// Check Customer
		Optional<Customer> customerOpt = customerRepository.findByEmail(email);
		if (customerOpt.isPresent()) {
			Customer customer = customerOpt.get();
			customer.setIsActive(true);
			customer.setProvider("GOOGLE");
			if (picture != null) customer.setProfilePicUrl(picture);
			if (name != null) customer.setFullName(name);
			customerRepository.save(customer);
			token = jwtService.generateToken(email);
			userType = "CUSTOMER";
		}
		
		// Check RestaurantOwner
		if (token == null) {
			Optional<RestaurantOwner> ownerOpt = restaurantOwnerRepository.findByEmail(email);
			if (ownerOpt.isPresent()) {
				RestaurantOwner owner = ownerOpt.get();
				owner.setIsActive(true);
				owner.setProvider("GOOGLE");
				if (picture != null) owner.setProfilePicUrl(picture);
				if (name != null) owner.setFullName(name);
				restaurantOwnerRepository.save(owner);
				token = jwtService.generateToken(email);
				userType = "RESTAURANT_OWNER";
			}
		}
		
		// Check DeliveryPartner
		if (token == null) {
			Optional<DeliveryPartner> partnerOpt = deliveryPartnerRepository.findByEmail(email);
			if (partnerOpt.isPresent()) {
				DeliveryPartner partner = partnerOpt.get();
				partner.setIsActive(true);
				partner.setProvider("GOOGLE");
				if (picture != null) partner.setProfilePicUrl(picture);
				if (name != null) partner.setFullName(name);
				deliveryPartnerRepository.save(partner);
				token = jwtService.generateToken(email);
				userType = "DELIVERY_AGENT";
			}
		}
		
		if (token != null) {
			// Redirect to frontend with token and user type
			// Update the frontend URL as per your frontend deployment URL
			String redirectUrl = "http://localhost:4200/oauth2/success?token=" + token + "&userType=" + userType + "&email=" + email;
			response.sendRedirect(redirectUrl);
		} else {
			// User not found, redirect to registration page or show error
			String redirectUrl = "http://localhost:4200/auth/register?error=user_not_found&email=" + email;
			response.sendRedirect(redirectUrl);
		}
	}
	
}