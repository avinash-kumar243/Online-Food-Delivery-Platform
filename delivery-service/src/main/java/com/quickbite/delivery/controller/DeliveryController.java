package com.quickbite.delivery.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.quickbite.delivery.dto.AvailabilityUpdateRequest;
import com.quickbite.delivery.dto.AvailabilityUpdateResponse;
import com.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.delivery.dto.DeliveryRatingRequest;
import com.quickbite.delivery.dto.LocationUpdateRequest;
import com.quickbite.delivery.dto.LocationUpdateResponse;
import com.quickbite.delivery.dto.NearbyAgentResponse;
import com.quickbite.delivery.dto.OrderAssignmentRequest;
import com.quickbite.delivery.dto.RegisterDeliveryAgentRequest;
import com.quickbite.delivery.service.DeliveryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/agents")
@Tag(name = "Delivery Agents", description = "Delivery agent registration, tracking, and assignment APIs")
public class DeliveryController {

	private final DeliveryService deliveryService;

	@PostMapping("/register")
	@Operation(summary = "Register a new delivery agent")
	public ResponseEntity<DeliveryAgentResponse> registerAgent(
			@Valid @RequestBody RegisterDeliveryAgentRequest request) {
		DeliveryAgentResponse response = deliveryService.registerAgent(request);
		URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/agents/{id}")
			.buildAndExpand(response.agentId())
			.toUri();

		return ResponseEntity.created(location).body(response);
	}

	@GetMapping("/{id}")
	@Operation(summary = "Retrieve a delivery agent profile")
	public ResponseEntity<DeliveryAgentResponse> getAgentById(
			@PathVariable @Positive(message = "Agent id must be positive") Long id) {
		return ResponseEntity.ok(deliveryService.getAgentById(id));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<DeliveryAgentResponse> getAgentByUserId(
			@PathVariable @Positive(message = "User id must be positive") Long userId) {
		return ResponseEntity.ok(deliveryService.getAgentByUserId(userId));
	}

	@GetMapping("/nearby")
	@Operation(summary = "Find verified and available nearby delivery agents")
	public ResponseEntity<List<NearbyAgentResponse>> getNearbyAgents(
			@Parameter(description = "Reference latitude")
			@RequestParam
			@DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
			@DecimalMax(value = "90.0", message = "Latitude must be <= 90")
			double latitude,
			@Parameter(description = "Reference longitude")
			@RequestParam
			@DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
			@DecimalMax(value = "180.0", message = "Longitude must be <= 180")
			double longitude,
			@Parameter(description = "Search radius in kilometers")
			@RequestParam
			@Positive(message = "Radius must be positive")
			double radius) {
		return ResponseEntity.ok(deliveryService.getNearbyAgents(latitude, longitude, radius));
	}

	@GetMapping("/available")
	public ResponseEntity<List<DeliveryAgentResponse>> getAvailableAgents() {
		return ResponseEntity.ok(deliveryService.getAvailableAgents());
	}

	@PutMapping("/{id}/location")
	@Operation(summary = "Update a delivery agent's real-time coordinates")
	public ResponseEntity<LocationUpdateResponse> updateLocation(
			@PathVariable @Positive(message = "Agent id must be positive") Long id,
			@Valid @RequestBody LocationUpdateRequest request) {
		return ResponseEntity.ok(deliveryService.updateLocation(id, request));
	}

	@PutMapping("/{id}/availability")
	@Operation(summary = "Toggle a delivery agent's availability")
	public ResponseEntity<AvailabilityUpdateResponse> updateAvailability(
			@PathVariable @Positive(message = "Agent id must be positive") Long id,
			@Valid @RequestBody AvailabilityUpdateRequest request) {
		return ResponseEntity.ok(deliveryService.setAvailability(id, request.available()));
	}

	@PutMapping(value = "/{id}/availability", params = "available")
	public ResponseEntity<AvailabilityUpdateResponse> updateAvailabilityViaQuery(
			@PathVariable @Positive(message = "Agent id must be positive") Long id,
			@RequestParam boolean available) {
		return ResponseEntity.ok(deliveryService.setAvailability(id, available));
	}

	@PatchMapping("/{id}/verify")
	@Operation(summary = "Verify a delivery agent")
	public ResponseEntity<DeliveryAgentResponse> verifyAgent(
			@PathVariable @Positive(message = "Agent id must be positive") Long id) {
		return ResponseEntity.ok(deliveryService.verifyAgent(id, null));
	}

	@PostMapping("/assign")
	@Operation(summary = "Assign an order to a delivery agent")
	public ResponseEntity<DeliveryAgentResponse> assignOrder(
			@Valid @RequestBody OrderAssignmentRequest request) {
		return ResponseEntity.status(HttpStatus.OK).body(deliveryService.assignOrder(request));
	}

	@PostMapping("/{agentId}/accept-order/{orderId}")
	public ResponseEntity<DeliveryAgentResponse> acceptOrder(
			@PathVariable Long agentId,
			@PathVariable Long orderId) {
		return ResponseEntity.ok(deliveryService.acceptOrder(agentId, orderId));
	}

	@PostMapping("/{agentId}/complete-delivery")
	public ResponseEntity<DeliveryAgentResponse> completeDelivery(@PathVariable Long agentId) {
		return ResponseEntity.ok(deliveryService.completeDelivery(agentId));
	}

	@PatchMapping("/{id}/rating")
	public ResponseEntity<DeliveryAgentResponse> updateAverageRating(
			@PathVariable Long id,
			@Valid @RequestBody DeliveryRatingRequest request) {
		return ResponseEntity.ok(deliveryService.updateAverageRating(id, request.avgRating()));
	}

	@GetMapping("/active")
	@Operation(summary = "List agents currently on active deliveries")
	public ResponseEntity<List<DeliveryAgentResponse>> getActiveAgents() {
		return ResponseEntity.ok(deliveryService.getActiveAgents());
	}
}
