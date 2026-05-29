package com.quickbite.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.auth.dto.ForgetPasswordRequestDto;
import com.quickbite.auth.dto.OtpVerificationRequestDto;
import com.quickbite.auth.dto.ResetPasswordRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.service.CustomerAuthServiceImpl;
import com.quickbite.auth.service.DeliveryPartnerAuthServiceImpl;
import com.quickbite.auth.service.RestaurantOwnerAuthServiceImpl;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class PasswordResetController {

    private final CustomerAuthServiceImpl customerAuthService;
    private final DeliveryPartnerAuthServiceImpl deliveryPartnerAuthService;
    private final RestaurantOwnerAuthServiceImpl restaurantOwnerAuthService;

    @PostMapping("/customer/forget-password")
    public ResponseEntity<ResponseDto> forgetPasswordCustomer(@RequestBody ForgetPasswordRequestDto request) {
        ResponseDto response = customerAuthService.forgetPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delivery-partner/forget-password")
    public ResponseEntity<ResponseDto> forgetPasswordDeliveryPartner(@RequestBody ForgetPasswordRequestDto request) {
        ResponseDto response = deliveryPartnerAuthService.forgetPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restaurant/forget-password")
    public ResponseEntity<ResponseDto> forgetPasswordRestaurantOwner(@RequestBody ForgetPasswordRequestDto request) {
        ResponseDto response = restaurantOwnerAuthService.forgetPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/customer/verify-otp")
    public ResponseEntity<ResponseDto> verifyOtpCustomer(@RequestBody OtpVerificationRequestDto request) {
        ResponseDto response = customerAuthService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delivery-partner/verify-otp")
    public ResponseEntity<ResponseDto> verifyOtpDeliveryPartner(@RequestBody OtpVerificationRequestDto request) {
        ResponseDto response = deliveryPartnerAuthService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restaurant/verify-otp")
    public ResponseEntity<ResponseDto> verifyOtpRestaurantOwner(@RequestBody OtpVerificationRequestDto request) {
        ResponseDto response = restaurantOwnerAuthService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/customer/reset-password")
    public ResponseEntity<ResponseDto> resetPasswordCustomer(@RequestBody ResetPasswordRequestDto request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(new ResponseDto("New password and confirm password do not match", ""));
        }
        ResponseDto response = customerAuthService.resetPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delivery-partner/reset-password")
    public ResponseEntity<ResponseDto> resetPasswordDeliveryPartner(@RequestBody ResetPasswordRequestDto request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(new ResponseDto("New password and confirm password do not match", ""));
        }
        ResponseDto response = deliveryPartnerAuthService.resetPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restaurant/reset-password")
    public ResponseEntity<ResponseDto> resetPasswordRestaurantOwner(@RequestBody ResetPasswordRequestDto request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(new ResponseDto("New password and confirm password do not match", ""));
        }
        ResponseDto response = restaurantOwnerAuthService.resetPassword(request.getEmail(), request.getNewPassword());
        return ResponseEntity.ok(response);
    }
}
