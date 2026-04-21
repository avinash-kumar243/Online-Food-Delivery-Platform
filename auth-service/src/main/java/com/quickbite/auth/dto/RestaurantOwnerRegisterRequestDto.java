package com.quickbite.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RestaurantOwnerRegisterRequestDto {
	
	 @NotBlank
     private String fullName;

     @Email
     @NotBlank
     private String email;

     @NotBlank
     private String password;

     @NotBlank
     private String phone;
     
     @NotBlank
     private String restaurantName;
     
     @NotBlank
     private String restaurantAddress;
     
     @NotBlank
     private String licenseNumber;
     
     @NotBlank
     private String businessRegistration;
}

