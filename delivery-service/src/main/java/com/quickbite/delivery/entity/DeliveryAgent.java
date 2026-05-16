package com.quickbite.delivery.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
	name = "delivery_agents",
	indexes = {
		@Index(name = "idx_delivery_agents_user_id", columnList = "user_id"),
		@Index(name = "idx_delivery_agents_available", columnList = "is_available"),
		@Index(name = "idx_delivery_agents_verified", columnList = "is_verified"),
		@Index(name = "idx_delivery_agents_active_order", columnList = "active_order_id")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_delivery_agents_user_id", columnNames = "user_id"),
		@UniqueConstraint(name = "uk_delivery_agents_vehicle_number", columnNames = "vehicle_number")
	}
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAgent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long agentId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 120)
	private String fullName;

	@Column(nullable = false, length = 20)
	private String phone;

	@Column(nullable = false, length = 40)
	private String vehicleType;

	@Column(name = "vehicle_number", nullable = false, length = 40)
	private String vehicleNumber;

	@Column(nullable = false)
	private double currentLatitude;

	@Column(nullable = false)
	private double currentLongitude;

	@Column(name = "is_available", nullable = false)
	private boolean available;

	@Column(name = "is_verified", nullable = false)
	private boolean verified;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private VerificationStatus verificationStatus;

	@Column(nullable = false)
	private double avgRating;

	@Column(nullable = false)
	private int totalDeliveries;

	@Column(name = "active_order_id")
	private Long activeOrderId;

	@Column(length = 500)
	private String rejectionReason;

	private Long reviewedByAdminId;

	private LocalDateTime reviewedAt;

	@Column(nullable = false)
	private LocalDateTime submittedAt;
}
