package com.quickbite.notification.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi notificationApiGroup() {
        return GroupedOpenApi.builder()
                .group("notification-service")
                .packagesToScan("com.quickbite.notification.resource")
                .build();
    }

    @Bean
    public OpenAPI notificationOpenApi() {
        return new OpenAPI().info(new Info()
                .title("QuickBite Notification Service")
                .version("v1")
                .description("Notification APIs for order alerts, delivery events, and promotional broadcasts.")
                .contact(new Contact().name("QuickBite Platform")));
    }
}
