package com.quickbite.delivery.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.delivery.client.AuthServiceClient;
import com.quickbite.delivery.client.OrderServiceClient;
import com.quickbite.delivery.dto.AdminDeliveryAgentResponse;
import com.quickbite.delivery.dto.AssignOrderRequestDto;
import com.quickbite.delivery.dto.AvailabilityUpdateResponse;
import com.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.delivery.dto.LocationUpdateRequest;
import com.quickbite.delivery.dto.LocationUpdateResponse;
import com.quickbite.delivery.dto.NearbyAgentResponse;
import com.quickbite.delivery.dto.OrderAssignmentRequest;
import com.quickbite.delivery.dto.RegisterDeliveryAgentRequest;
import com.quickbite.delivery.dto.UserSummaryDto;
import com.quickbite.delivery.entity.DeliveryAgent;
import com.quickbite.delivery.entity.VerificationStatus;
import com.quickbite.delivery.exception.BadRequestException;
import com.quickbite.delivery.exception.ConflictException;
import com.quickbite.delivery.exception.ResourceNotFoundException;
import com.quickbite.delivery.messaging.GenericEventPublisher;
import com.quickbite.delivery.messaging.dto.DeliveryEventDTO;
import com.quickbite.delivery.messaging.dto.OrderEventDTO;
import com.quickbite.delivery.repository.DeliveryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

	private static final double EARTH_RADIUS_KM = 6371.0;

	private final DeliveryRepository deliveryRepository;
	private final AuthServiceClient authServiceClient;
	private final OrderServiceClient orderServiceClient;
	private final GenericEventPublisher eventPublisher;

	@Override
	@Transactional
	public DeliveryAgentResponse registerAgent(RegisterDeliveryAgentRequest request) {
		validateInitialCoordinates(request.currentLatitude(), request.currentLongitude());

		DeliveryAgent agent = deliveryRepository.findByUserId(request.userId()).orElseGet(() -> DeliveryAgent.builder()
			.userId(request.userId())
			.avgRating(0.0)
			.totalDeliveries(0)
			.activeOrderId(null)
			.build());

		if (agent.getAgentId() == null) {
			if (deliveryRepository.existsByVehicleNumberIgnoreCase(request.vehicleNumber())) {
				throw new ConflictException("Vehicle number is already registered: " + request.vehicleNumber());
			}
		} else if (deliveryRepository.existsByVehicleNumberIgnoreCaseAndAgentIdNot(request.vehicleNumber(), agent.getAgentId())) {
			throw new ConflictException("Vehicle number is already registered: " + request.vehicleNumber());
		}

		agent.setFullName(request.fullName().trim());
		agent.setPhone(request.phone().trim());
		agent.setVehicleType(request.vehicleType().trim());
		agent.setVehicleNumber(request.vehicleNumber().trim());
		agent.setCurrentLatitude(defaultCoordinate(request.currentLatitude()));
		agent.setCurrentLongitude(defaultCoordinate(request.currentLongitude()));
		agent.setAvailable(false);
		agent.setVerified(false);
		agent.setVerificationStatus(VerificationStatus.PENDING);
		agent.setRejectionReason(null);
		agent.setReviewedByAdminId(null);
		agent.setReviewedAt(null);
		agent.setSubmittedAt(java.time.LocalDateTime.now());

		return toResponse(deliveryRepository.save(agent));
	}

	@Override
	public DeliveryAgentResponse getAgentById(Long agentId) {
		return toResponse(getAgent(agentId));
	}

	@Override
	public DeliveryAgentResponse getAgentByUserId(Long userId) {
		return toResponse(deliveryRepository.findByUserId(userId)
			.orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found for userId " + userId)));
	}

	@Override
	public List<NearbyAgentResponse> getNearbyAgents(double latitude, double longitude, double radiusInKm) {
		return deliveryRepository.findByIsAvailableTrue().stream()
			.filter(agent -> agent.getVerificationStatus() == VerificationStatus.VERIFIED)
			.filter(agent -> agent.getActiveOrderId() == null)
			.map(agent -> new NearbyAgentCandidate(agent,
				calculateDistanceInKm(latitude, longitude, agent.getCurrentLatitude(), agent.getCurrentLongitude())))
			.filter(candidate -> candidate.distanceKm() <= radiusInKm)
			.sorted(Comparator.comparingDouble(NearbyAgentCandidate::distanceKm))
			.map(candidate -> toNearbyResponse(candidate.agent(), candidate.distanceKm()))
			.toList();
	}

	@Override
	public List<DeliveryAgentResponse> getAvailableAgents() {
		return deliveryRepository.findByIsAvailableTrue().stream()
			.filter(agent -> agent.getVerificationStatus() == VerificationStatus.VERIFIED)
			.filter(agent -> agent.getActiveOrderId() == null)
			.map(this::toResponse)
			.toList();
	}

	@Override
	@Transactional
	public LocationUpdateResponse updateLocation(Long agentId, LocationUpdateRequest request) {
		int updatedRows = deliveryRepository.updateLocation(agentId, request.latitude(), request.longitude());
		if (updatedRows == 0) {
			throw new ResourceNotFoundException("Delivery agent not found with id " + agentId);
		}

		return new LocationUpdateResponse(agentId, request.latitude(), request.longitude());
	}

	@Override
	@Transactional
	public AvailabilityUpdateResponse setAvailability(Long agentId, boolean available) {
		DeliveryAgent agent = getAgent(agentId);
		if (available && agent.getVerificationStatus() != VerificationStatus.VERIFIED) {
			throw new ConflictException("Only verified delivery partners can go online");
		}
		if (available && agent.getActiveOrderId() != null) {
			throw new ConflictException("Agent cannot be marked available while an active delivery is assigned");
		}

		agent.setAvailable(available);
		deliveryRepository.save(agent);

		return new AvailabilityUpdateResponse(agent.getAgentId(), agent.isAvailable(), agent.getActiveOrderId());
	}

	@Override
	@Transactional
	public List<AdminDeliveryAgentResponse> getPendingAgents() {
		return deliveryRepository.findByVerificationStatus(VerificationStatus.PENDING).stream()
			.sorted(Comparator.comparing(DeliveryAgent::getSubmittedAt).reversed())
			.map(this::toAdminResponse)
			.toList();
	}

	@Override
	public List<AdminDeliveryAgentResponse> getAllAgentsForAdmin() {
		return deliveryRepository.findAll().stream()
			.sorted(Comparator.comparing(DeliveryAgent::getSubmittedAt).reversed())
			.map(this::toAdminResponse)
			.toList();
	}

	@Override
	@Transactional
	public DeliveryAgentResponse verifyAgent(Long agentId, Long adminId) {
		DeliveryAgent agent = getAgent(agentId);
		agent.setVerified(true);
		agent.setVerificationStatus(VerificationStatus.VERIFIED);
		agent.setRejectionReason(null);
		agent.setReviewedByAdminId(adminId);
		agent.setReviewedAt(java.time.LocalDateTime.now());
		return toResponse(deliveryRepository.save(agent));
	}

	@Override
	@Transactional
	public DeliveryAgentResponse rejectAgent(Long agentId, Long adminId, String feedback) {
		DeliveryAgent agent = getAgent(agentId);
		agent.setVerified(false);
		agent.setAvailable(false);
		agent.setVerificationStatus(VerificationStatus.REJECTED);
		agent.setRejectionReason(feedback);
		agent.setReviewedByAdminId(adminId);
		agent.setReviewedAt(java.time.LocalDateTime.now());
		return toResponse(deliveryRepository.save(agent));
	}

	@Override
	@Transactional
	public DeliveryAgentResponse assignOrder(OrderAssignmentRequest request) {
		DeliveryAgent agent = getAgentForUpdate(request.agentId());
		if (agent.getVerificationStatus() != VerificationStatus.VERIFIED) {
			throw new ConflictException("Only verified delivery agents can be assigned orders");
		}
		if (!agent.isAvailable()) {
			throw new ConflictException("Delivery agent is currently unavailable");
		}
		if (agent.getActiveOrderId() != null) {
			throw new ConflictException("Delivery agent already has an active delivery");
		}

		agent.setAvailable(false);
		agent.setActiveOrderId(request.orderId());
		DeliveryAgent savedAgent = deliveryRepository.save(agent);
		eventPublisher.send("delivery.assigned", new DeliveryEventDTO(
			request.orderId(),
			savedAgent.getAgentId(),
			"ASSIGNED",
			savedAgent.getCurrentLatitude() + "," + savedAgent.getCurrentLongitude()
		));
		return toResponse(savedAgent);
	}

	@Override
	@Transactional
	public DeliveryAgentResponse acceptOrder(Long agentId, Long orderId) {
		DeliveryAgent agent = getAgentForUpdate(agentId);
		if (agent.getVerificationStatus() != VerificationStatus.VERIFIED) {
			throw new ConflictException("Only verified delivery partners can accept orders");
		}
		if (agent.getActiveOrderId() == null || !agent.getActiveOrderId().equals(orderId)) {
			throw new ConflictException("Delivery partner is not assigned to this order");
		}

		eventPublisher.send("order.pickedup", new DeliveryEventDTO(
			orderId,
			agentId,
			"PICKED_UP",
			agent.getCurrentLatitude() + "," + agent.getCurrentLongitude()
		));
		return toResponse(agent);
	}

	@Override
	@Transactional
	public DeliveryAgentResponse completeDelivery(Long agentId) {
		DeliveryAgent agent = getAgentForUpdate(agentId);
		if (agent.getActiveOrderId() == null) {
			throw new ConflictException("Delivery agent does not have an active delivery to complete");
		}
		Long completedOrderId = agent.getActiveOrderId();

		agent.setActiveOrderId(null);
		agent.setAvailable(true);
		agent.setTotalDeliveries(agent.getTotalDeliveries() + 1);
		DeliveryAgent savedAgent = deliveryRepository.save(agent);
		eventPublisher.send("order.delivered", new OrderEventDTO(
			completedOrderId,
			null,
			null,
			null,
			java.time.LocalDateTime.now()
		));
		return toResponse(savedAgent);
	}

	@Override
	public List<DeliveryAgentResponse> getActiveAgents() {
		return deliveryRepository.findByActiveOrderIdIsNotNull().stream()
			.map(this::toResponse)
			.toList();
	}

	private DeliveryAgent getAgent(Long agentId) {
		return deliveryRepository.findById(agentId)
			.orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found with id " + agentId));
	}

	private DeliveryAgent getAgentForUpdate(Long agentId) {
		return deliveryRepository.findByIdForUpdate(agentId)
			.orElseThrow(() -> new ResourceNotFoundException("Delivery agent not found with id " + agentId));
	}

	private DeliveryAgentResponse toResponse(DeliveryAgent agent) {
		VerificationStatus verificationStatus = agent.getVerificationStatus() == null
			? (agent.isVerified() ? VerificationStatus.VERIFIED : VerificationStatus.PENDING)
			: agent.getVerificationStatus();
		return new DeliveryAgentResponse(
			agent.getAgentId(),
			agent.getUserId(),
			agent.getFullName(),
			agent.getPhone(),
			agent.getVehicleType(),
			agent.getVehicleNumber(),
			agent.getCurrentLatitude(),
			agent.getCurrentLongitude(),
			agent.isAvailable(),
			agent.isVerified(),
			verificationStatus.name(),
			agent.getAvgRating(),
			agent.getTotalDeliveries(),
			agent.getActiveOrderId(),
			agent.getRejectionReason(),
			agent.getReviewedByAdminId(),
			agent.getReviewedAt(),
			agent.getSubmittedAt()
		);
	}

	private NearbyAgentResponse toNearbyResponse(DeliveryAgent agent, double distanceKm) {
		return new NearbyAgentResponse(
			agent.getAgentId(),
			agent.getUserId(),
			agent.getFullName(),
			agent.getPhone(),
			agent.getVehicleType(),
			agent.getVehicleNumber(),
			agent.getCurrentLatitude(),
			agent.getCurrentLongitude(),
			agent.isAvailable(),
			agent.isVerified(),
			agent.getAvgRating(),
			agent.getTotalDeliveries(),
			agent.getActiveOrderId(),
			distanceKm
		);
	}

	private void validateInitialCoordinates(Double latitude, Double longitude) {
		if ((latitude == null && longitude == null) || (latitude != null && longitude != null)) {
			return;
		}

		throw new BadRequestException("Both latitude and longitude must be provided together during registration");
	}

	private double defaultCoordinate(Double coordinate) {
		return coordinate == null ? 0.0 : coordinate;
	}

	private double calculateDistanceInKm(
			double sourceLatitude,
			double sourceLongitude,
			double targetLatitude,
			double targetLongitude) {
		double latitudeDistance = Math.toRadians(targetLatitude - sourceLatitude);
		double longitudeDistance = Math.toRadians(targetLongitude - sourceLongitude);
		double sourceLatitudeRadians = Math.toRadians(sourceLatitude);
		double targetLatitudeRadians = Math.toRadians(targetLatitude);

		double haversine = Math.pow(Math.sin(latitudeDistance / 2), 2)
			+ Math.cos(sourceLatitudeRadians)
			* Math.cos(targetLatitudeRadians)
			* Math.pow(Math.sin(longitudeDistance / 2), 2);

		double angularDistance = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
		return EARTH_RADIUS_KM * angularDistance;
	}

	private record NearbyAgentCandidate(DeliveryAgent agent, double distanceKm) {
	}

	private AdminDeliveryAgentResponse toAdminResponse(DeliveryAgent agent) {
		VerificationStatus verificationStatus = agent.getVerificationStatus() == null
			? (agent.isVerified() ? VerificationStatus.VERIFIED : VerificationStatus.PENDING)
			: agent.getVerificationStatus();
		UserSummaryDto user = null;
		try {
			user = authServiceClient.getUserSummary("DELIVERY_PARTNER", agent.getUserId());
		} catch (RuntimeException ignored) {
			// Return the delivery agent record even if auth lookup is unavailable.
		}
		return new AdminDeliveryAgentResponse(
			agent.getAgentId(),
			agent.getUserId(),
			agent.getFullName(),
			user != null ? user.email() : null,
			agent.getPhone(),
			agent.getVehicleType(),
			agent.getVehicleNumber(),
			agent.isAvailable(),
			agent.isVerified(),
			verificationStatus.name(),
			agent.getSubmittedAt(),
			agent.getRejectionReason(),
			agent.getReviewedByAdminId(),
			agent.getReviewedAt()
		);
	}
}
