package com.quickbite.auth.service;

import com.quickbite.auth.dto.RestaurantOwnerRegisterRequestDto;
import com.quickbite.auth.dto.RestaurantOwnerProfileDto;
import com.quickbite.auth.dto.RestaurantOwnerUpdateProfileDto;
import com.quickbite.auth.dto.PasswordChangeRequestDto;
import com.quickbite.auth.dto.ResponseDto;

public interface IRestaurantOwnerAuthService {
	
	ResponseDto register(RestaurantOwnerRegisterRequestDto user); 
	ResponseDto login(String email, String password);
    void logout(String token);
    ResponseDto refreshToken(String token);
    RestaurantOwnerProfileDto getProfile(Long ownerId);
    RestaurantOwnerProfileDto updateProfile(Long ownerId, RestaurantOwnerUpdateProfileDto updateDto);
    ResponseDto changePassword(Long ownerId, PasswordChangeRequestDto passwordDto);
    ResponseDto deactivateAccount(Long ownerId);
    ResponseDto forgetPassword(String email);
    ResponseDto verifyOtp(String email, String otp);
    ResponseDto resetPassword(String email, String newPassword);
}
