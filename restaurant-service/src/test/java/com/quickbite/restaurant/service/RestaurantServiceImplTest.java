package com.quickbite.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.quickbite.restaurant.dto.RestaurantRequest;
import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.exception.BadRequestException;
import com.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.restaurant.util.RestaurantMapper;

class RestaurantServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    private RestaurantServiceImpl restaurantService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        restaurantService = new RestaurantServiceImpl(restaurantRepository, new RestaurantMapper());
    }

    @Test
    void registerRestaurantShouldInitializeWorkflowFields() {
        RestaurantRequest request = new RestaurantRequest(
            11L,
            "QuickBite Cafe",
            "All day dining",
            "Indian",
            "12 Main Street",
            "Pune",
            18.5204,
            73.8567,
            "9876543210",
            6.5,
            150,
            35
        );

        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant restaurant = invocation.getArgument(0);
            restaurant.setRestaurantId(1L);
            return restaurant;
        });

        var response = restaurantService.registerRestaurant(request);

        assertThat(response.restaurantId()).isEqualTo(1L);
        assertThat(response.avgRating()).isZero();
        assertThat(response.isOpen()).isFalse();
        assertThat(response.isApproved()).isFalse();
    }

    @Test
    void findNearbyRestaurantsShouldOnlyReturnApprovedOpenRestaurantsWithinRadius() {
        Restaurant nearby = Restaurant.builder()
            .restaurantId(1L)
            .ownerId(1L)
            .name("Nearby Kitchen")
            .description("Desc")
            .cuisine("Indian")
            .address("Addr")
            .city("Pune")
            .latitude(18.5205)
            .longitude(73.8568)
            .phone("9999999999")
            .avgRating(4.2)
            .deliveryRadius(2.0)
            .isOpen(true)
            .isApproved(true)
            .minOrderAmount(100)
            .estimatedDeliveryMin(25)
            .build();

        Restaurant farAway = Restaurant.builder()
            .restaurantId(2L)
            .ownerId(2L)
            .name("Far Kitchen")
            .description("Desc")
            .cuisine("Indian")
            .address("Addr")
            .city("Pune")
            .latitude(19.0760)
            .longitude(72.8777)
            .phone("8888888888")
            .avgRating(4.0)
            .deliveryRadius(3.0)
            .isOpen(true)
            .isApproved(true)
            .minOrderAmount(120)
            .estimatedDeliveryMin(40)
            .build();

        when(restaurantRepository.findByIsOpenTrueAndIsApprovedTrue()).thenReturn(List.of(nearby, farAway));

        var result = restaurantService.findNearbyRestaurants(18.5204, 73.8567);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Nearby Kitchen");
    }

    @Test
    void updateAverageRatingShouldRejectOutOfRangeValue() {
        assertThatThrownBy(() -> restaurantService.updateAverageRating(1L, 5.6))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("avgRating");
    }

    @Test
    void deleteRestaurantShouldDeleteExistingRestaurant() {
        Restaurant restaurant = Restaurant.builder()
            .restaurantId(5L)
            .ownerId(3L)
            .name("Delete Me")
            .description("Desc")
            .cuisine("Indian")
            .address("Addr")
            .city("Pune")
            .latitude(18.5204)
            .longitude(73.8567)
            .phone("7777777777")
            .avgRating(3.9)
            .deliveryRadius(5.0)
            .isOpen(false)
            .isApproved(false)
            .minOrderAmount(100)
            .estimatedDeliveryMin(30)
            .build();

        when(restaurantRepository.findById(5L)).thenReturn(Optional.of(restaurant));

        restaurantService.deleteRestaurant(5L);

        verify(restaurantRepository).delete(restaurant);
    }
}
