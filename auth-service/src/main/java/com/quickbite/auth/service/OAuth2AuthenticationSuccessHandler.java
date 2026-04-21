package com.quickbite.auth.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Service;

import com.quickbite.auth.entity.AuthProvider;
import com.quickbite.auth.entity.Role;
import com.quickbite.auth.entity.User;
import com.quickbite.auth.repository.UserRepository;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	
	private final UserRepository userRepository;
	private final JwtService jwtService;
	
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		OAuth2User oAuth2User = (OAuth2User)authentication.getPrincipal();
		
		Map<String, Object> attributes = oAuth2User.getAttributes();
		
		String email = (String)attributes.get("email");
		String fullname = (String)attributes.get("fullname"); 
		String phone = (String)attributes.get("phone");
		String passwordHash = (String)attributes.get("passwordHash"); 
		
//		String providerId = (String)attributes.get("sub");
		
		Optional<User> optionalUser = userRepository.findByEmail(email);
		
		User user;
		
		if(optionalUser.isPresent()) {
			user = optionalUser.get();
			user.setIsActive(true);
		} else {
			user = new User();
			
            user.setEmail(email);
            user.setFullName(fullname);
            user.setPhone(phone);
            user.setPasswordHash(passwordHash);
            user.setProvider(AuthProvider.GOOGLE);
            user.setRole(Role.CUSTOMER);
            user.setIsActive(true);
            user.setCreatedAt(LocalDateTime.now()); 
		}
		
		userRepository.save(user); 
		
		String token = jwtService.generateToken(email);
		
//		String redirectUrl = "http://localhost:4200/oauth2/success?token=" + token;
		
//		response.sendRedirect(redirectUrl); 
	}
	
}