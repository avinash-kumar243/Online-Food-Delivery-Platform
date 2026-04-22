package com.quickbite.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PasswordChangeRequestDto {
	
	@NotBlank
	private String oldPassword;
	
	@NotBlank
	private String newPassword;
	
	@NotBlank
	private String confirmPassword;
}