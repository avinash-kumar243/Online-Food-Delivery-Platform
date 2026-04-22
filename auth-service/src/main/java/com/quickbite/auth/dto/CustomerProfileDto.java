package com.quickbite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerProfileDto {
	
	private Long customerId;
	private String fullName;
	private String email;
	private String phone;
	private Boolean isActive;
	private String profilePicUrl;
	private LocalDateTime createdAt;
}