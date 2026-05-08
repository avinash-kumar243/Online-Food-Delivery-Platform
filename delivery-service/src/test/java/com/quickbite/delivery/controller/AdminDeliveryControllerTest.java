package com.quickbite.delivery.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.delivery.dto.AdminDeliveryAgentResponse;
import com.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.delivery.exception.GlobalExceptionHandler;
import com.quickbite.delivery.service.DeliveryService;

@ExtendWith(MockitoExtension.class)
class AdminDeliveryControllerTest {

	@Mock
	private DeliveryService deliveryService;

	private MockMvc mockMvc;
	private AdminDeliveryAgentResponse adminResponse;
	private DeliveryAgentResponse agentResponse;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders.standaloneSetup(new AdminDeliveryController(deliveryService))
			.setControllerAdvice(new GlobalExceptionHandler())
			.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
			.setValidator(validator)
			.build();

		adminResponse = new AdminDeliveryAgentResponse(
			1L, 101L, "Arun Kumar", "arun@example.com", "9999999999", "Bike", "KA-01-AB-1234",
			false, false, "PENDING", LocalDateTime.now().minusDays(1), null, null, null
		);
		agentResponse = new DeliveryAgentResponse(
			1L, 101L, "Arun Kumar", "9999999999", "Bike", "KA-01-AB-1234",
			12.9716, 77.5946, false, true, "VERIFIED", 4.8, 25, null,
			null, 700L, LocalDateTime.now(), LocalDateTime.now().minusDays(1)
		);
	}

	@Test
	void getPendingAgents_ReturnsOk() throws Exception {
		when(deliveryService.getPendingAgents()).thenReturn(List.of(adminResponse));

		mockMvc.perform(get("/api/v1/admin/agents/pending"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].agentId").value(1));
	}

	@Test
	void getAllAgents_ReturnsOk() throws Exception {
		when(deliveryService.getAllAgentsForAdmin()).thenReturn(List.of(adminResponse));

		mockMvc.perform(get("/api/v1/admin/agents/all"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].userId").value(101));
	}

	@Test
	void verifyAgent_ReturnsOk() throws Exception {
		when(deliveryService.verifyAgent(1L, 700L)).thenReturn(agentResponse);

		mockMvc.perform(put("/api/v1/admin/agents/1/verify")
				.contentType("application/json")
				.content("""
					{"adminId":700}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.reviewedByAdminId").value(700));
	}

	@Test
	void rejectAgent_ReturnsOk() throws Exception {
		when(deliveryService.rejectAgent(1L, 700L, "Missing documents")).thenReturn(agentResponse);

		mockMvc.perform(put("/api/v1/admin/agents/1/reject")
				.contentType("application/json")
				.content("""
					{"adminId":700,"feedback":"Missing documents"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.agentId").value(1));
	}
}
