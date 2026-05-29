package com.quickbite.orderservice.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.orderservice.dto.OrderItemResponse;
import com.quickbite.orderservice.dto.OrderResponse;
import com.quickbite.orderservice.entity.OrderStatus;
import com.quickbite.orderservice.service.OrderService;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminOrderController(orderService))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();
    }

    @Test
    void getAllOrders_ReturnsOk() throws Exception {
        OrderResponse response = new OrderResponse(
            1L,
            10L,
            20L,
            null,
            new BigDecimal("300.00"),
            BigDecimal.ZERO,
            new BigDecimal("300.00"),
            "CARD",
            "PENDING",
            OrderStatus.PLACED,
            OffsetDateTime.now(),
            OffsetDateTime.now().plusMinutes(45),
            "221B Baker Street",
            null,
            List.of(new OrderItemResponse(11L, 101L, "Burger", new BigDecimal("120.00"), 2, null, new BigDecimal("240.00"))),
            null,
            null
        );
        when(orderService.getAllOrders()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/admin/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].orderId").value(1));
    }
}
