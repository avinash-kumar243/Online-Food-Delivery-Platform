package com.quickbite.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomerUpdateProfileDto {
	private String fullName;
	private String phone;
	private String profilePicUrl;
}
