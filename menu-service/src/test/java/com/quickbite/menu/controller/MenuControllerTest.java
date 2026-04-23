package com.quickbite.menu.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.quickbite.menu.config.OpenApiConfig;
import com.quickbite.menu.controller.MenuController;
import com.quickbite.menu.dto.MenuCategoryResponse;
import com.quickbite.menu.dto.RestaurantMenuResponse;
import com.quickbite.menu.security.SecurityConfig;
import com.quickbite.menu.security.RoleClaimJwtAuthenticationConverter;
import com.quickbite.menu.service.MenuService;

@WebMvcTest(MenuController.class)
@Import({SecurityConfig.class, OpenApiConfig.class, RoleClaimJwtAuthenticationConverter.class})
@TestPropertySource(properties = "app.jwt.secret=change-me-change-me-change-me-change-me")
class MenuResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuService menuService;

    @Test
    void getRestaurantMenuShouldBePublic() throws Exception {
        when(menuService.getMenuByRestaurant(7)).thenReturn(new RestaurantMenuResponse(7, 0, List.of()));

        mockMvc.perform(get("/menu/restaurant/7"))
            .andExpect(status().isOk());
    }

    @Test
    void createShouldRequireRestaurantOwnerRole() throws Exception {
        when(menuService.addCategory(any())).thenReturn(new MenuCategoryResponse(1, 4, "Starters", null, null, 1, List.of()));

        String payload = """
            {
              "type": "CATEGORY",
              "category": {
                "restaurantId": 4,
                "name": "Starters",
                "description": "Quick bites",
                "imageUrl": null,
                "displayOrder": 1
              }
            }
            """;

        mockMvc.perform(post("/menu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/menu")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RESTAURANT_OWNER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isCreated());
    }
}
