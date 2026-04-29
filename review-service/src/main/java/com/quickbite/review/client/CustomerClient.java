package com.quickbite.review.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.quickbite.review.client.dto.CustomerDto;

@FeignClient(
        name = "${quickbite.clients.customer-service.name:customer-service}",
        path = "${quickbite.clients.customer-service.path:/customers}")
public interface CustomerClient {

    @GetMapping("/{customerId}")
    CustomerDto getCustomerById(@PathVariable("customerId") Long customerId);
}
