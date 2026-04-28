package com.quickbite.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.quickbite.auth.dto.RegisterRequestDto;
import com.quickbite.auth.dto.CustomerProfileDto;
import com.quickbite.auth.dto.CustomerUpdateProfileDto;
import com.quickbite.auth.dto.PasswordChangeRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.entity.Customer;
import com.quickbite.auth.enums.UserRole;
import com.quickbite.auth.enums.UserStatus;
import com.quickbite.auth.exception.AccountNotFoundException;
import com.quickbite.auth.exception.PasswordNotMatchException;
import com.quickbite.auth.repository.CustomerRepository;

import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerAuthServiceImpl implements ICustomerAuthService {
	
	private final CustomerRepository customerRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final TokenBlacklistService tokenBlacklistService;
	private final EmailService emailService;
	private final OtpService otpService;
	private final UserStatusSupport userStatusSupport;

	@Override 
	public ResponseDto register(RegisterRequestDto registerDto) {
        if(customerRepository.existsByEmail(registerDto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if(customerRepository.existsByPhone(registerDto.getPhone())) {
            throw new RuntimeException("Phone already registered");
        }

        Customer customer = new Customer();
        
        customer.setFullName(registerDto.getFullName());
        customer.setEmail(registerDto.getEmail());
        customer.setPhone(registerDto.getPhone()); 
        customer.setPasswordHash(passwordEncoder.encode(registerDto.getPassword()));
        
        customer.setProvider("LOCAL"); 
        customer.setIsActive(true); 
        customer.setStatus(UserStatus.ACTIVE);
        customer.setCreatedAt(LocalDateTime.now()); 

        customerRepository.save(customer); 
        
        String token = jwtService.generateToken(customer.getEmail(), UserRole.CUSTOMER.name(), customer.getCustomerId());
        
        return new ResponseDto("Customer registration successful", token, UserRole.CUSTOMER.name(), customer.getCustomerId(), customer.getEmail()); 
    }

    @Override
    public ResponseDto login(String email, String password) { 
    	Customer customer = customerRepository.findByEmail(email).orElseThrow(() -> new AccountNotFoundException("Account not found with this email!!!"));
		userStatusSupport.ensureActive(userStatusSupport.resolve(customer.getStatus(), customer.getIsActive()), "Customer account");
    	
    	if(!passwordEncoder.matches(password, customer.getPasswordHash())) {
    		throw new PasswordNotMatchException("Wrong Password");
    	}
    	
    	customer.setCreatedAt(LocalDateTime.now());
    	customer.setIsActive(true);
		customer.setStatus(UserStatus.ACTIVE);
    	
    	customerRepository.save(customer);
    	
    	String token = jwtService.generateToken(customer.getEmail(), UserRole.CUSTOMER.name(), customer.getCustomerId()); 
        
        return new ResponseDto("Customer login successful", token, UserRole.CUSTOMER.name(), customer.getCustomerId(), customer.getEmail());  
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
    	Customer customer = customerRepository.findByEmail(email)
    			.orElseThrow(() -> new RuntimeException("Customer not found"));
		userStatusSupport.ensureActive(userStatusSupport.resolve(customer.getStatus(), customer.getIsActive()), "Customer account");
    	
    	UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                customer.getEmail(),
                customer.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
    	
    	if(!jwtService.validateToken(token, userDetails)) {
    		throw new RuntimeException("Invalid or expired token");
    	}
    	
    	String refreshedToken = jwtService.generateToken(customer.getEmail(), UserRole.CUSTOMER.name(), customer.getCustomerId());
    	return new ResponseDto("New Token: ", refreshedToken, UserRole.CUSTOMER.name(), customer.getCustomerId(), customer.getEmail()); 
    }

	@Override
	public CustomerProfileDto getProfile(Long customerId) {
		Customer customer = customerRepository.findByCustomerId(customerId)
				.orElseThrow(() -> new AccountNotFoundException("Customer not found with id: " + customerId));
		
		return mapToProfileDto(customer);
	}

	@Override
	public CustomerProfileDto updateProfilePic(Long customerId, CustomerUpdateProfileDto updateDto) {
		Customer customer = customerRepository.findByCustomerId(customerId)
				.orElseThrow(() -> new AccountNotFoundException("Customer not found with id: " + customerId));

		if(updateDto.getProfilePicUrl() != null) {
			customer.setProfilePicUrl(updateDto.getProfilePicUrl());
		}
		
		customerRepository.save(customer);
		return mapToProfileDto(customer);
	}

	@Override
	public ResponseDto changePassword(Long customerId, PasswordChangeRequestDto passwordDto) {
		Customer customer = customerRepository.findByCustomerId(customerId)
				.orElseThrow(() -> new AccountNotFoundException("Customer not found with id: " + customerId));
		
		if(!passwordEncoder.matches(passwordDto.getOldPassword(), customer.getPasswordHash())) {
			throw new PasswordNotMatchException("Old password is incorrect");
		}
		
		if(!passwordDto.getNewPassword().equals(passwordDto.getConfirmPassword())) {
			throw new RuntimeException("New password and confirm password do not match");
		}
		
		customer.setPasswordHash(passwordEncoder.encode(passwordDto.getNewPassword()));
		customerRepository.save(customer);
		
		return new ResponseDto("Password changed successfully", "");
	}

	@Override
	public ResponseDto deactivateAccount(Long customerId) {
		Customer customer = customerRepository.findByCustomerId(customerId)
				.orElseThrow(() -> new AccountNotFoundException("Customer not found with id: " + customerId));
		
		customer.setIsActive(false);
		customer.setStatus(UserStatus.SUSPENDED);
		customerRepository.save(customer);
		
		return new ResponseDto("Account deactivated successfully", "");
	}

	private CustomerProfileDto mapToProfileDto(Customer customer) {
		return new CustomerProfileDto(
				customer.getCustomerId(),
				customer.getFullName(),
				customer.getEmail(),
				customer.getPhone(),
				customer.getIsActive(),
				customer.getProfilePicUrl(),
				customer.getCreatedAt()
		);
	}

	@Override
	public ResponseDto forgetPassword(String email) {
		Customer customer = customerRepository.findByEmail(email)
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
		Customer customer = customerRepository.findByEmail(email)
				.orElseThrow(() -> new AccountNotFoundException("Account not found with this email"));
		customer.setPasswordHash(passwordEncoder.encode(newPassword));
		customerRepository.save(customer);
		return new ResponseDto("Password reset successfully", "");
	}
	
}
