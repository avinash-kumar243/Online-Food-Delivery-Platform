package com.quickbite.delivery.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.quickbite.delivery.entity.DeliveryAgent;
import com.quickbite.delivery.entity.VerificationStatus;

import jakarta.persistence.LockModeType;

public interface DeliveryRepository extends JpaRepository<DeliveryAgent, Long> {

	@Query("select agent from DeliveryAgent agent where agent.userId = :userId")
	Optional<DeliveryAgent> findByUserId(@Param("userId") Long userId);

	@Query("select agent from DeliveryAgent agent where agent.available = true")
	List<DeliveryAgent> findByIsAvailableTrue();

	@Query("select agent from DeliveryAgent agent where agent.verified = true")
	List<DeliveryAgent> findByIsVerifiedTrue();

	@Query("select count(agent) from DeliveryAgent agent where agent.available = :available")
	long countByIsAvailable(@Param("available") boolean available);

	boolean existsByVehicleNumberIgnoreCase(String vehicleNumber);

	boolean existsByVehicleNumberIgnoreCaseAndAgentIdNot(String vehicleNumber, Long agentId);

	List<DeliveryAgent> findByActiveOrderIdIsNotNull();

	List<DeliveryAgent> findByVerificationStatus(VerificationStatus verificationStatus);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select agent from DeliveryAgent agent where agent.agentId = :agentId")
	Optional<DeliveryAgent> findByIdForUpdate(@Param("agentId") Long agentId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		update DeliveryAgent agent
		set agent.currentLatitude = :latitude,
		    agent.currentLongitude = :longitude
		where agent.agentId = :agentId
	""")
	int updateLocation(
		@Param("agentId") Long agentId,
		@Param("latitude") double latitude,
		@Param("longitude") double longitude
	);
}
