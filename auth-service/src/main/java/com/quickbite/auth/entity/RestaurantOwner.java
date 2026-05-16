package com.quickbite.auth.entity;

import java.time.LocalDateTime;

import com.quickbite.auth.enums.UserStatus;

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
@Table(name = "restaurant_owners") 
public class RestaurantOwner {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long ownerId;
	
	@Column(nullable = false)
	private String fullName;
	
	@Column(nullable = false, unique = true)
	private String email;
	
	@Column(nullable = false, unique = true)
	private String phone;
	 
	private String passwordHash;
	
	@Column(nullable = false)
	private String provider;
	
	@Column(nullable = false) 
	private Boolean isActive;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private UserStatus status;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	private String profilePicUrl;
	
	private String restaurantName;
	
	private String restaurantAddress;
	
	private String licenseNumber;
	
	private String businessRegistration;
	
}
