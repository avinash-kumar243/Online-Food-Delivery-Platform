package com.quickbite.cartservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.cartservice.dto.MenuItemSnapshotDto;

@FeignClient(name = "menu-service", path = "/menu")
public interface MenuServiceClient {

    @GetMapping("/item/{itemId}")
    MenuItemSnapshotDto getMenuItem(@PathVariable("itemId") Integer itemId);
}
