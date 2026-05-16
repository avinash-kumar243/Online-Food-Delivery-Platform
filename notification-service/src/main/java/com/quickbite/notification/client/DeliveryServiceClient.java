package com.quickbite.notification.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.notification.client.dto.DeliveryAgentResponseDto;

@FeignClient(name = "DELIVERY-SERVICE", path = "/api/v1/agents")
public interface DeliveryServiceClient {

    @GetMapping("/available")
    List<DeliveryAgentResponseDto> getAvailableAgents();

    @GetMapping("/{agentId}")
    DeliveryAgentResponseDto getAgentById(@PathVariable("agentId") Long agentId);
}
