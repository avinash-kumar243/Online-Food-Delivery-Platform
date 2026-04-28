package com.quickbite.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.quickbite.auth.dto.DeliveryPartnerProfileDto;
import com.quickbite.auth.dto.DeliveryPartnerUpdateProfileDto;
import com.quickbite.auth.dto.PasswordChangeRequestDto;
import com.quickbite.auth.dto.RegisterRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.entity.DeliveryPartner;
import com.quickbite.auth.enums.UserRole;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.exception.AccountNotFoundException;
import com.quickbite.auth.exception.PasswordNotMatchException;
import com.quickbite.auth.repository.DeliveryPartnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryPartnerAuthServiceImpl implements IDeliveryPartnerAuthService {
	
	private final DeliveryPartnerRepository deliveryPartnerRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final TokenBlacklistService tokenBlacklistService;
	private final EmailService emailService;
	private final OtpService otpService;
	private final UserStatusSupport userStatusSupport;

	@Override 
	public ResponseDto register(RegisterRequestDto registerDto) {
        if(deliveryPartnerRepository.existsByEmail(registerDto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if(deliveryPartnerRepository.existsByPhone(registerDto.getPhone())) {
            throw new RuntimeException("Phone already registered");
        }

        DeliveryPartner partner = new DeliveryPartner();
        
        partner.setFullName(registerDto.getFullName());
        partner.setEmail(registerDto.getEmail());
        partner.setPhone(registerDto.getPhone()); 
        partner.setPasswordHash(passwordEncoder.encode(registerDto.getPassword()));
        
        partner.setProvider("LOCAL"); 
        partner.setIsActive(true); 
        partner.setStatus(UserStatus.ACTIVE);
        partner.setIsVerified(false);
        partner.setIsOnline(false);
        partner.setRating(0.0);
        partner.setCreatedAt(LocalDateTime.now()); 

        deliveryPartnerRepository.save(partner); 
        
        String token = jwtService.generateToken(partner.getEmail(), UserRole.DELIVERY_PARTNER.name(), partner.getPartnerId());
        
        return new ResponseDto("Delivery partner registration successful", token, UserRole.DELIVERY_PARTNER.name(), partner.getPartnerId(), partner.getEmail()); 
    }

    @Override
    public ResponseDto login(String email, String password) { 
    	DeliveryPartner partner = deliveryPartnerRepository.findByEmail(email)
    			.orElseThrow(() -> new AccountNotFoundException("Delivery partner account not found with this email!!!"));
		userStatusSupport.ensureActive(userStatusSupport.resolve(partner.getStatus(), partner.getIsActive()), "Delivery partner account");
    	
    	if(!passwordEncoder.matches(password, partner.getPasswordHash())) {
    		throw new PasswordNotMatchException("Wrong Password");
    	}
    	
    	partner.setCreatedAt(LocalDateTime.now());
    	partner.setIsActive(true);
		partner.setStatus(UserStatus.ACTIVE);
    	
    	deliveryPartnerRepository.save(partner);
    	
    	String token = jwtService.generateToken(partner.getEmail(), UserRole.DELIVERY_PARTNER.name(), partner.getPartnerId()); 
        
        return new ResponseDto("Delivery partner login successful", token, UserRole.DELIVERY_PARTNER.name(), partner.getPartnerId(), partner.getEmail());  
    }

    @Override
    public void logout(String token) {
        tokenBlacklistService.blacklistToken(token); 
    }
     
    @Override
    public ResponseDto refreshToken(String token) {
    	if(tokenBlacklistService.isBlacklisted(token)) {  
    		throw new RuntimeException("Token is blacklisted");
    	}
    	    	
    	String email = jwtService.extractEmailFromToken(token);
    	DeliveryPartner partner = deliveryPartnerRepository.findByEmail(email)
    			.orElseThrow(() -> new RuntimeException("Delivery partner not found"));
		userStatusSupport.ensureActive(userStatusSupport.resolve(partner.getStatus(), partner.getIsActive()), "Delivery partner account");
    	
    	UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                partner.getEmail(),
                partner.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_DELIVERY_PARTNER"))
        );
    	
    	if(!jwtService.validateToken(token, userDetails)) {
    		throw new RuntimeException("Invalid or expired token");
    	}
    	
    	String refreshedToken = jwtService.generateToken(partner.getEmail(), UserRole.DELIVERY_PARTNER.name(), partner.getPartnerId());
    	return new ResponseDto("New Token: ", refreshedToken, UserRole.DELIVERY_PARTNER.name(), partner.getPartnerId(), partner.getEmail()); 
    }

	@Override
	public DeliveryPartnerProfileDto getProfile(Long partnerId) {
		DeliveryPartner partner = deliveryPartnerRepository.findByPartnerId(partnerId)
				.orElseThrow(() -> new AccountNotFoundException("Delivery partner not found with id: " + partnerId));
		
		return mapToProfileDto(partner);
	}

	@Override
	public DeliveryPartnerProfileDto updateProfile(Long partnerId, DeliveryPartnerUpdateProfileDto updateDto) {
		DeliveryPartner partner = deliveryPartnerRepository.findByPartnerId(partnerId)
				.orElseThrow(() -> new AccountNotFoundException("Delivery partner not found with id: " + partnerId));
		
		if(updateDto.getVehicleType() != null) {
			partner.setVehicleType(updateDto.getVehicleType());
		}
		if(updateDto.getVehicleNumber() != null) {
			partner.setVehicleNumber(updateDto.getVehicleNumber());
		}
		if(updateDto.getProfilePicUrl() != null) {
			partner.setProfilePicUrl(updateDto.getProfilePicUrl());
		}
		
		if(updateDto.getLicenseNumber() != null) {
			partner.setLicenseNumber(updateDto.getLicenseNumber());
		}
		if(updateDto.getIsVerified() != null) {
			partner.setIsVerified(updateDto.getIsVerified());
		}
		if(updateDto.getIsOnline() != null) {
			partner.setIsOnline(updateDto.getIsOnline());
		}
		
		deliveryPartnerRepository.save(partner);
		return mapToProfileDto(partner);
	}

	@Override
	public ResponseDto changePassword(Long partnerId, PasswordChangeRequestDto passwordDto) {
		DeliveryPartner partner = deliveryPartnerRepository.findByPartnerId(partnerId)
				.orElseThrow(() -> new AccountNotFoundException("Delivery partner not found with id: " + partnerId));
		
		if(!passwordEncoder.matches(passwordDto.getOldPassword(), partner.getPasswordHash())) {
			throw new PasswordNotMatchException("Old password is incorrect");
		}
		
		if(!passwordDto.getNewPassword().equals(passwordDto.getConfirmPassword())) {
			throw new RuntimeException("New password and confirm password do not match");
		}
		
		partner.setPasswordHash(passwordEncoder.encode(passwordDto.getNewPassword()));
		deliveryPartnerRepository.save(partner);
		
		return new ResponseDto("Password changed successfully", "");
	}

	@Override
	public ResponseDto deactivateAccount(Long partnerId) {
		DeliveryPartner partner = deliveryPartnerRepository.findByPartnerId(partnerId)
				.orElseThrow(() -> new AccountNotFoundException("Delivery partner not found with id: " + partnerId));
		
		partner.setIsActive(false);
		partner.setStatus(UserStatus.SUSPENDED);
		deliveryPartnerRepository.save(partner);
		
		return new ResponseDto("Account deactivated successfully", "");
	}

	private DeliveryPartnerProfileDto mapToProfileDto(DeliveryPartner partner) {
		return new DeliveryPartnerProfileDto(
				partner.getPartnerId(),
				partner.getFullName(),
				partner.getEmail(),
				partner.getPhone(),
				partner.getLicenseNumber(),
				partner.getVehicleType(),
				partner.getVehicleNumber(),
				partner.getIsActive(),
				partner.getIsVerified(),
				partner.getIsOnline(),
				partner.getRating(),
				partner.getProfilePicUrl(),
				partner.getCreatedAt()
		);
	}

	@Override
	public ResponseDto forgetPassword(String email) {
		DeliveryPartner partner = deliveryPartnerRepository.findByEmail(email)
				.orElseThrow(() -> new AccountNotFoundException("Account not found with this email"));
		
		String otp = otpService.generateOtp(email);
		emailService.sendOtpEmail(email, otp);
		
		return new ResponseDto("OTP sent to your email", "");
	}
 
	@Override
	public ResponseDto verifyOtp(String email, String otp) {
		if (!otpService.verifyOtp(email, otp)) {
			throw new RuntimeException("Invalid or expired OTP");
		}
		return new ResponseDto("OTP verified successfully", "");
	}

	@Override
	public ResponseDto resetPassword(String email, String newPassword) {
		DeliveryPartner partner = deliveryPartnerRepository.findByEmail(email)
				.orElseThrow(() -> new AccountNotFoundException("Account not found with this email"));
		partner.setPasswordHash(passwordEncoder.encode(newPassword));
		deliveryPartnerRepository.save(partner);
		return new ResponseDto("Password reset successfully", "");
	}
	
}
