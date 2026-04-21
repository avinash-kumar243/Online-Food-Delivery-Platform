package com.quickbite.auth.dto;

import com.quickbite.auth.entity.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequestDto {
	
	 @NotBlank
     private String fullName;

     @Email
     @NotBlank
     private String email;

     @NotBlank
     private String password;

     @NotBlank
     private String phone;

     @NotNull
     private Role role; 
}
