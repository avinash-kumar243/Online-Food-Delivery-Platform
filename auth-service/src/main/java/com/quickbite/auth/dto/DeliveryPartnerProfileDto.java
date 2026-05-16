package com.quickbite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryPartnerProfileDto {
	
	private Long partnerId;
	private String fullName;
	private String email;
	private String phone;
	private String licenseNumber;
	private String vehicleType;
	private String vehicleNumber;
	private Boolean isActive;
	private Boolean isVerified;
	private Boolean isOnline;
	private Double rating;
	private String profilePicUrl;
	private LocalDateTime createdAt;
}
