package com.quickbite.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResponseDto {
	
	@NotBlank
	private String message;
	
	@NotBlank
	private String token;

	private String role;

	private Long userId;

	private String email;

	public ResponseDto(String message, String token) {
		this.message = message;
		this.token = token;
	}

	public ResponseDto(String message, String token, String role, Long userId, String email) {
		this.message = message;
		this.token = token;
		this.role = role;
		this.userId = userId;
		this.email = email;
	}
} 
