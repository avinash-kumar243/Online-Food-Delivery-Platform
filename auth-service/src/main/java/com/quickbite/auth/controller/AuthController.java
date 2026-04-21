package com.quickbite.auth.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
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
import com.quickbite.auth.dto.RegisterRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.entity.User;
import com.quickbite.auth.service.AuthServiceImpl;

import lombok.RequiredArgsConstructor; 

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
	
	private final AuthServiceImpl authService;   
	  
	
	@PostMapping("/register")
    public ResponseEntity<ResponseDto> register(@RequestBody RegisterRequestDto user) {
		ResponseDto registeredUser = authService.register(user); 
        return ResponseEntity.ok(registeredUser); 
    }
	
  
    @PostMapping("/login")
    public ResponseEntity<ResponseDto> login(@RequestBody LoginRequestDTO user) {
    	ResponseDto loginUser = authService.login(user.getEmail(), user.getPassword());
        return ResponseEntity.ok(loginUser); 
    }
    
    
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Invalid or missing Authorization header");
        }

        String token = authHeader.substring(7); 
        authService.logout(token); 

        return ResponseEntity.ok("Logged out successfully"); 
    }
    
 
    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto> refresh(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;

        ResponseDto user = authService.refreshToken(token);
        return ResponseEntity.ok(user); 
    } 
    
//
//    @GetMapping("/profile/{id}")
//    public ResponseEntity<?> getProfile(@PathVariable("id") int userId) {
//        User user = authService.getUserById(userId);
//        return ResponseEntity.ok(user);
//    }
//
//    @PutMapping("/profile/{id}")
//    public ResponseEntity<?> updateProfile(@PathVariable("id") int userId,
//                                           @RequestBody User updatedUser) {
//        User user = authService.updateProfile(userId, updatedUser);
//        return ResponseEntity.ok(user);
//    }
//
//    @PutMapping("/password/{id}")
//    public ResponseEntity<?> changePassword(@PathVariable("id") int userId,
//                                            @RequestBody Map<String, String> request) {
//        String newPassword = request.get("newPassword");
//        authService.changePassword(userId, newPassword);
//        return ResponseEntity.ok("Password changed successfully");
//    }
//
//    @PutMapping("/deactivate/{id}")
//    public ResponseEntity<?> deactivateAccount(@PathVariable("id") int userId) {
//        authService.deactivateAccount(userId);
//        return ResponseEntity.ok("Account deactivated successfully");
//    }
}