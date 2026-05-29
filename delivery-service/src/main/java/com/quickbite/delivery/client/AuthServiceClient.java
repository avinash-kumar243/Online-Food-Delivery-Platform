package com.quickbite.delivery.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quickbite.delivery.dto.UserSummaryDto;

@FeignClient(name = "auth-service", path = "/api/v1/internal/users")
public interface AuthServiceClient {

    @GetMapping("/summary")
    UserSummaryDto getUserSummary(@RequestParam("role") String role, @RequestParam("userId") Long userId);
}
