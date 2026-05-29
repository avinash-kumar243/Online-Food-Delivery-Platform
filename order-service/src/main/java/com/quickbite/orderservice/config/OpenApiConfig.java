package com.quickbite.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenApi() {
        return new OpenAPI().info(new Info()
            .title("QuickBite Order Service API")
            .version("v1")
            .description("Central orchestration service for QuickBite order lifecycle management. "
                + "Prepared for future Cart-Service and Payment-Service integrations."));
    }
}
