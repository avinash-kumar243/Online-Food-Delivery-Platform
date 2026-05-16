package com.quickbite.delivery.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.quickbite.delivery.entity.DeliveryAgent;
import com.quickbite.delivery.entity.VerificationStatus;

@DataJpaTest
class DeliveryRepositoryTest {

	@Autowired
	private DeliveryRepository deliveryRepository;

	private DeliveryAgent availableVerifiedAgent;
	private DeliveryAgent busyVerifiedAgent;
	private DeliveryAgent pendingAgent;

	@BeforeEach
	void setUp() {
		availableVerifiedAgent = deliveryRepository.save(agent(101L, "KA-01-AB-1234", true, true, VerificationStatus.VERIFIED, null));
		busyVerifiedAgent = deliveryRepository.save(agent(102L, "KA-01-AB-5678", true, true, VerificationStatus.VERIFIED, 7001L));
		pendingAgent = deliveryRepository.save(agent(103L, "KA-01-AB-9999", false, false, VerificationStatus.PENDING, null));
	}

	@Test
	void queryMethods_ReturnExpectedAgents() {
		assertThat(deliveryRepository.findByUserId(101L)).isPresent();
		assertThat(deliveryRepository.findByActiveOrderId(7001L)).isPresent().get().extracting(DeliveryAgent::getAgentId)
			.isEqualTo(busyVerifiedAgent.getAgentId());
		assertThat(deliveryRepository.findByIsAvailableTrue()).extracting(DeliveryAgent::getAgentId)
			.containsExactlyInAnyOrder(availableVerifiedAgent.getAgentId(), busyVerifiedAgent.getAgentId());
		assertThat(deliveryRepository.findByIsVerifiedTrue()).extracting(DeliveryAgent::getAgentId)
			.containsExactlyInAnyOrder(availableVerifiedAgent.getAgentId(), busyVerifiedAgent.getAgentId());
		assertThat(deliveryRepository.countByIsAvailable(true)).isEqualTo(2L);
		assertThat(deliveryRepository.existsByVehicleNumberIgnoreCase("ka-01-ab-1234")).isTrue();
		assertThat(deliveryRepository.existsByVehicleNumberIgnoreCaseAndAgentIdNot("KA-01-AB-1234", busyVerifiedAgent.getAgentId())).isTrue();
		assertThat(deliveryRepository.findByActiveOrderIdIsNotNull()).extracting(DeliveryAgent::getAgentId)
			.containsExactly(busyVerifiedAgent.getAgentId());
		assertThat(deliveryRepository.findByVerificationStatus(VerificationStatus.PENDING)).extracting(DeliveryAgent::getAgentId)
			.containsExactly(pendingAgent.getAgentId());
		assertThat(deliveryRepository.findByIdForUpdate(availableVerifiedAgent.getAgentId())).isPresent();
	}

	@Test
	void updateLocation_UpdatesCoordinates() {
		int updatedRows = deliveryRepository.updateLocation(availableVerifiedAgent.getAgentId(), 13.05, 77.61);

		assertThat(updatedRows).isEqualTo(1);
		DeliveryAgent updated = deliveryRepository.findById(availableVerifiedAgent.getAgentId()).orElseThrow();
		assertThat(updated.getCurrentLatitude()).isEqualTo(13.05);
		assertThat(updated.getCurrentLongitude()).isEqualTo(77.61);
	}

	private DeliveryAgent agent(Long userId, String vehicleNumber, boolean available, boolean verified,
			VerificationStatus verificationStatus, Long activeOrderId) {
		return DeliveryAgent.builder()
			.userId(userId)
			.fullName("Agent " + userId)
			.phone("9999999999")
			.vehicleType("Bike")
			.vehicleNumber(vehicleNumber)
			.currentLatitude(12.9716)
			.currentLongitude(77.5946)
			.available(available)
			.verified(verified)
			.verificationStatus(verificationStatus)
			.avgRating(4.5)
			.totalDeliveries(12)
			.activeOrderId(activeOrderId)
			.rejectionReason(null)
			.reviewedByAdminId(null)
			.reviewedAt(null)
			.submittedAt(LocalDateTime.now())
			.build();
	}
}
