package com.quickbite.restaurant.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.quickbite.restaurant.dto.RestaurantRequest;
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.exception.BadRequestException;
import com.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.restaurant.util.RestaurantMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;

    @Override
    @Transactional
    public RestaurantResponse registerRestaurant(RestaurantRequest request) {
        Restaurant restaurant = restaurantMapper.toEntity(request);
        restaurant.setAvgRating(0.0);
        restaurant.setIsOpen(Boolean.FALSE);
        restaurant.setIsApproved(Boolean.FALSE);
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurantById(Long restaurantId) {
        return restaurantMapper.toResponse(getRestaurant(restaurantId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getRestaurantsByOwner(Long ownerId) {
        return restaurantRepository.findByOwnerId(ownerId).stream()
            .sorted(Comparator.comparing(Restaurant::getName, String.CASE_INSENSITIVE_ORDER))
            .map(restaurantMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> searchRestaurants(String name, String city, String cuisine) {
        return restaurantRepository.findAll().stream()
            .filter(restaurant -> containsIgnoreCase(restaurant.getName(), name))
            .filter(restaurant -> equalsIgnoreCase(restaurant.getCity(), city))
            .filter(restaurant -> equalsIgnoreCase(restaurant.getCuisine(), cuisine))
            .sorted(Comparator.comparing(Restaurant::getName, String.CASE_INSENSITIVE_ORDER))
            .map(restaurantMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> findNearbyRestaurants(double latitude, double longitude) {
        return restaurantRepository.findByIsOpenTrueAndIsApprovedTrue().stream()
            .map(restaurant -> new RestaurantDistance(restaurant, calculateDistance(
                latitude,
                longitude,
                restaurant.getLatitude(),
                restaurant.getLongitude()
            )))
            .filter(result -> result.distance() <= result.restaurant().getDeliveryRadius())
            .sorted(Comparator.comparingDouble(RestaurantDistance::distance))
            .map(RestaurantDistance::restaurant)
            .map(restaurantMapper::toResponse)
            .toList();
    }

    @Override
    @Transactional
    public RestaurantResponse updateRestaurant(Long restaurantId, RestaurantRequest request) {
        Restaurant restaurant = getRestaurant(restaurantId);
        restaurantMapper.updateEntity(restaurant, request);
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional
    public RestaurantResponse approveRestaurant(Long restaurantId, boolean approved) {
        Restaurant restaurant = getRestaurant(restaurantId);
        restaurant.setIsApproved(approved);
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional
    public RestaurantResponse toggleRestaurantStatus(Long restaurantId, boolean open) {
        Restaurant restaurant = getRestaurant(restaurantId);
        restaurant.setIsOpen(open);
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional
    public RestaurantResponse updateAverageRating(Long restaurantId, double avgRating) {
        if (avgRating < 0.0 || avgRating > 5.0) {
            throw new BadRequestException("avgRating must be between 0.0 and 5.0");
        }

        Restaurant restaurant = getRestaurant(restaurantId);
        restaurant.setAvgRating(avgRating);
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional
    public void deleteRestaurant(Long restaurantId) {
        Restaurant restaurant = getRestaurant(restaurantId);
        restaurantRepository.delete(restaurant);
    }

    private Restaurant getRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));
    }

    private boolean containsIgnoreCase(String value, String search) {
        return !StringUtils.hasText(search) || value.toLowerCase().contains(search.trim().toLowerCase());
    }

    private boolean equalsIgnoreCase(String value, String search) {
        return !StringUtils.hasText(search) || value.equalsIgnoreCase(search.trim());
    }

    private double calculateDistance(double latitude1, double longitude1, double latitude2, double longitude2) {
        double earthRadiusKm = 6371.0;
        double latDistance = Math.toRadians(latitude2 - latitude1);
        double lonDistance = Math.toRadians(longitude2 - longitude1);
        double originLatitude = Math.toRadians(latitude1);
        double destinationLatitude = Math.toRadians(latitude2);

        double haversine = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(originLatitude) * Math.cos(destinationLatitude)
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double arc = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return earthRadiusKm * arc;
    }

    private record RestaurantDistance(Restaurant restaurant, double distance) {
    }
}
