package com.quickbite.auth.service;

import com.quickbite.auth.dto.RegisterRequestDto;
import com.quickbite.auth.dto.CustomerProfileDto;
import com.quickbite.auth.dto.CustomerUpdateProfileDto;
import com.quickbite.auth.dto.PasswordChangeRequestDto;
import com.quickbite.auth.dto.ResponseDto;

public interface ICustomerAuthService {
	
	ResponseDto register(RegisterRequestDto user); 
	ResponseDto login(String email, String password);
    void logout(String token);
    ResponseDto refreshToken(String token);
    CustomerProfileDto getProfile(Long customerId);
    CustomerProfileDto updateProfilePic(Long customerId, CustomerUpdateProfileDto updateDto);
    ResponseDto changePassword(Long customerId, PasswordChangeRequestDto passwordDto);
    ResponseDto deactivateAccount(Long customerId);
    ResponseDto forgetPassword(String email);
    ResponseDto verifyOtp(String email, String otp);
    ResponseDto resetPassword(String email, String newPassword);
}
