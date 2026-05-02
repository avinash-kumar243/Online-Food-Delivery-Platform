package com.quickbite.delivery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.quickbite.delivery.dto.NearbyAgentResponse;
import com.quickbite.delivery.dto.OrderSnapshotDto;
import com.quickbite.delivery.dto.OrderAssignmentRequest;
import com.quickbite.delivery.dto.RegisterDeliveryAgentRequest;
import com.quickbite.delivery.entity.DeliveryAgent;
import com.quickbite.delivery.entity.VerificationStatus;
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
	void registerAgentInitializesNewAgentState() {
		RegisterDeliveryAgentRequest request = new RegisterDeliveryAgentRequest(
			101L,
			"Arun Kumar",
			"+91 9876543210",
			"Bike",
			"KA-01-AB-1234",
			null,
			null
		);

		when(deliveryRepository.findByUserId(101L)).thenReturn(Optional.empty());
		when(deliveryRepository.existsByVehicleNumberIgnoreCase("KA-01-AB-1234")).thenReturn(false);
		when(deliveryRepository.save(any(DeliveryAgent.class))).thenAnswer(invocation -> {
			DeliveryAgent agent = invocation.getArgument(0);
			agent.setAgentId(1L);
			return agent;
		});

		var response = deliveryService.registerAgent(request);

		assertEquals(1L, response.agentId());
		assertFalse(response.isAvailable());
		assertFalse(response.isVerified());
		assertEquals(0.0, response.avgRating());
		assertEquals(0, response.totalDeliveries());
		assertNull(response.activeOrderId());
		assertEquals(0.0, response.currentLatitude());
		assertEquals(0.0, response.currentLongitude());
	}

	@Test
	void getNearbyAgentsReturnsOnlyVerifiedAvailableAgentsWithinRadiusSortedByDistance() {
		DeliveryAgent nearestAgent = DeliveryAgent.builder()
			.agentId(1L)
			.userId(201L)
			.fullName("Nearest Agent")
			.phone("9999999999")
			.vehicleType("Bike")
			.vehicleNumber("KA-01-NEAR")
			.currentLatitude(12.9721)
			.currentLongitude(77.5933)
			.available(true)
			.verified(true)
			.verificationStatus(VerificationStatus.VERIFIED)
			.avgRating(4.7)
			.totalDeliveries(120)
			.build();

		DeliveryAgent fartherAgent = DeliveryAgent.builder()
			.agentId(2L)
			.userId(202L)
			.fullName("Far Agent")
			.phone("8888888888")
			.vehicleType("Scooter")
			.vehicleNumber("KA-01-FAR")
			.currentLatitude(12.9850)
			.currentLongitude(77.6050)
			.available(true)
			.verified(true)
			.verificationStatus(VerificationStatus.VERIFIED)
			.avgRating(4.5)
			.totalDeliveries(95)
			.build();

		DeliveryAgent unverifiedAgent = DeliveryAgent.builder()
			.agentId(3L)
			.userId(203L)
			.fullName("Unverified Agent")
			.phone("7777777777")
			.vehicleType("Bike")
			.vehicleNumber("KA-01-UNVERIFIED")
			.currentLatitude(12.9730)
			.currentLongitude(77.5940)
			.available(true)
			.verified(false)
			.verificationStatus(VerificationStatus.PENDING)
			.build();

		when(deliveryRepository.findByIsAvailableTrue())
			.thenReturn(List.of(fartherAgent, unverifiedAgent, nearestAgent));

		List<NearbyAgentResponse> nearbyAgents = deliveryService.getNearbyAgents(12.9716, 77.5946, 3.0);

		assertEquals(2, nearbyAgents.size());
		assertEquals(1L, nearbyAgents.get(0).agentId());
		assertEquals(2L, nearbyAgents.get(1).agentId());
		assertTrue(nearbyAgents.get(0).distanceKm() <= nearbyAgents.get(1).distanceKm());
	}

	@Test
	void assignOrderMarksAgentAsBusy() {
		DeliveryAgent agent = DeliveryAgent.builder()
			.agentId(10L)
			.userId(301L)
			.fullName("Active Agent")
			.phone("6666666666")
			.vehicleType("Bike")
			.vehicleNumber("KA-01-ACTIVE")
			.currentLatitude(12.9716)
			.currentLongitude(77.5946)
			.available(true)
			.verified(true)
			.verificationStatus(VerificationStatus.VERIFIED)
			.avgRating(4.8)
			.totalDeliveries(50)
			.build();

		when(deliveryRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(agent));
		when(deliveryRepository.save(agent)).thenReturn(agent);

		var response = deliveryService.assignOrder(new OrderAssignmentRequest(10L, 9001L));

		assertFalse(response.isAvailable());
		assertEquals(9001L, response.activeOrderId());
		verify(deliveryRepository).save(agent);
	}

	@Test
	void acceptOrderShouldClaimReadyOrderForFirstOnlineAgent() {
		DeliveryAgent agent = DeliveryAgent.builder()
			.agentId(11L)
			.userId(302L)
			.fullName("Claiming Agent")
			.phone("5555555555")
			.vehicleType("Bike")
			.vehicleNumber("KA-01-CLAIM")
			.currentLatitude(12.9716)
			.currentLongitude(77.5946)
			.available(true)
			.verified(true)
			.verificationStatus(VerificationStatus.VERIFIED)
			.avgRating(4.9)
			.totalDeliveries(20)
			.build();

		when(deliveryRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(agent));
		when(orderServiceClient.assignAgent(7001L, new com.quickbite.delivery.dto.AssignOrderRequestDto(11L)))
			.thenReturn(new OrderSnapshotDto(7001L, 21L, 31L, 11L));
		when(deliveryRepository.save(agent)).thenReturn(agent);

		var response = deliveryService.acceptOrder(11L, 7001L);

		assertFalse(response.isAvailable());
		assertEquals(7001L, response.activeOrderId());
		verify(deliveryRepository).save(agent);
	}
}
