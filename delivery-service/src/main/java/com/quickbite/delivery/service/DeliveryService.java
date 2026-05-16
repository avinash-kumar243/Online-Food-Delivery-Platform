package com.quickbite.delivery.service;

import java.util.List;

import com.quickbite.delivery.dto.AdminDeliveryAgentResponse;
import com.quickbite.delivery.dto.AvailabilityUpdateResponse;
import com.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.delivery.dto.LocationUpdateRequest;
import com.quickbite.delivery.dto.LocationUpdateResponse;
import com.quickbite.delivery.dto.NearbyAgentResponse;
import com.quickbite.delivery.dto.OrderAssignmentRequest;
import com.quickbite.delivery.dto.RegisterDeliveryAgentRequest;

public interface DeliveryService {

	DeliveryAgentResponse registerAgent(RegisterDeliveryAgentRequest request);

	DeliveryAgentResponse getAgentById(Long agentId);

	DeliveryAgentResponse getAgentByUserId(Long userId);

	List<NearbyAgentResponse> getNearbyAgents(double latitude, double longitude, double radiusInKm);

	List<DeliveryAgentResponse> getAvailableAgents();

	LocationUpdateResponse updateLocation(Long agentId, LocationUpdateRequest request);

	AvailabilityUpdateResponse setAvailability(Long agentId, boolean available);

	List<AdminDeliveryAgentResponse> getPendingAgents();

	List<AdminDeliveryAgentResponse> getAllAgentsForAdmin();

	DeliveryAgentResponse verifyAgent(Long agentId, Long adminId);

	DeliveryAgentResponse rejectAgent(Long agentId, Long adminId, String feedback);

	DeliveryAgentResponse assignOrder(OrderAssignmentRequest request);

	DeliveryAgentResponse acceptOrder(Long agentId, Long orderId);

	DeliveryAgentResponse completeDelivery(Long agentId);

	List<DeliveryAgentResponse> getActiveAgents();
}
