package com.quickbite.orderservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickbite.orderservice.dto.OrderItemResponse;
import com.quickbite.orderservice.dto.OrderResponse;
import com.quickbite.orderservice.entity.OrderStatus;
import com.quickbite.orderservice.exception.ConflictException;
import com.quickbite.orderservice.exception.GlobalExceptionHandler;
import com.quickbite.orderservice.exception.OrderNotFoundException;
import com.quickbite.orderservice.service.OrderService;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    private MockMvc mockMvc;
    private OrderResponse orderResponse;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new OrderController(orderService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();

        orderResponse = new OrderResponse(
            1L,
            10L,
            20L,
            null,
            new BigDecimal("300.00"),
            new BigDecimal("20.00"),
            new BigDecimal("295.00"),
            "CARD",
            "PENDING",
            OrderStatus.PLACED,
            OffsetDateTime.now(),
            OffsetDateTime.now().plusMinutes(45),
            "221B Baker Street",
            "No onions",
            List.of(new OrderItemResponse(11L, 101L, "Burger", new BigDecimal("120.00"), 2, "Extra cheese", new BigDecimal("240.00"))),
            null,
            null
        );
    }

    @Test
    void placeOrder_ReturnsCreated() throws Exception {
        when(orderService.placeOrder(any())).thenReturn(orderResponse);

        mockMvc.perform(post("/api/v1/orders/place")
                .contentType("application/json")
                .content("""
                    {"checkoutReference":"checkout-1","customerId":10,"restaurantId":20,"discount":20.00,"modeOfPayment":"CARD","estimatedDelivery":"2030-01-01T10:15:30","deliveryAddress":"221B Baker Street","specialInstructions":"No onions","items":[{"menuItemId":101,"name":"Burger","price":120.00,"quantity":2,"customization":"Extra cheese"}]}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.finalAmount").value(295.00));
    }

    @Test
    void placeOrder_WhenValidationFails_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/orders/place")
                .contentType("application/json")
                .content("""
                    {"checkoutReference":"","customerId":10,"restaurantId":20,"discount":20.00,"modeOfPayment":"CARD","estimatedDelivery":"2030-01-01T10:15:30","deliveryAddress":"221B Baker Street","items":[{"menuItemId":101,"name":"Burger","price":120.00,"quantity":2}]}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("must not be blank"));
    }

    @Test
    void getOrderById_ReturnsOk() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(orderResponse);

        mockMvc.perform(get("/api/v1/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Burger"));
    }

    @Test
    void getOrderById_WhenNotFound_Returns404WithProperMessage() throws Exception {
        when(orderService.getOrderById(999L)).thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(get("/api/v1/orders/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Order not found with id: 999"));
    }

    @Test
    void getActiveOrders_ReturnsOk() throws Exception {
        when(orderService.getActiveOrders()).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/v1/orders/active"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].orderId").value(1));
    }

    @Test
    void countOrders_ReturnsOk() throws Exception {
        when(orderService.countOrders(20L)).thenReturn(4L);

        mockMvc.perform(get("/api/v1/orders/count").param("restaurantId", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.restaurantId").value(20))
            .andExpect(jsonPath("$.totalOrders").value(4));
    }

    @Test
    void getCustomerOrders_ReturnsOk() throws Exception {
        when(orderService.getOrdersByCustomerId(10L)).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/v1/orders/customer/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].customerId").value(10));
    }

    @Test
    void getRestaurantOrders_ReturnsOk() throws Exception {
        when(orderService.getOrdersByRestaurantId(20L)).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/v1/orders/restaurant/20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].restaurantId").value(20));
    }

    @Test
    void getAgentOrders_AliasReturnsOk() throws Exception {
        when(orderService.getOrdersByDeliveryAgentId(77L)).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/v1/orders/agent/77"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].orderId").value(1));
    }

    @Test
    void getDeliveryAgentOrders_ReturnsOk() throws Exception {
        when(orderService.getOrdersByDeliveryAgentId(77L)).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/v1/orders/delivery-agent/77"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].orderId").value(1));
    }

    @Test
    void getAvailableOrders_ReturnsOk() throws Exception {
        when(orderService.getAvailableOrders()).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/v1/orders/available"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].orderId").value(1));
    }

    @Test
    void updateStatus_ReturnsOk() throws Exception {
        OrderResponse confirmedResponse = new OrderResponse(
            orderResponse.orderId(),
            orderResponse.customerId(),
            orderResponse.restaurantId(),
            orderResponse.deliveryAgentId(),
            orderResponse.totalAmount(),
            orderResponse.discount(),
            orderResponse.finalAmount(),
            orderResponse.modeOfPayment(),
            orderResponse.paymentStatus(),
            OrderStatus.CONFIRMED,
            orderResponse.orderDate(),
            orderResponse.estimatedDelivery(),
            orderResponse.deliveryAddress(),
            orderResponse.specialInstructions(),
            orderResponse.items(),
            orderResponse.restaurant(),
            orderResponse.deliveryPartner()
        );
        when(orderService.updateOrderStatus(1L, OrderStatus.CONFIRMED)).thenReturn(confirmedResponse);

        mockMvc.perform(put("/api/v1/orders/1/status")
                .contentType("application/json")
                .content("""
                    {"orderStatus":"CONFIRMED"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderStatus").value("CONFIRMED"));
    }

    @Test
    void updateStatus_WhenNotFound_Returns404() throws Exception {
        when(orderService.updateOrderStatus(999L, OrderStatus.CONFIRMED)).thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(put("/api/v1/orders/999/status")
                .contentType("application/json")
                .content("""
                    {"orderStatus":"CONFIRMED"}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Order not found with id: 999"));
    }

    @Test
    void assignDeliveryAgent_ReturnsOk() throws Exception {
        OrderResponse assignedResponse = new OrderResponse(
            orderResponse.orderId(),
            orderResponse.customerId(),
            orderResponse.restaurantId(),
            70L,
            orderResponse.totalAmount(),
            orderResponse.discount(),
            orderResponse.finalAmount(),
            orderResponse.modeOfPayment(),
            orderResponse.paymentStatus(),
            orderResponse.orderStatus(),
            orderResponse.orderDate(),
            orderResponse.estimatedDelivery(),
            orderResponse.deliveryAddress(),
            orderResponse.specialInstructions(),
            orderResponse.items(),
            orderResponse.restaurant(),
            orderResponse.deliveryPartner()
        );
        when(orderService.assignDeliveryAgent(1L, 70L)).thenReturn(assignedResponse);

        mockMvc.perform(put("/api/v1/orders/1/assign-agent")
                .contentType("application/json")
                .content("""
                    {"deliveryAgentId":70}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deliveryAgentId").value(70));
    }

    @Test
    void assignDeliveryAgent_WhenConflict_Returns409() throws Exception {
        when(orderService.assignDeliveryAgent(1L, 70L))
            .thenThrow(new ConflictException("Order has already been accepted by another delivery partner"));

        mockMvc.perform(put("/api/v1/orders/1/assign-agent")
                .contentType("application/json")
                .content("""
                    {"deliveryAgentId":70}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Order has already been accepted by another delivery partner"));
    }

    @Test
    void updatePaymentStatus_ReturnsOk() throws Exception {
        OrderResponse paidResponse = new OrderResponse(
            orderResponse.orderId(),
            orderResponse.customerId(),
            orderResponse.restaurantId(),
            orderResponse.deliveryAgentId(),
            orderResponse.totalAmount(),
            orderResponse.discount(),
            orderResponse.finalAmount(),
            orderResponse.modeOfPayment(),
            "PAID",
            orderResponse.orderStatus(),
            orderResponse.orderDate(),
            orderResponse.estimatedDelivery(),
            orderResponse.deliveryAddress(),
            orderResponse.specialInstructions(),
            orderResponse.items(),
            orderResponse.restaurant(),
            orderResponse.deliveryPartner()
        );
        when(orderService.updatePaymentStatus(1L, "PAID")).thenReturn(paidResponse);

        mockMvc.perform(put("/api/v1/orders/1/payment-status")
                .contentType("application/json")
                .content("""
                    {"paymentStatus":"PAID"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    void updatePaymentStatus_WhenValidationFails_ReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/orders/1/payment-status")
                .contentType("application/json")
                .content("""
                    {"paymentStatus":" "}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("must not be blank"));
    }

    @Test
    void cancelOrder_ReturnsOk() throws Exception {
        when(orderService.cancelOrder(1L)).thenReturn(orderResponse);

        mockMvc.perform(put("/api/v1/orders/1/cancel"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(1));
    }

    @Test
    void reorder_ReturnsCreated() throws Exception {
        when(orderService.reorder(1L)).thenReturn(orderResponse);

        mockMvc.perform(post("/api/v1/orders/1/reorder"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.orderId").value(1));
    }
}
