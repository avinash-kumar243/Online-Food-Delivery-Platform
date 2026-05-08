package com.quickbite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryPartnerUpdateProfileDto {

	private String fullName;
	private String phone;
	private String vehicleType;
	private String vehicleNumber;
	private String profilePicUrl;
	private String licenseNumber;
	private Boolean isVerified;
	private Boolean isOnline;
}
