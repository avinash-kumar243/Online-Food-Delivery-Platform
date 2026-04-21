package com.quickbite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RestaurantOwnerUpdateProfileDto {
	
	private String fullName;
	private String phone;
	private String restaurantName;
	private String restaurantAddress;
	private String profilePicUrl;
}

