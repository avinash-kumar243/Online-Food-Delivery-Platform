package com.quickbite.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.orderservice.client.dto.DeliveryAgentRealtimeDto;

@FeignClient(name = "delivery-service", path = "/api/v1/agents")
public interface DeliveryAgentClient {

    @GetMapping("/{agentId}")
    DeliveryAgentRealtimeDto getAgentById(@PathVariable Long agentId);
}
