package com.quickbite.auth.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users") 
public class User {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long userId;
	
	@Column(nullable = false)
	private String fullName;
	
	@Column(nullable = false, unique = true)
	private String email;
	
	@Column(nullable = false, unique = true)
	private String phone;
	 
	@Enumerated(EnumType.STRING) 
	@Column(nullable = false)
	private Role role;
		
	private String passwordHash;
	
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuthProvider provider;
	
	@Column(nullable = false) 
	private Boolean isActive;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private String profilePicUrl;
	
}