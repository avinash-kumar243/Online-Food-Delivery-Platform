package com.quickbite.notification.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.quickbite.notification.client.dto.InternalUserSummaryDto;

@FeignClient(name = "AUTH-SERVICE", path = "/api/v1/internal/users")
public interface AuthServiceClient {

    @GetMapping("/summary")
    InternalUserSummaryDto getUserSummary(@RequestParam("role") String role, @RequestParam("userId") Long userId);

    @GetMapping("/role")
    List<InternalUserSummaryDto> getUsersByRole(@RequestParam("role") String role);
}
