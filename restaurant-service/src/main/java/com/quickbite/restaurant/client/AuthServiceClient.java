package com.quickbite.restaurant.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quickbite.restaurant.dto.UserSummaryDto;

@FeignClient(name = "auth-service", path = "/api/v1/internal/users")
public interface AuthServiceClient {

    @GetMapping("/summary")
    UserSummaryDto getUserSummary(@RequestParam("role") String role, @RequestParam("userId") Long userId);
}
