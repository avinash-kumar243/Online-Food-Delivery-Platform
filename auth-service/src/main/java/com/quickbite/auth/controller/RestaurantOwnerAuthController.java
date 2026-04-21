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

import com.quickbite.auth.dto.LoginRequestDTO;
import com.quickbite.auth.dto.PasswordChangeRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.dto.RestaurantOwnerRegisterRequestDto;
import com.quickbite.auth.dto.RestaurantOwnerProfileDto;
import com.quickbite.auth.dto.RestaurantOwnerUpdateProfileDto;
import com.quickbite.auth.dto.ForgetPasswordRequestDto;
import com.quickbite.auth.dto.OtpVerificationRequestDto;
import com.quickbite.auth.dto.ResetPasswordRequestDto;
import com.quickbite.auth.service.RestaurantOwnerAuthServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/restaurant")
public class RestaurantOwnerAuthController {
	
	private final RestaurantOwnerAuthServiceImpl restaurantOwnerAuthService;

	@PostMapping("/register")
    public ResponseEntity<ResponseDto> register(@RequestBody RestaurantOwnerRegisterRequestDto user) {
		ResponseDto registeredUser = restaurantOwnerAuthService.register(user); 
        return ResponseEntity.ok(registeredUser); 
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDto> login(@RequestBody LoginRequestDTO user) {
    	ResponseDto loginUser = restaurantOwnerAuthService.login(user.getEmail(), user.getPassword());
        return ResponseEntity.ok(loginUser); 
    }
    
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid or missing Authorization header");
        }

        String token = authHeader.substring(7); 
        restaurantOwnerAuthService.logout(token); 

        return ResponseEntity.ok("Restaurant owner logged out successfully");  
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        ResponseDto user = restaurantOwnerAuthService.refreshToken(token);
        return ResponseEntity.ok(user); 
    }
    
    @GetMapping("/profile/{ownerId}")
    public ResponseEntity<RestaurantOwnerProfileDto> getProfile(@PathVariable Long ownerId) {
    	RestaurantOwnerProfileDto profile = restaurantOwnerAuthService.getProfile(ownerId);
    	return ResponseEntity.ok(profile);
    }
    
    @PutMapping("/profile/{ownerId}")
    public ResponseEntity<RestaurantOwnerProfileDto> updateProfile(@PathVariable Long ownerId,
    		@RequestBody RestaurantOwnerUpdateProfileDto updateDto) {
    	RestaurantOwnerProfileDto profile = restaurantOwnerAuthService.updateProfile(ownerId, updateDto);
    	return ResponseEntity.ok(profile);
    }
    
    @PostMapping("/change-password/{ownerId}")
    public ResponseEntity<ResponseDto> changePassword(@PathVariable Long ownerId,
    		@RequestBody PasswordChangeRequestDto passwordDto) {
    	ResponseDto response = restaurantOwnerAuthService.changePassword(ownerId, passwordDto);
    	return ResponseEntity.ok(response);
    }
    
    @PostMapping("/deactivate/{ownerId}")
    public ResponseEntity<ResponseDto> deactivateAccount(@PathVariable Long ownerId) {
    	ResponseDto response = restaurantOwnerAuthService.deactivateAccount(ownerId);
    	return ResponseEntity.ok(response);
    }

    @PostMapping("/forget-password")
    public ResponseEntity<ResponseDto> forgetPassword(@RequestBody ForgetPasswordRequestDto request) {
    	if (!"RESTAURANT_OWNER".equals(request.getRole())) {
    		return ResponseEntity.badRequest().body(new ResponseDto("Invalid role for this endpoint", ""));
    	}
    	ResponseDto response = restaurantOwnerAuthService.forgetPassword(request.getEmail());
    	return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ResponseDto> verifyOtp(@RequestBody OtpVerificationRequestDto request) {
    	if (!"RESTAURANT_OWNER".equals(request.getRole())) {
    		return ResponseEntity.badRequest().body(new ResponseDto("Invalid role for this endpoint", ""));
    	}
    	ResponseDto response = restaurantOwnerAuthService.verifyOtp(request.getEmail(), request.getOtp());
    	return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResponseDto> resetPassword(@RequestBody ResetPasswordRequestDto request) {
    	if (!"RESTAURANT_OWNER".equals(request.getRole())) {
    		return ResponseEntity.badRequest().body(new ResponseDto("Invalid role for this endpoint", ""));
    	}
    	if (!request.getNewPassword().equals(request.getConfirmPassword())) {
    		return ResponseEntity.badRequest().body(new ResponseDto("New password and confirm password do not match", ""));
    	}
    	ResponseDto response = restaurantOwnerAuthService.resetPassword(request.getEmail(), request.getNewPassword());
    	return ResponseEntity.ok(response);
    }
}
