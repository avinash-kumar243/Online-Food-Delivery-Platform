package com.quickbite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantOwnerProfileDto {
	
	private Long ownerId;
	private String fullName;
	private String email;
	private String phone;
	private String restaurantName;
	private String restaurantAddress;
	private String licenseNumber;
	private String businessRegistration;
	private Boolean isActive;
	private String profilePicUrl;
	private LocalDateTime createdAt;
}

