package com.quickbite.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.auth.dto.DeliveryPartnerRegisterRequestDto;
import com.quickbite.auth.dto.DeliveryPartnerProfileDto;
import com.quickbite.auth.dto.DeliveryPartnerUpdateProfileDto;
import com.quickbite.auth.dto.LoginRequestDTO;
import com.quickbite.auth.dto.PasswordChangeRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.service.DeliveryPartnerAuthServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/delivery-partner")
public class DeliveryPartnerAuthController {
	
	private final DeliveryPartnerAuthServiceImpl deliveryPartnerAuthService;

	@PostMapping("/register")
    public ResponseEntity<ResponseDto> register(@RequestBody DeliveryPartnerRegisterRequestDto user) {
		ResponseDto registeredUser = deliveryPartnerAuthService.register(user); 
        return ResponseEntity.ok(registeredUser); 
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDto> login(@RequestBody LoginRequestDTO user) {
    	ResponseDto loginUser = deliveryPartnerAuthService.login(user.getEmail(), user.getPassword());
        return ResponseEntity.ok(loginUser); 
    }
    
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid or missing Authorization header");
        }

        String token = authHeader.substring(7); 
        deliveryPartnerAuthService.logout(token); 

        return ResponseEntity.ok("Delivery partner logged out successfully"); 
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        ResponseDto user = deliveryPartnerAuthService.refreshToken(token);
        return ResponseEntity.ok(user); 
    }
    
    @GetMapping("/profile/{partnerId}")
    public ResponseEntity<DeliveryPartnerProfileDto> getProfile(@PathVariable Long partnerId) {
    	DeliveryPartnerProfileDto profile = deliveryPartnerAuthService.getProfile(partnerId);
    	return ResponseEntity.ok(profile);
    }
    
    @PutMapping("/profile/{partnerId}")
    public ResponseEntity<DeliveryPartnerProfileDto> updateProfile(@PathVariable Long partnerId,
    		@RequestBody DeliveryPartnerUpdateProfileDto updateDto) {
    	DeliveryPartnerProfileDto profile = deliveryPartnerAuthService.updateProfile(partnerId, updateDto);
    	return ResponseEntity.ok(profile);
    }
    
    @PostMapping("/change-password/{partnerId}")
    public ResponseEntity<ResponseDto> changePassword(@PathVariable Long partnerId,
    		@RequestBody PasswordChangeRequestDto passwordDto) {
    	ResponseDto response = deliveryPartnerAuthService.changePassword(partnerId, passwordDto);
    	return ResponseEntity.ok(response);
    }
    
    @PostMapping("/deactivate/{partnerId}")
    public ResponseEntity<ResponseDto> deactivateAccount(@PathVariable Long partnerId) {
    	ResponseDto response = deliveryPartnerAuthService.deactivateAccount(partnerId);
    	return ResponseEntity.ok(response);
    }
}
