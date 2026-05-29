package com.quickbite.cartservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.quickbite.cartservice.dto.CartItemResponse;
import com.quickbite.cartservice.dto.CartResponse;
import com.quickbite.cartservice.exception.BadRequestException;
import com.quickbite.cartservice.exception.CartItemNotFoundException;
import com.quickbite.cartservice.exception.GlobalExceptionHandler;
import com.quickbite.cartservice.service.CartService;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    private MockMvc mockMvc;
    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new CartController(cartService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();

        cartResponse = new CartResponse(
            1L,
            10L,
            20L,
            180.0,
            List.of(new CartItemResponse(11L, 101L, "Burger", 90.0, 2, "Extra cheese", 180.0))
        );
    }

    @Test
    void getCart_ReturnsOk() throws Exception {
        when(cartService.getCartByCustomerId(10L)).thenReturn(cartResponse);

        mockMvc.perform(get("/api/v1/cart/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerId").value(10))
            .andExpect(jsonPath("$.items[0].name").value("Burger"));
    }

    @Test
    void addItem_ReturnsCreated() throws Exception {
        when(cartService.addItemToCart(any())).thenReturn(cartResponse);

        mockMvc.perform(post("/api/v1/cart/add")
                .contentType("application/json")
                .content("""
                    {"customerId":10,"restaurantId":20,"menuItemId":101,"name":"Burger","price":120.0,"quantity":2,"customization":"Extra cheese"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.cartId").value(1))
            .andExpect(jsonPath("$.totalPrice").value(180.0));
    }

    @Test
    void addItem_WhenValidationFails_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/cart/add")
                .contentType("application/json")
                .content("""
                    {"customerId":10,"restaurantId":20,"menuItemId":101,"name":"","price":120.0,"quantity":2}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("must not be blank"));
    }

    @Test
    void updateQuantity_WhenCartItemMissing_Returns404WithProperMessage() throws Exception {
        when(cartService.updateItemQuantity(any()))
            .thenThrow(new CartItemNotFoundException(99L));

        mockMvc.perform(put("/api/v1/cart/update-quantity")
                .contentType("application/json")
                .content("""
                    {"customerId":10,"itemId":99,"quantity":3}
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Cart item not found with id: 99"));
    }

    @Test
    void removeItem_WhenCartDetailsNotFound_Returns404WithProperMessage() throws Exception {
        when(cartService.removeItem(999L)).thenThrow(new CartItemNotFoundException(999L));

        mockMvc.perform(delete("/api/v1/cart/items/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Cart item not found with id: 999"));
    }

    @Test
    void updateQuantityByPath_ReturnsOk() throws Exception {
        when(cartService.updateItemQuantity(any())).thenReturn(cartResponse);

        mockMvc.perform(put("/api/v1/cart/items/11/quantity")
                .contentType("application/json")
                .content("""
                    {"customerId":10,"itemId":99,"quantity":2}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cartId").value(1))
            .andExpect(jsonPath("$.items[0].itemId").value(11));
    }

    @Test
    void removeItemByMenuItem_ReturnsOk() throws Exception {
        when(cartService.removeItem(10L, 101L)).thenReturn(new CartResponse(1L, 10L, null, 0.0, List.of()));

        mockMvc.perform(delete("/api/v1/cart/customer/10/items/menu/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerId").value(10))
            .andExpect(jsonPath("$.totalPrice").value(0.0));
    }

    @Test
    void applyPromo_WhenServiceRejectsRequest_ReturnsBadRequest() throws Exception {
        when(cartService.applyPromoCode(10L, "SAVE10"))
            .thenThrow(new BadRequestException("Promo code is not applicable to this cart"));

        mockMvc.perform(post("/api/v1/cart/apply-promo")
                .contentType("application/json")
                .content("""
                    {"customerId":10,"promoCode":"SAVE10"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Promo code is not applicable to this cart"));
    }

    @Test
    void clearCart_ReturnsNoContent() throws Exception {
        doNothing().when(cartService).clearCart(10L);

        mockMvc.perform(delete("/api/v1/cart/clear/10"))
            .andExpect(status().isNoContent());
    }

    @Test
    void updateQuantityByMenuItem_ReturnsOk() throws Exception {
        when(cartService.updateItemQuantity(10L, 101L, 4)).thenReturn(cartResponse);

        mockMvc.perform(put("/api/v1/cart/customer/10/items/menu/101/quantity")
                .contentType("application/json")
                .content("""
                    {"customerId":10,"itemId":7,"quantity":4}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].quantity").value(2));
    }
}
