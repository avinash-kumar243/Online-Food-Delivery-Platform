package com.quickbite.delivery.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(
	info = @Info(
		title = "QuickBite Delivery Service API",
		version = "1.0.0",
		description = "APIs for delivery agent registration, tracking, and order assignment."
	)
)
public class OpenApiConfig {

	@Bean
	GroupedOpenApi deliveryAgentsOpenApi() {
		return GroupedOpenApi.builder()
			.group("delivery-agents")
			.packagesToScan("com.quickbite.delivery.resource")
			.build();
	}
}
