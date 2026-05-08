package com.quickbite.menu.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
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
import com.quickbite.menu.dto.MenuCategoryResponse;
import com.quickbite.menu.dto.MenuItemResponse;
import com.quickbite.menu.dto.RestaurantMenuResponse;
import com.quickbite.menu.exception.GlobalExceptionHandler;
import com.quickbite.menu.exception.MenuNotFoundException;
import com.quickbite.menu.service.MenuService;

@ExtendWith(MockitoExtension.class)
class MenuControllerTest {

    @Mock
    private MenuService menuService;

    private MockMvc mockMvc;
    private MenuCategoryResponse categoryResponse;
    private MenuItemResponse itemResponse;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new MenuController(menuService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .setValidator(validator)
            .build();

        itemResponse = new MenuItemResponse(1, 10, 1, "Paneer Tikka", "desc", 250.0, 220.0, "img", true, true, 4.5, 350, "starter");
        categoryResponse = new MenuCategoryResponse(1, 10, "Starters", "desc", "img", 1, List.of(itemResponse));
    }

    @Test
    void create_Category_ReturnsCreated() throws Exception {
        when(menuService.addCategory(any())).thenReturn(categoryResponse);

        mockMvc.perform(post("/api/v1/menu/create")
                .contentType("application/json")
                .content("""
                    {"type":"CATEGORY","category":{"categoryId":1,"restaurantId":10,"name":"Starters","description":"desc","imageUrl":"img","displayOrder":1}}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.categoryId").value(1));
    }

    @Test
    void create_Item_ReturnsCreated() throws Exception {
        when(menuService.addItem(any())).thenReturn(itemResponse);

        mockMvc.perform(post("/api/v1/menu/create")
                .contentType("application/json")
                .content("""
                    {"type":"ITEM","item":{"itemId":1,"restaurantId":10,"categoryId":1,"name":"Paneer Tikka","description":"desc","price":250.0,"discountedPrice":220.0,"imageUrl":"img","isVeg":true,"isAvailable":true,"rating":4.5,"calories":350,"tags":"starter"}}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.itemId").value(1));
    }

    @Test
    void create_CategoryPayloadMissing_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/menu/create")
                .contentType("application/json")
                .content("{\"type\":\"CATEGORY\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Category payload is required"));
    }

    @Test
    void create_ItemPayloadMissing_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/menu/create")
                .contentType("application/json")
                .content("{\"type\":\"ITEM\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Item payload is required"));
    }

    @Test
    void getMenuByRestaurant_ReturnsOk() throws Exception {
        when(menuService.getMenuByRestaurant(10)).thenReturn(new RestaurantMenuResponse(10, 1, List.of(categoryResponse)));

        mockMvc.perform(get("/api/v1/menu/restaurant/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.restaurantId").value(10))
            .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void getMenuByCategory_ReturnsOk() throws Exception {
        when(menuService.getItemsByCategory(1)).thenReturn(List.of(itemResponse));

        mockMvc.perform(get("/api/v1/menu/category/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].itemId").value(1));
    }

    @Test
    void getItem_ReturnsOk() throws Exception {
        when(menuService.getItemById(1)).thenReturn(itemResponse);

        mockMvc.perform(get("/api/v1/menu/item/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Paneer Tikka"));
    }

    @Test
    void getItem_WhenMenuIdNotFound_Returns404WithProperMessage() throws Exception {
        when(menuService.getItemById(999))
            .thenThrow(new MenuNotFoundException("Menu item not found with id: 999"));

        mockMvc.perform(get("/api/v1/menu/item/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Menu item not found with id: 999"));
    }

    @Test
    void update_Category_ReturnsOk() throws Exception {
        when(menuService.updateCategory(any())).thenReturn(categoryResponse);

        mockMvc.perform(put("/api/v1/menu")
                .contentType("application/json")
                .content("""
                    {"type":"CATEGORY","category":{"categoryId":1,"restaurantId":10,"name":"Starters","description":"desc","imageUrl":"img","displayOrder":1}}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categoryId").value(1));
    }

    @Test
    void update_Item_ReturnsOk() throws Exception {
        when(menuService.updateItem(any())).thenReturn(itemResponse);

        mockMvc.perform(put("/api/v1/menu")
                .contentType("application/json")
                .content("""
                    {"type":"ITEM","item":{"itemId":1,"restaurantId":10,"categoryId":1,"name":"Paneer Tikka","description":"desc","price":250.0,"discountedPrice":220.0,"imageUrl":"img","isVeg":true,"isAvailable":true,"rating":4.5,"calories":350,"tags":"starter"}}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemId").value(1));
    }

    @Test
    void toggleAvailability_ReturnsOk() throws Exception {
        when(menuService.toggleAvailability(1, false)).thenReturn(itemResponse);

        mockMvc.perform(put("/api/v1/menu/toggleAvailability")
                .contentType("application/json")
                .content("{\"itemId\":1,\"available\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.itemId").value(1));
    }

    @Test
    void delete_Category_ReturnsOk() throws Exception {
        doNothing().when(menuService).deleteCategory(1);

        mockMvc.perform(delete("/api/v1/menu/delete")
                .contentType("application/json")
                .content("{\"type\":\"CATEGORY\",\"categoryId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Menu category deleted"));
    }

    @Test
    void delete_CategoryMissingId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/v1/menu/delete")
                .contentType("application/json")
                .content("{\"type\":\"CATEGORY\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("categoryId is required"));
    }

    @Test
    void delete_Item_ReturnsOk() throws Exception {
        doNothing().when(menuService).deleteItem(1);

        mockMvc.perform(delete("/api/v1/menu/delete")
                .contentType("application/json")
                .content("{\"type\":\"ITEM\",\"itemId\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Menu item deleted"));
    }

    @Test
    void delete_ItemMissingId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(delete("/api/v1/menu/delete")
                .contentType("application/json")
                .content("{\"type\":\"ITEM\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("itemId is required"));
    }

    @Test
    void delete_ItemWhenMenuIdNotFound_Returns404WithProperMessage() throws Exception {
        org.mockito.Mockito.doThrow(new MenuNotFoundException("Menu item not found with id: 999"))
            .when(menuService).deleteItem(999);

        mockMvc.perform(delete("/api/v1/menu/delete")
                .contentType("application/json")
                .content("{\"type\":\"ITEM\",\"itemId\":999}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Menu item not found with id: 999"));
    }

    @Test
    void search_ReturnsOk() throws Exception {
        when(menuService.searchItems("paneer")).thenReturn(List.of(itemResponse));

        mockMvc.perform(get("/api/v1/menu/search").param("query", "paneer"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].itemId").value(1));
    }

    @Test
    void getVegItems_ReturnsOk() throws Exception {
        when(menuService.getVegItems(10, true)).thenReturn(List.of(itemResponse));

        mockMvc.perform(get("/api/v1/menu/vegItems")
                .param("restaurantId", "10")
                .param("availableOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].itemId").value(1));
    }

    @Test
    void search_WhenQueryIsBlank_ServiceErrorBecomesBadRequest() throws Exception {
        when(menuService.searchItems(" ")).thenThrow(new com.quickbite.menu.exception.BadRequestException("query must not be blank"));

        mockMvc.perform(get("/api/v1/menu/search").param("query", " "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("query must not be blank"));
    }
}
