package com.quickbite.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.quickbite.auth.dto.RestaurantOwnerRegisterRequestDto;
import com.quickbite.auth.dto.RestaurantOwnerProfileDto;
import com.quickbite.auth.dto.RestaurantOwnerUpdateProfileDto;
import com.quickbite.auth.dto.PasswordChangeRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.entity.RestaurantOwner;
import com.quickbite.auth.exception.AccountNotFoundException;
import com.quickbite.auth.exception.PasswordNotMatchException;
import com.quickbite.auth.repository.RestaurantOwnerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantOwnerAuthServiceImpl implements IRestaurantOwnerAuthService {
	
	private final RestaurantOwnerRepository restaurantOwnerRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final TokenBlacklistService tokenBlacklistService;
	private final EmailService emailService;
	private final OtpService otpService;

	@Override 
	public ResponseDto register(RestaurantOwnerRegisterRequestDto registerDto) {
        if(restaurantOwnerRepository.existsByEmail(registerDto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if(restaurantOwnerRepository.existsByPhone(registerDto.getPhone())) {
            throw new RuntimeException("Phone already registered");
        }

        RestaurantOwner owner = new RestaurantOwner();
        
        owner.setFullName(registerDto.getFullName());
        owner.setEmail(registerDto.getEmail());
        owner.setPhone(registerDto.getPhone()); 
        owner.setPasswordHash(passwordEncoder.encode(registerDto.getPassword()));
        owner.setRestaurantName(registerDto.getRestaurantName());
        owner.setRestaurantAddress(registerDto.getRestaurantAddress());
        owner.setLicenseNumber(registerDto.getLicenseNumber());
        owner.setBusinessRegistration(registerDto.getBusinessRegistration());
        
        owner.setProvider("LOCAL"); 
        owner.setIsActive(true); 
        owner.setCreatedAt(LocalDateTime.now()); 

        restaurantOwnerRepository.save(owner); 
        
        String token = jwtService.generateToken(registerDto.getEmail());
        
        return new ResponseDto("Restaurant owner registration successful", token); 
    }

    @Override
    public ResponseDto login(String email, String password) { 
    	RestaurantOwner owner = restaurantOwnerRepository.findByEmail(email)
    			.orElseThrow(() -> new AccountNotFoundException("Restaurant account not found with this email!!!"));
    	
    	if(!passwordEncoder.matches(password, owner.getPasswordHash())) {
    		throw new PasswordNotMatchException("Wrong Password");
    	}
    	
    	owner.setCreatedAt(LocalDateTime.now());
    	owner.setIsActive(true);
    	
    	restaurantOwnerRepository.save(owner);
    	
    	String token = jwtService.generateToken(email); 
        
        return new ResponseDto("Restaurant owner login successful", token);  
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
    	RestaurantOwner owner = restaurantOwnerRepository.findByEmail(email)
    			.orElseThrow(() -> new RuntimeException("Restaurant owner not found"));
    	
    	UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                owner.getEmail(),
                owner.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_RESTAURANT_OWNER"))
        );
    	
    	if(!jwtService.validateToken(token, userDetails)) {
    		throw new RuntimeException("Invalid or expired token");
    	}
    	
    	return new ResponseDto("New Token: ", jwtService.generateToken(owner.getEmail())); 
    }

	@Override
	public RestaurantOwnerProfileDto getProfile(Long ownerId) {
		RestaurantOwner owner = restaurantOwnerRepository.findByOwnerId(ownerId)
				.orElseThrow(() -> new AccountNotFoundException("Restaurant owner not found with id: " + ownerId));
		
		return mapToProfileDto(owner);
	}

	@Override
	public RestaurantOwnerProfileDto updateProfile(Long ownerId, RestaurantOwnerUpdateProfileDto updateDto) {
		RestaurantOwner owner = restaurantOwnerRepository.findByOwnerId(ownerId)
				.orElseThrow(() -> new AccountNotFoundException("Restaurant owner not found with id: " + ownerId));
		
		if(updateDto.getFullName() != null) {
			owner.setFullName(updateDto.getFullName());
		}
		if(updateDto.getPhone() != null && !updateDto.getPhone().equals(owner.getPhone())) {
			if(restaurantOwnerRepository.existsByPhone(updateDto.getPhone())) {
				throw new RuntimeException("Phone already in use");
			}
			owner.setPhone(updateDto.getPhone());
		}
		if(updateDto.getRestaurantName() != null) {
			owner.setRestaurantName(updateDto.getRestaurantName());
		}
		if(updateDto.getRestaurantAddress() != null) {
			owner.setRestaurantAddress(updateDto.getRestaurantAddress());
		}
		if(updateDto.getProfilePicUrl() != null) {
			owner.setProfilePicUrl(updateDto.getProfilePicUrl());
		}
		
		restaurantOwnerRepository.save(owner);
		return mapToProfileDto(owner);
	}

	@Override
	public ResponseDto changePassword(Long ownerId, PasswordChangeRequestDto passwordDto) {
		RestaurantOwner owner = restaurantOwnerRepository.findByOwnerId(ownerId)
				.orElseThrow(() -> new AccountNotFoundException("Restaurant owner not found with id: " + ownerId));
		
		if(!passwordEncoder.matches(passwordDto.getOldPassword(), owner.getPasswordHash())) {
			throw new PasswordNotMatchException("Old password is incorrect");
		}
		
		if(!passwordDto.getNewPassword().equals(passwordDto.getConfirmPassword())) {
			throw new RuntimeException("New password and confirm password do not match");
		}
		
		owner.setPasswordHash(passwordEncoder.encode(passwordDto.getNewPassword()));
		restaurantOwnerRepository.save(owner);
		
		return new ResponseDto("Password changed successfully", "");
	}

	@Override
	public ResponseDto deactivateAccount(Long ownerId) {
		RestaurantOwner owner = restaurantOwnerRepository.findByOwnerId(ownerId)
				.orElseThrow(() -> new AccountNotFoundException("Restaurant owner not found with id: " + ownerId));
		
		owner.setIsActive(false);
		restaurantOwnerRepository.save(owner);
		
		return new ResponseDto("Account deactivated successfully", "");
	}

	private RestaurantOwnerProfileDto mapToProfileDto(RestaurantOwner owner) {
		return new RestaurantOwnerProfileDto(
				owner.getOwnerId(),
				owner.getFullName(),
				owner.getEmail(),
				owner.getPhone(),
				owner.getRestaurantName(),
				owner.getRestaurantAddress(),
				owner.getLicenseNumber(),
				owner.getBusinessRegistration(),
				owner.getIsActive(),
				owner.getProfilePicUrl(),
				owner.getCreatedAt()
		);
	}

	@Override
	public ResponseDto forgetPassword(String email) {
		RestaurantOwner owner = restaurantOwnerRepository.findByEmail(email)
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
		RestaurantOwner owner = restaurantOwnerRepository.findByEmail(email)
				.orElseThrow(() -> new AccountNotFoundException("Account not found with this email"));
		owner.setPasswordHash(passwordEncoder.encode(newPassword));
		restaurantOwnerRepository.save(owner);
		return new ResponseDto("Password reset successfully", "");
	}
	
}
