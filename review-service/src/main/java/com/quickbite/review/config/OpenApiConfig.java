package com.quickbite.review.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi reviewApiGroup() {
        return GroupedOpenApi.builder()
                .group("review-service")
                .packagesToScan("com.quickbite.review.resource")
                .build();
    }

    @Bean
    public OpenAPI reviewOpenApi() {
        return new OpenAPI().info(new Info()
                .title("QuickBite Review Service")
                .version("v1")
                .description("Dual-rating review APIs for QuickBite orders.")
                .contact(new Contact().name("QuickBite Platform")));
    }
}
