package com.quickbite.delivery.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.quickbite.delivery.dto.AdminDeliveryAgentResponse;
import com.quickbite.delivery.dto.AdminDeliveryDecisionRequest;
import com.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.delivery.service.DeliveryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/agents")
public class AdminDeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping("/pending")
    public ResponseEntity<List<AdminDeliveryAgentResponse>> getPendingAgents() {
        return ResponseEntity.ok(deliveryService.getPendingAgents());
    }

    @GetMapping("/all")
    public ResponseEntity<List<AdminDeliveryAgentResponse>> getAllAgents() {
        return ResponseEntity.ok(deliveryService.getAllAgentsForAdmin());
    }

    @PutMapping("/{agentId}/verify")
    public ResponseEntity<DeliveryAgentResponse> verifyAgent(
        @PathVariable Long agentId,
        @Valid @RequestBody AdminDeliveryDecisionRequest request
    ) {
        return ResponseEntity.ok(deliveryService.verifyAgent(agentId, request.adminId()));
    }

    @PutMapping("/{agentId}/reject")
    public ResponseEntity<DeliveryAgentResponse> rejectAgent(
        @PathVariable Long agentId,
        @Valid @RequestBody AdminDeliveryDecisionRequest request
    ) {
        return ResponseEntity.ok(deliveryService.rejectAgent(agentId, request.adminId(), request.feedback()));
    }
}
