package com.quickbite.auth.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.quickbite.auth.dto.RegisterRequestDto;
import com.quickbite.auth.dto.ResponseDto;
import com.quickbite.auth.entity.AuthProvider;
import com.quickbite.auth.entity.User;
import com.quickbite.auth.exception.AccountNotFoundException;
import com.quickbite.auth.exception.PasswordNotMatchException;
import com.quickbite.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {
	
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final TokenBlacklistService tokenBlacklistService;
  

	@Override 
	public ResponseDto register(RegisterRequestDto registerDto) {
        if(userRepository.existsByEmail(registerDto.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        if(userRepository.existsByPhone(registerDto.getPhone())) {
            throw new RuntimeException("Phone already registered");
        }

        User user = new User();
        
        user.setFullName(registerDto.getFullName());
        user.setEmail(registerDto.getEmail());
        user.setPhone(registerDto.getPhone()); 
        user.setRole(registerDto.getRole()); 
        user.setPasswordHash(passwordEncoder.encode(registerDto.getPassword()));
        
        user.setProvider(AuthProvider.LOCAL); 
        user.setIsActive(true); 
        user.setCreatedAt(LocalDateTime.now()); 

        userRepository.save(user); 
        
        String token = jwtService.generateToken(registerDto.getEmail());
        
        return new ResponseDto("Registration successful", token); 
    }

    @Override
    public ResponseDto login(String email, String password) { 
    	User user = userRepository.findByEmail(email).orElseThrow(() -> new AccountNotFoundException("Account not found with this email!!!"));
    	
    	if(!passwordEncoder.matches(password, user.getPasswordHash())) {
    		throw new PasswordNotMatchException("Wrong Password");
    	}
    	
    	user.setCreatedAt(LocalDateTime.now());
    	user.setIsActive(true);
    	
    	userRepository.save(user);
    	
    	String token = jwtService.generateToken(email); 
        
        return new ResponseDto("Login successful", token);  
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
    	User user = userRepository.findByEmail(email)
    			.orElseThrow(() -> new RuntimeException("User not found"));
    	
    	UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    	
    	if(!jwtService.validateToken(token, userDetails)) {
    		throw new RuntimeException("Invalid or expired token");
    	}
    	
    	return new ResponseDto("New Token: ", jwtService.generateToken(user.getEmail())); 
    }
	    
    
//
//    @Override
//    public boolean validateToken(String token) {
//        if (blacklistedTokens.contains(token)) {
//            return false;
//        }
//        return jwtService.validateToken(token);
//    }
//
//
//    @Override
//    public User getUserByEmail(String email) {
//        return userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
//    }
//
//    @Override
//    public User getUserById(int userId) {
//        return userRepository.findById((long) userId)
//                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
//    }
//
//    @Override
//    public ResponseDto updateProfile(int userId, User updatedUser) {
//        User existingUser = userRepository.findById((long) userId)
//                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
//
//        existingUser.setFullName(updatedUser.getFullName());
//        existingUser.setPhone(updatedUser.getPhone());
//        existingUser.setProfilePicUrl(updatedUser.getProfilePicUrl());
//
//        return userRepository.save(existingUser);
//    }
//
//    @Override
//    public void changePassword(int userId, String newPassword) {
//        User user = userRepository.findById((long) userId)
//                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
//
//        user.setPasswordHash(passwordEncoder.encode(newPassword));
//        userRepository.save(user);
//    }
//
//    @Override
//    public void deactivateAccount(int userId) {
//        User user = userRepository.findById((long) userId)
//                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
//
//        user.setIsActive(false);
//        userRepository.save(user);
//    }
//	
	 
	
}