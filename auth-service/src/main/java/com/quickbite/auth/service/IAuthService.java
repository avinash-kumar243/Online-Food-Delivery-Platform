package com.quickbite.auth.service;

import com.quickbite.auth.dto.RegisterRequestDto;
import com.quickbite.auth.dto.ResponseDto;

public interface IAuthService {
	
	ResponseDto register(RegisterRequestDto user); 
	ResponseDto login(String email, String password);
    void logout(String token);
    
//    
//    boolean validateToken(String token);
    ResponseDto refreshToken(String token); 
//    
//    ResponseDto getUserByEmail(String email);
//    ResponseDto getUserById(int userId); 
//    ResponseDto updateProfile(int userId, RegisterRequestDto updatedUser);
//    void changePassword(int userId, String newPassword);
//    
//    void deactivateAccount(int userId);
} 