package com.quickbite.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quickbite.payment.client.dto.InternalUserSummaryDto;

@FeignClient(name = "auth-service", path = "/api/v1/internal/users")
public interface AuthServiceClient {

    @GetMapping("/summary")
    InternalUserSummaryDto getUserSummary(@RequestParam("role") String role, @RequestParam("userId") Long userId);
}
