package com.quickbite.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.quickbite.review.dto.ReviewResponse;
import com.quickbite.review.enums.ReviewType;
import com.quickbite.review.exception.GlobalExceptionHandler;
import com.quickbite.review.exception.ReviewNotFoundException;
import com.quickbite.review.service.ReviewService;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    private MockMvc mockMvc;
    private ReviewResponse reviewResponse;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();

        reviewResponse = new ReviewResponse(
            1L,
            10L,
            20L,
            30L,
            40L,
            ReviewType.FOOD,
            5,
            "Excellent",
            LocalDateTime.now(),
            false
        );
    }

    @Test
    void createFoodReview_ReturnsCreated() throws Exception {
        when(reviewService.createFoodReview(any())).thenReturn(reviewResponse);

        mockMvc.perform(post("/api/v1/reviews/food")
                .contentType("application/json")
                .content("""
                    {"orderId":10,"customerId":20,"rating":5,"comment":"Excellent"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reviewId").value(1))
            .andExpect(jsonPath("$.reviewType").value("FOOD"));
    }

    @Test
    void createDeliveryReview_WhenValidationFails_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/reviews/delivery")
                .contentType("application/json")
                .content("""
                    {"orderId":10,"customerId":20,"rating":6,"comment":"Too high"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details[0]").value("rating: rating must be between 1 and 5"));
    }

    @Test
    void getReviewById_ReturnsOk() throws Exception {
        when(reviewService.getReviewById(1L)).thenReturn(reviewResponse);

        mockMvc.perform(get("/api/v1/reviews/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewId").value(1))
            .andExpect(jsonPath("$.comment").value("Excellent"));
    }

    @Test
    void getReviewById_WhenNotFound_Returns404WithProperMessage() throws Exception {
        when(reviewService.getReviewById(999L)).thenThrow(new ReviewNotFoundException(999L));

        mockMvc.perform(get("/api/v1/reviews/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Review not found with id: 999"));
    }

    @Test
    void listEndpointsAndAverageEndpoints_ReturnOk() throws Exception {
        when(reviewService.getReviewsByRestaurantId(30L)).thenReturn(List.of(reviewResponse));
        when(reviewService.getReviewsByCustomerId(20L)).thenReturn(List.of(reviewResponse));
        when(reviewService.getReviewsByOrderId(10L)).thenReturn(List.of(reviewResponse));
        when(reviewService.getReviewsByAgentId(40L)).thenReturn(List.of(reviewResponse));
        when(reviewService.getAllReviews()).thenReturn(List.of(reviewResponse));
        when(reviewService.getAverageFoodRating(30L)).thenReturn(4.7d);
        when(reviewService.getAverageDeliveryRating(40L)).thenReturn(4.2d);

        mockMvc.perform(get("/api/v1/reviews/restaurants/30"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].reviewId").value(1));
        mockMvc.perform(get("/api/v1/reviews/customers/20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].customerId").value(20));
        mockMvc.perform(get("/api/v1/reviews/orders/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].orderId").value(10));
        mockMvc.perform(get("/api/v1/reviews/agents/40"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].agentId").value(40));
        mockMvc.perform(get("/api/v1/reviews/admin"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].reviewId").value(1));
        mockMvc.perform(get("/api/v1/reviews/restaurants/30/average"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(4.7d));
        mockMvc.perform(get("/api/v1/reviews/agents/40/average"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(4.2d));
    }
}
