package com.quickbite.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.review.client.dto.AgentDto;

@FeignClient(
        name = "${quickbite.clients.agent-service.name:agent-service}",
        path = "${quickbite.clients.agent-service.path:/agents}")
public interface AgentClient {

    @GetMapping("/{agentId}")
    AgentDto getAgentById(@PathVariable("agentId") Long agentId);
}
