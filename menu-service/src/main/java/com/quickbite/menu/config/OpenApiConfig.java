package com.quickbite.menu.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI menuOpenApi() {
        return new OpenAPI().info(
            new Info()
                .title("QuickBite Menu Service API")
                .description("Menu management service for categories, items, pricing, and availability.")
                .version("v1")
        );
    }

    @Bean
    public GroupedOpenApi menuApi() {
        return GroupedOpenApi.builder()
            .group("menu-service")
            .pathsToMatch("/menu/**")
            .build();
    }
}
