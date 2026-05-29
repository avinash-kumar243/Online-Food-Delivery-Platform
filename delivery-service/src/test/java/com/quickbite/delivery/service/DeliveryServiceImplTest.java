package com.quickbite.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickbite.delivery.client.AuthServiceClient;
import com.quickbite.delivery.client.OrderServiceClient;
import com.quickbite.delivery.dto.LocationUpdateRequest;
import com.quickbite.delivery.dto.NearbyAgentResponse;
import com.quickbite.delivery.dto.OrderAssignmentRequest;
import com.quickbite.delivery.dto.OrderSnapshotDto;
import com.quickbite.delivery.dto.RegisterDeliveryAgentRequest;
import com.quickbite.delivery.dto.UserSummaryDto;
import com.quickbite.delivery.entity.DeliveryAgent;
import com.quickbite.delivery.entity.VerificationStatus;
import com.quickbite.delivery.exception.BadRequestException;
import com.quickbite.delivery.exception.ConflictException;
import com.quickbite.delivery.exception.ResourceNotFoundException;
import com.quickbite.delivery.messaging.GenericEventPublisher;
import com.quickbite.delivery.repository.DeliveryRepository;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

	@Mock
	private DeliveryRepository deliveryRepository;

	@Mock
	private AuthServiceClient authServiceClient;

	@Mock
	private OrderServiceClient orderServiceClient;

	@Mock
	private GenericEventPublisher eventPublisher;

	@InjectMocks
	private DeliveryServiceImpl deliveryService;

	@Test
	void registerAgent_InitializesNewAgentState() {
		RegisterDeliveryAgentRequest request = registerRequest(101L, "KA-01-AB-1234", null, null);

		when(deliveryRepository.findByUserId(101L)).thenReturn(Optional.empty());
		when(deliveryRepository.existsByVehicleNumberIgnoreCase("KA-01-AB-1234")).thenReturn(false);
		when(deliveryRepository.save(any(DeliveryAgent.class))).thenAnswer(invocation -> {
			DeliveryAgent agent = invocation.getArgument(0);
			agent.setAgentId(1L);
			return agent;
		});

		var response = deliveryService.registerAgent(request);

		assertThat(response.agentId()).isEqualTo(1L);
		assertThat(response.isAvailable()).isFalse();
		assertThat(response.isVerified()).isFalse();
		assertThat(response.verificationStatus()).isEqualTo("PENDING");
		assertThat(response.currentLatitude()).isEqualTo(0.0);
		assertThat(response.currentLongitude()).isEqualTo(0.0);
	}

	@Test
	void registerAgent_WhenOnlyOneCoordinatePresent_ThrowsBadRequest() {
		RegisterDeliveryAgentRequest request = registerRequest(101L, "KA-01-AB-1234", 12.97, null);

		assertThatThrownBy(() -> deliveryService.registerAgent(request))
			.isInstanceOf(BadRequestException.class)
			.hasMessage("Both latitude and longitude must be provided together during registration");
	}

	@Test
	void registerAgent_WhenVehicleNumberExists_ThrowsConflict() {
		RegisterDeliveryAgentRequest request = registerRequest(101L, "KA-01-AB-1234", null, null);

		when(deliveryRepository.findByUserId(101L)).thenReturn(Optional.empty());
		when(deliveryRepository.existsByVehicleNumberIgnoreCase("KA-01-AB-1234")).thenReturn(true);

		assertThatThrownBy(() -> deliveryService.registerAgent(request))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("Vehicle number is already registered");
	}

	@Test
	void getAgentById_WhenMissing_ThrowsResourceNotFoundException() {
		when(deliveryRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> deliveryService.getAgentById(404L))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Delivery agent not found with id 404");
	}

	@Test
	void getAgentByUserId_WhenMissing_ThrowsResourceNotFoundException() {
		when(deliveryRepository.findByUserId(909L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> deliveryService.getAgentByUserId(909L))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Delivery agent not found for userId 909");
	}

	@Test
	void getNearbyAgents_ReturnsOnlyVerifiedAvailableAgentsWithinRadiusSortedByDistance() {
		DeliveryAgent nearestAgent = verifiedAgent(1L, 201L, true, null, 12.9721, 77.5933);
		DeliveryAgent fartherAgent = verifiedAgent(2L, 202L, true, null, 12.9850, 77.6050);
		DeliveryAgent unverifiedAgent = pendingAgent(3L, 203L, true, null);

		when(deliveryRepository.findByIsAvailableTrue()).thenReturn(List.of(fartherAgent, unverifiedAgent, nearestAgent));

		List<NearbyAgentResponse> nearbyAgents = deliveryService.getNearbyAgents(12.9716, 77.5946, 3.0);

		assertThat(nearbyAgents).hasSize(2);
		assertThat(nearbyAgents.get(0).agentId()).isEqualTo(1L);
		assertThat(nearbyAgents.get(1).agentId()).isEqualTo(2L);
		assertThat(nearbyAgents.get(0).distanceKm()).isLessThanOrEqualTo(nearbyAgents.get(1).distanceKm());
	}

	@Test
	void getAvailableAgents_FiltersOutUnverifiedAndBusyAgents() {
		DeliveryAgent freeVerified = verifiedAgent(1L, 201L, true, null, 12.97, 77.59);
		DeliveryAgent busyVerified = verifiedAgent(2L, 202L, true, 5001L, 12.97, 77.59);
		DeliveryAgent freePending = pendingAgent(3L, 203L, true, null);

		when(deliveryRepository.findByIsAvailableTrue()).thenReturn(List.of(freeVerified, busyVerified, freePending));

		var result = deliveryService.getAvailableAgents();

		assertThat(result).hasSize(1);
		assertThat(result.get(0).agentId()).isEqualTo(1L);
	}

	@Test
	void updateLocation_WhenAgentMissing_ThrowsResourceNotFoundException() {
		when(deliveryRepository.updateLocation(99L, 12.9, 77.6)).thenReturn(0);

		assertThatThrownBy(() -> deliveryService.updateLocation(99L, new LocationUpdateRequest(12.9, 77.6)))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Delivery agent not found with id 99");
	}

	@Test
	void setAvailability_WhenUnverified_ThrowsConflict() {
		DeliveryAgent agent = pendingAgent(11L, 301L, false, null);
		when(deliveryRepository.findById(11L)).thenReturn(Optional.of(agent));

		assertThatThrownBy(() -> deliveryService.setAvailability(11L, true))
			.isInstanceOf(ConflictException.class)
			.hasMessage("Only verified delivery partners can go online");
	}

	@Test
	void setAvailability_WhenBusy_ThrowsConflict() {
		DeliveryAgent agent = verifiedAgent(12L, 302L, false, 6001L, 12.97, 77.59);
		when(deliveryRepository.findById(12L)).thenReturn(Optional.of(agent));

		assertThatThrownBy(() -> deliveryService.setAvailability(12L, true))
			.isInstanceOf(ConflictException.class)
			.hasMessage("Agent cannot be marked available while an active delivery is assigned");
	}

	@Test
	void setAvailability_WhenValid_UpdatesAgent() {
		DeliveryAgent agent = verifiedAgent(13L, 303L, false, null, 12.97, 77.59);
		when(deliveryRepository.findById(13L)).thenReturn(Optional.of(agent));

		var response = deliveryService.setAvailability(13L, true);

		assertThat(response.isAvailable()).isTrue();
		verify(deliveryRepository).save(agent);
	}

	@Test
	void getPendingAgents_ReturnsAdminViewEvenWhenAuthLookupFails() {
		DeliveryAgent pending = pendingAgent(21L, 401L, false, null);
		when(deliveryRepository.findByVerificationStatus(VerificationStatus.PENDING)).thenReturn(List.of(pending));
		when(authServiceClient.getUserSummary("DELIVERY_PARTNER", 401L)).thenThrow(new RuntimeException("auth down"));

		var result = deliveryService.getPendingAgents();

		assertThat(result).singleElement().satisfies(agent -> {
			assertThat(agent.agentId()).isEqualTo(21L);
			assertThat(agent.email()).isNull();
			assertThat(agent.verificationStatus()).isEqualTo("PENDING");
		});
	}

	@Test
	void verifyAndRejectAgent_UpdateReviewFields() {
		DeliveryAgent pending = pendingAgent(31L, 501L, false, null);
		when(deliveryRepository.findById(31L)).thenReturn(Optional.of(pending));
		when(deliveryRepository.save(any(DeliveryAgent.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var verified = deliveryService.verifyAgent(31L, 700L);
		assertThat(verified.isVerified()).isTrue();
		assertThat(verified.verificationStatus()).isEqualTo("VERIFIED");
		assertThat(verified.reviewedByAdminId()).isEqualTo(700L);

		var rejected = deliveryService.rejectAgent(31L, 701L, "Missing documents");
		assertThat(rejected.isVerified()).isFalse();
		assertThat(rejected.isAvailable()).isFalse();
		assertThat(rejected.verificationStatus()).isEqualTo("REJECTED");
		assertThat(rejected.rejectionReason()).isEqualTo("Missing documents");
	}

	@Test
	void assignOrder_WhenBusy_ThrowsConflict() {
		DeliveryAgent agent = verifiedAgent(41L, 601L, true, 9000L, 12.97, 77.59);
		when(deliveryRepository.findByIdForUpdate(41L)).thenReturn(Optional.of(agent));

		assertThatThrownBy(() -> deliveryService.assignOrder(new OrderAssignmentRequest(41L, 9001L)))
			.isInstanceOf(ConflictException.class)
			.hasMessage("Delivery agent already has an active delivery");
	}

	@Test
	void assignOrder_WhenValid_MarksAgentBusyAndPublishesEvent() {
		DeliveryAgent agent = verifiedAgent(42L, 602L, true, null, 12.9716, 77.5946);
		when(deliveryRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(agent));
		when(deliveryRepository.save(agent)).thenReturn(agent);

		var response = deliveryService.assignOrder(new OrderAssignmentRequest(42L, 9001L));

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.activeOrderId()).isEqualTo(9001L);
		verify(deliveryRepository).save(agent);
		verify(eventPublisher).send(eq("delivery.assigned"), any());
	}

	@Test
	void acceptOrder_WhenUnavailable_ThrowsConflict() {
		DeliveryAgent agent = verifiedAgent(51L, 701L, false, null, 12.97, 77.59);
		when(deliveryRepository.findByIdForUpdate(51L)).thenReturn(Optional.of(agent));

		assertThatThrownBy(() -> deliveryService.acceptOrder(51L, 7001L))
			.isInstanceOf(ConflictException.class)
			.hasMessage("Delivery partner is currently unavailable");
	}

	@Test
	void acceptOrder_WhenValid_ClaimsOrderAndPublishesEvent() {
		DeliveryAgent agent = verifiedAgent(52L, 702L, true, null, 12.9716, 77.5946);
		when(deliveryRepository.findByIdForUpdate(52L)).thenReturn(Optional.of(agent));
		when(orderServiceClient.assignAgent(7001L, new com.quickbite.delivery.dto.AssignOrderRequestDto(52L)))
			.thenReturn(new OrderSnapshotDto(7001L, 21L, 31L, 52L));
		when(deliveryRepository.save(agent)).thenReturn(agent);

		var response = deliveryService.acceptOrder(52L, 7001L);

		assertThat(response.isAvailable()).isFalse();
		assertThat(response.activeOrderId()).isEqualTo(7001L);
		verify(eventPublisher).send(eq("delivery.assigned"), any());
	}

	@Test
	void completeDelivery_WhenNoActiveOrder_ThrowsConflict() {
		DeliveryAgent agent = verifiedAgent(61L, 801L, false, null, 12.97, 77.59);
		when(deliveryRepository.findByIdForUpdate(61L)).thenReturn(Optional.of(agent));

		assertThatThrownBy(() -> deliveryService.completeDelivery(61L))
			.isInstanceOf(ConflictException.class)
			.hasMessage("Delivery agent does not have an active delivery to complete");
	}

	@Test
	void completeDelivery_WhenValid_ClearsActiveOrderAndPublishesEvent() {
		DeliveryAgent agent = verifiedAgent(62L, 802L, false, 8001L, 12.97, 77.59);
		agent.setTotalDeliveries(5);
		when(deliveryRepository.findByIdForUpdate(62L)).thenReturn(Optional.of(agent));
		when(deliveryRepository.save(agent)).thenReturn(agent);

		var response = deliveryService.completeDelivery(62L);

		assertThat(response.activeOrderId()).isNull();
		assertThat(response.isAvailable()).isTrue();
		assertThat(response.totalDeliveries()).isEqualTo(6);
		verify(eventPublisher).send(eq("order.delivered"), any());
	}

	@Test
	void getActiveAgents_ReturnsMappedActiveAgents() {
		DeliveryAgent activeAgent = verifiedAgent(71L, 901L, false, 8101L, 12.97, 77.59);
		when(deliveryRepository.findByActiveOrderIdIsNotNull()).thenReturn(List.of(activeAgent));

		var result = deliveryService.getActiveAgents();

		assertThat(result).singleElement().satisfies(agent -> {
			assertThat(agent.agentId()).isEqualTo(71L);
			assertThat(agent.activeOrderId()).isEqualTo(8101L);
		});
	}

	private RegisterDeliveryAgentRequest registerRequest(Long userId, String vehicleNumber, Double latitude, Double longitude) {
		return new RegisterDeliveryAgentRequest(
			userId,
			"Arun Kumar",
			"+91 9876543210",
			"Bike",
			vehicleNumber,
			latitude,
			longitude
		);
	}

	private DeliveryAgent verifiedAgent(Long agentId, Long userId, boolean available, Long activeOrderId,
			double latitude, double longitude) {
		return DeliveryAgent.builder()
			.agentId(agentId)
			.userId(userId)
			.fullName("Verified Agent")
			.phone("9999999999")
			.vehicleType("Bike")
			.vehicleNumber("KA-01-" + agentId)
			.currentLatitude(latitude)
			.currentLongitude(longitude)
			.available(available)
			.verified(true)
			.verificationStatus(VerificationStatus.VERIFIED)
			.avgRating(4.7)
			.totalDeliveries(12)
			.activeOrderId(activeOrderId)
			.submittedAt(java.time.LocalDateTime.now())
			.build();
	}

	private DeliveryAgent pendingAgent(Long agentId, Long userId, boolean available, Long activeOrderId) {
		return DeliveryAgent.builder()
			.agentId(agentId)
			.userId(userId)
			.fullName("Pending Agent")
			.phone("8888888888")
			.vehicleType("Scooter")
			.vehicleNumber("KA-02-" + agentId)
			.currentLatitude(12.9716)
			.currentLongitude(77.5946)
			.available(available)
			.verified(false)
			.verificationStatus(VerificationStatus.PENDING)
			.avgRating(0.0)
			.totalDeliveries(0)
			.activeOrderId(activeOrderId)
			.submittedAt(java.time.LocalDateTime.now())
			.build();
	}
}
