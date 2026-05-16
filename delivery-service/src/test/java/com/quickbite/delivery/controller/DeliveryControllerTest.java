package com.quickbite.delivery.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import com.quickbite.delivery.dto.AvailabilityUpdateResponse;
import com.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.delivery.dto.LocationUpdateResponse;
import com.quickbite.delivery.dto.NearbyAgentResponse;
import com.quickbite.delivery.exception.ConflictException;
import com.quickbite.delivery.exception.GlobalExceptionHandler;
import com.quickbite.delivery.exception.ResourceNotFoundException;
import com.quickbite.delivery.service.DeliveryService;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

	@Mock
	private DeliveryService deliveryService;

	private MockMvc mockMvc;
	private DeliveryAgentResponse agentResponse;

	@BeforeEach
	void setUp() {
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();

		mockMvc = MockMvcBuilders.standaloneSetup(new DeliveryController(deliveryService))
			.setControllerAdvice(new GlobalExceptionHandler())
			.setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
			.setValidator(validator)
			.build();

		agentResponse = new DeliveryAgentResponse(
			1L, 101L, "Arun Kumar", "9999999999", "Bike", "KA-01-AB-1234",
			12.9716, 77.5946, true, true, "VERIFIED", 4.8, 25, null,
			null, 700L, LocalDateTime.now(), LocalDateTime.now().minusDays(1)
		);
	}

	@Test
	void registerAgent_ReturnsCreated() throws Exception {
		when(deliveryService.registerAgent(any())).thenReturn(agentResponse);

		mockMvc.perform(post("/api/v1/agents/register")
				.contentType("application/json")
				.content("""
					{"userId":101,"fullName":"Arun Kumar","phone":"9999999999","vehicleType":"Bike","vehicleNumber":"KA-01-AB-1234","currentLatitude":12.9716,"currentLongitude":77.5946}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/agents/1")))
			.andExpect(jsonPath("$.agentId").value(1));
	}

	@Test
	void registerAgent_WhenValidationFails_ReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/agents/register")
				.contentType("application/json")
				.content("""
					{"userId":101,"fullName":"","phone":"9999999999","vehicleType":"Bike","vehicleNumber":"KA-01-AB-1234"}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.message").value("Input validation failed"))
			.andExpect(jsonPath("$.errors.fullName").value("Full name is required"));
	}

	@Test
	void getAgentById_WhenDeliveryDetailsNotFound_Returns404WithProperMessage() throws Exception {
		when(deliveryService.getAgentById(999L))
			.thenThrow(new ResourceNotFoundException("Delivery agent not found with id 999"));

		mockMvc.perform(get("/api/v1/agents/999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.error").value("Not Found"))
			.andExpect(jsonPath("$.message").value("Delivery agent not found with id 999"));
	}

	@Test
	void getAgentByUserId_ReturnsOk() throws Exception {
		when(deliveryService.getAgentByUserId(101L)).thenReturn(agentResponse);

		mockMvc.perform(get("/api/v1/agents/user/101"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.userId").value(101));
	}

	@Test
	void getNearbyAgents_ReturnsOk() throws Exception {
		when(deliveryService.getNearbyAgents(12.97, 77.59, 5.0)).thenReturn(List.of(
			new NearbyAgentResponse(1L, 101L, "Arun Kumar", "9999999999", "Bike", "KA-01-AB-1234",
				12.9716, 77.5946, true, true, 4.8, 25, null, 0.4)
		));

		mockMvc.perform(get("/api/v1/agents/nearby")
				.param("latitude", "12.97")
				.param("longitude", "77.59")
				.param("radius", "5.0"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].agentId").value(1));
	}

	@Test
	void updateLocation_ReturnsOk() throws Exception {
		when(deliveryService.updateLocation(1L, new com.quickbite.delivery.dto.LocationUpdateRequest(12.98, 77.60)))
			.thenReturn(new LocationUpdateResponse(1L, 12.98, 77.60));

		mockMvc.perform(put("/api/v1/agents/1/location")
				.contentType("application/json")
				.content("""
					{"latitude":12.98,"longitude":77.60}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.agentId").value(1));
	}

	@Test
	void updateAvailability_WhenConflict_Returns409() throws Exception {
		when(deliveryService.setAvailability(1L, true))
			.thenThrow(new ConflictException("Only verified delivery partners can go online"));

		mockMvc.perform(put("/api/v1/agents/1/availability")
				.contentType("application/json")
				.content("""
					{"available":true}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409))
			.andExpect(jsonPath("$.message").value("Only verified delivery partners can go online"));
	}

	@Test
	void updateAvailabilityViaQuery_ReturnsOk() throws Exception {
		when(deliveryService.setAvailability(1L, false)).thenReturn(new AvailabilityUpdateResponse(1L, false, null));

		mockMvc.perform(put("/api/v1/agents/1/availability").param("available", "false"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.agentId").value(1))
			.andExpect(jsonPath("$.activeOrderId").doesNotExist());
	}

	@Test
	void verifyAgent_ReturnsOk() throws Exception {
		when(deliveryService.verifyAgent(1L, null)).thenReturn(agentResponse);

		mockMvc.perform(patch("/api/v1/agents/1/verify"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.agentId").value(1));
	}

	@Test
	void assignOrder_WhenValidationFails_ReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/agents/assign")
				.contentType("application/json")
				.content("""
					{"agentId":1,"orderId":0}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Input validation failed"))
			.andExpect(jsonPath("$.errors.orderId").value("Order id must be positive"));
	}

	@Test
	void acceptOrder_ReturnsOk() throws Exception {
		when(deliveryService.acceptOrder(1L, 7001L)).thenReturn(agentResponse);

		mockMvc.perform(post("/api/v1/agents/1/accept-order/7001"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.agentId").value(1));
	}

	@Test
	void completeDelivery_ReturnsOk() throws Exception {
		when(deliveryService.completeDelivery(1L)).thenReturn(agentResponse);

		mockMvc.perform(post("/api/v1/agents/1/complete-delivery"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.agentId").value(1));
	}
}
