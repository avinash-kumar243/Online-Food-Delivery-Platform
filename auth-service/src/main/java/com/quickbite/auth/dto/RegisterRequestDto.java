package com.quickbite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}