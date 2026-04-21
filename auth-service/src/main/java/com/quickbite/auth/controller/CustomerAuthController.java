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

import com.quickbite.auth.dto.CustomerRegisterRequestDto;
import com.quickbite.auth.dto.CustomerProfileDto;
import com.quickbite.auth.dto.CustomerUpdateProfileDto;
import com.quickbite.auth.dto.LoginRequestDTO;
import com.quickbite.auth.dto.PasswordChangeRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.service.CustomerAuthServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/customer")
public class CustomerAuthController {
	
	private final CustomerAuthServiceImpl customerAuthService;

	@PostMapping("/register")
    public ResponseEntity<ResponseDto> register(@RequestBody CustomerRegisterRequestDto user) {
		ResponseDto registeredUser = customerAuthService.register(user); 
        return ResponseEntity.ok(registeredUser); 
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDto> login(@RequestBody LoginRequestDTO user) {
    	ResponseDto loginUser = customerAuthService.login(user.getEmail(), user.getPassword());
        return ResponseEntity.ok(loginUser); 
    }
    
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid or missing Authorization header");
        }

        String token = authHeader.substring(7); 
        customerAuthService.logout(token); 

        return ResponseEntity.ok("Customer logged out successfully"); 
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        ResponseDto user = customerAuthService.refreshToken(token);
        return ResponseEntity.ok(user); 
    }
    
    @GetMapping("/profile/{customerId}")
    public ResponseEntity<CustomerProfileDto> getProfile(@PathVariable Long customerId) {
    	CustomerProfileDto profile = customerAuthService.getProfile(customerId);
    	return ResponseEntity.ok(profile);
    }
    
    @PutMapping("/profile/{customerId}")
    public ResponseEntity<CustomerProfileDto> updateProfile(@PathVariable Long customerId,
    		@RequestBody CustomerUpdateProfileDto updateDto) {
    	CustomerProfileDto profile = customerAuthService.updateProfile(customerId, updateDto);
    	return ResponseEntity.ok(profile);
    }
    
    @PostMapping("/change-password/{customerId}")
    public ResponseEntity<ResponseDto> changePassword(@PathVariable Long customerId,
    		@RequestBody PasswordChangeRequestDto passwordDto) {
    	ResponseDto response = customerAuthService.changePassword(customerId, passwordDto);
    	return ResponseEntity.ok(response);
    }
    
    @PostMapping("/deactivate/{customerId}")
    public ResponseEntity<ResponseDto> deactivateAccount(@PathVariable Long customerId) {
    	ResponseDto response = customerAuthService.deactivateAccount(customerId);
    	return ResponseEntity.ok(response);
    }
}
