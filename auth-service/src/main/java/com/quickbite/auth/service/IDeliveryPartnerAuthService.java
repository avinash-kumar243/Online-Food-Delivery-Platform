package com.quickbite.auth.service;

import com.quickbite.auth.dto.DeliveryPartnerRegisterRequestDto;
import com.quickbite.auth.dto.DeliveryPartnerProfileDto;
import com.quickbite.auth.dto.DeliveryPartnerUpdateProfileDto;
import com.quickbite.auth.dto.PasswordChangeRequestDto;
import com.quickbite.auth.dto.ResponseDto;

public interface IDeliveryPartnerAuthService {
	
	ResponseDto register(DeliveryPartnerRegisterRequestDto user); 
	ResponseDto login(String email, String password);
    void logout(String token);
    ResponseDto refreshToken(String token);
    DeliveryPartnerProfileDto getProfile(Long partnerId);
    DeliveryPartnerProfileDto updateProfile(Long partnerId, DeliveryPartnerUpdateProfileDto updateDto);
    ResponseDto changePassword(Long partnerId, PasswordChangeRequestDto passwordDto);
    ResponseDto deactivateAccount(Long partnerId);
    ResponseDto forgetPassword(String email);
    ResponseDto verifyOtp(String email, String otp);
    ResponseDto resetPassword(String email, String newPassword);
}
