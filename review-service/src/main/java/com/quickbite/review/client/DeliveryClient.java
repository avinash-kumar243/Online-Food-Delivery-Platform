package com.quickbite.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.quickbite.review.client.dto.DeliveryRatingRequestDto;

@FeignClient(name = "delivery-service", path = "/api/v1/agents")
public interface DeliveryClient {

    @PatchMapping("/{agentId}/rating")
    void updateAverageRating(@PathVariable("agentId") Long agentId, @RequestBody DeliveryRatingRequestDto request);
}
