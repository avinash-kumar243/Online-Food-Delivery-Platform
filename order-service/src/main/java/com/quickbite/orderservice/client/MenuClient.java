package com.quickbite.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.orderservice.client.dto.MenuItemSnapshotDto;

@FeignClient(name = "menu-service", path = "/api/v1/menu")
public interface MenuClient {

    @GetMapping("/item/{itemId}")
    MenuItemSnapshotDto getItemById(@PathVariable Integer itemId);
}
