package com.quickbite.restaurant.service;

import java.util.Comparator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.quickbite.restaurant.client.AuthServiceClient;
import com.quickbite.restaurant.dto.AdminRestaurantResponse;
import com.quickbite.restaurant.dto.RestaurantRequest;
import com.quickbite.restaurant.dto.RestaurantResponse;
import com.quickbite.restaurant.dto.UserSummaryDto;
import com.quickbite.restaurant.entity.ApprovalStatus;
import com.quickbite.restaurant.entity.Restaurant;
import com.quickbite.restaurant.exception.BadRequestException;
import com.quickbite.restaurant.exception.RestaurantNotFoundException;
import com.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.restaurant.util.RestaurantMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;
    private final AuthServiceClient authServiceClient;

    @Override
    @Transactional
    public RestaurantResponse registerRestaurant(RestaurantRequest request) {
        Restaurant restaurant = restaurantRepository.findFirstByOwnerIdOrderByRestaurantIdAsc(request.ownerId())
            .orElseGet(() -> restaurantMapper.toEntity(request));

        restaurantMapper.updateEntity(restaurant, request);
        restaurant.setAvgRating(restaurant.getAvgRating() == null ? 0.0 : restaurant.getAvgRating());
        restaurant.setIsOpen(Boolean.FALSE);
        restaurant.setIsApproved(Boolean.FALSE);
        restaurant.setApprovalStatus(ApprovalStatus.PENDING);
        restaurant.setRejectionReason(null);
        restaurant.setReviewedByAdminId(null);
        restaurant.setReviewedAt(null);
        restaurant.setSubmittedAt(java.time.LocalDateTime.now());

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        log.info("Restaurant registration successful. restaurantId={}, ownerId={}, status={}",
                savedRestaurant.getRestaurantId(), savedRestaurant.getOwnerId(), savedRestaurant.getApprovalStatus());

        return restaurantMapper.toResponse(savedRestaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public RestaurantResponse getRestaurantById(Long restaurantId) {

        Restaurant restaurant = getRestaurant(restaurantId);
        log.info("Getting restaurant by id - restaurantId={}, name={}, approvalStatus={}",
                restaurant.getRestaurantId(), restaurant.getName(), restaurant.getApprovalStatus());

        return restaurantMapper.toResponse(restaurant);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getRestaurantsByOwner(Long ownerId) {

        List<RestaurantResponse> restaurantList = restaurantRepository.findByOwnerId(ownerId).stream()
            .sorted(Comparator.comparing(Restaurant::getName, String.CASE_INSENSITIVE_ORDER))
            .map(restaurantMapper::toResponse)
            .toList();

        log.info("Getting all restaurants registered by one owner - Owner id = {}", ownerId);

        return restaurantList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> getApprovedRestaurants() {

        List<RestaurantResponse> restaurantResponseList = restaurantRepository.findByApprovalStatusAndIsApprovedTrue(ApprovalStatus.APPROVED).stream()
            .sorted(Comparator.comparing(Restaurant::getName, String.CASE_INSENSITIVE_ORDER))
            .map(restaurantMapper::toResponse)
            .toList();

        log.info("All approved restaurants list are given");

        return restaurantResponseList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> searchRestaurants(String name, String city, String cuisine) {
        List<RestaurantResponse> restaurantList =  restaurantRepository.findByApprovalStatusAndIsApprovedTrue(ApprovalStatus.APPROVED).stream()
            .filter(restaurant -> containsIgnoreCase(restaurant.getName(), name))
            .filter(restaurant -> equalsIgnoreCase(restaurant.getCity(), city))
            .filter(restaurant -> equalsIgnoreCase(restaurant.getCuisine(), cuisine))
            .sorted(Comparator.comparing(Restaurant::getName, String.CASE_INSENSITIVE_ORDER))
            .map(restaurantMapper::toResponse)
            .toList();

        log.info("Restaurant search completed. name={}, city={}, cuisine={}, totalResults={}",
                name, city, cuisine, restaurantList.size());

        return restaurantList;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RestaurantResponse> findNearbyRestaurants(double latitude, double longitude, double radiusKm) {
        List<RestaurantResponse> nearbyRestaurants = restaurantRepository.findByIsOpenTrueAndIsApprovedTrue().stream()
            .map(restaurant -> new RestaurantDistance(restaurant, calculateDistance(
                latitude,
                longitude,
                restaurant.getLatitude(),
                restaurant.getLongitude()
            )))
            .filter(result -> result.distance() <= Math.min(radiusKm, result.restaurant().getDeliveryRadius()))
            .sorted(Comparator.comparingDouble(RestaurantDistance::distance))
            .map(RestaurantDistance::restaurant)
            .map(restaurantMapper::toResponse)
            .toList();

        log.info("Nearby restaurant search completed. latitude={}, longitude={}, radiusKm={}, totalResults={}",
                latitude, longitude, radiusKm, nearbyRestaurants.size());

        return nearbyRestaurants;
    }

    @Override
    @Transactional
    public RestaurantResponse updateRestaurant(Long restaurantId, RestaurantRequest request) {
        Restaurant restaurant = getRestaurant(restaurantId);

        restaurantMapper.updateEntity(restaurant, request);

        if(restaurant.getApprovalStatus() != ApprovalStatus.APPROVED) {
            restaurant.setApprovalStatus(ApprovalStatus.PENDING);
            restaurant.setIsApproved(Boolean.FALSE);
            restaurant.setRejectionReason(null);
            restaurant.setReviewedByAdminId(null);
            restaurant.setReviewedAt(null);
            restaurant.setSubmittedAt(java.time.LocalDateTime.now());
        }

        log.info("Restaurant updated successfully. restaurantId={}, ownerId={}, previousStatus={}, currentStatus={}",
                restaurant.getRestaurantId(), restaurant.getOwnerId(), restaurant.getApprovalStatus(), restaurant.getApprovalStatus());

        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminRestaurantResponse> getPendingRestaurants() {
        List<AdminRestaurantResponse> pendingRestaurants = restaurantRepository.findByApprovalStatus(ApprovalStatus.PENDING).stream()
            .sorted(Comparator.comparing(Restaurant::getSubmittedAt).reversed())
            .map(this::toAdminResponse)
            .toList();

        log.info("Pending restaurants fetched for admin review. totalPendingRestaurants={}",
                pendingRestaurants.size());

        return pendingRestaurants;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminRestaurantResponse> getAllRestaurantsForAdmin() {
        List<AdminRestaurantResponse> allRestaurantsForAdmin = restaurantRepository.findAll().stream()
            .sorted(Comparator.comparing(Restaurant::getSubmittedAt).reversed())
            .map(this::toAdminResponse)
            .toList();

        log.info("All restaurants fetched for admin dashboard. totalRestaurants={}",
                allRestaurantsForAdmin.size());

        return allRestaurantsForAdmin;
    }

    @Override
    @Transactional
    public RestaurantResponse approveRestaurant(Long restaurantId, Long adminId) {
        Restaurant restaurant = getRestaurant(restaurantId);

        restaurant.setIsApproved(Boolean.TRUE);
        restaurant.setApprovalStatus(ApprovalStatus.APPROVED);
        restaurant.setRejectionReason(null);
        restaurant.setReviewedByAdminId(adminId);
        restaurant.setReviewedAt(java.time.LocalDateTime.now());

        Restaurant approvedRestaurant = restaurantRepository.save(restaurant);

        log.info("Restaurant approved successfully. restaurantId={}, ownerId={}, approvedByAdminId={}",
                approvedRestaurant.getRestaurantId(),
                approvedRestaurant.getOwnerId(),
                adminId);

        return restaurantMapper.toResponse(approvedRestaurant);
    }

    @Override
    @Transactional
    public RestaurantResponse rejectRestaurant(Long restaurantId, Long adminId, String feedback) {
        Restaurant restaurant = getRestaurant(restaurantId);

        restaurant.setIsApproved(Boolean.FALSE);
        restaurant.setApprovalStatus(ApprovalStatus.REJECTED);
        restaurant.setRejectionReason(feedback);
        restaurant.setReviewedByAdminId(adminId);
        restaurant.setReviewedAt(java.time.LocalDateTime.now());
        restaurant.setIsOpen(Boolean.FALSE);

        Restaurant rejectedRestaurant = restaurantRepository.save(restaurant);

        log.info("Restaurant rejected successfully. restaurantId={}, ownerId={}, rejectedByAdminId={}",
                rejectedRestaurant.getRestaurantId(),
                rejectedRestaurant.getOwnerId(),
                adminId);

        return restaurantMapper.toResponse(rejectedRestaurant);
    }

    @Override
    @Transactional
    public RestaurantResponse toggleRestaurantStatus(Long restaurantId, boolean open) {
        Restaurant restaurant = getRestaurant(restaurantId);

        Boolean previousStatus = restaurant.getIsOpen();
        restaurant.setIsOpen(open);

        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);

        log.info("Restaurant open status changed. restaurantId={}, previousOpenStatus={}, currentOpenStatus={}",
                updatedRestaurant.getRestaurantId(),
                previousStatus,
                updatedRestaurant.getIsOpen());

        return restaurantMapper.toResponse(updatedRestaurant);
    }

    @Override
    @Transactional
    public RestaurantResponse updateAverageRating(Long restaurantId, double avgRating) {
        if(avgRating < 0.0 || avgRating > 5.0) {
            log.warn("Invalid average rating update requested. restaurantId={}, requestedRating={}",
                    restaurantId, avgRating);

            throw new BadRequestException("avgRating must be between 0.0 and 5.0");
        }

        Restaurant restaurant = getRestaurant(restaurantId);
        Double previousRating = restaurant.getAvgRating();
        restaurant.setAvgRating(avgRating);
        Restaurant updatedRestaurant = restaurantRepository.save(restaurant);

        log.info("Restaurant average rating updated. restaurantId={}, previousRating={}, newRating={}",
                updatedRestaurant.getRestaurantId(),
                previousRating,
                updatedRestaurant.getAvgRating());

        return restaurantMapper.toResponse(updatedRestaurant);
    }

    @Override
    @Transactional
    public void deleteRestaurant(Long restaurantId) {
        Restaurant restaurant = getRestaurant(restaurantId);

        restaurantRepository.delete(restaurant);

        log.info("Restaurant deleted successfully. restaurantId={}, ownerId={}, restaurantName={}",
                restaurant.getRestaurantId(),
                restaurant.getOwnerId(),
                restaurant.getName());
    }

    private Restaurant getRestaurant(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
            .orElseThrow(() -> {
                    log.warn("Restaurant not found. restaurantId={}", restaurantId);
                    return new RestaurantNotFoundException(restaurantId);
            });
    }

    private AdminRestaurantResponse toAdminResponse(Restaurant restaurant) {
        UserSummaryDto owner = null;

        try {
            owner = authServiceClient.getUserSummary("RESTAURANT_OWNER", restaurant.getOwnerId());
        } catch (RuntimeException exception) {
            log.warn("Owner details could not be fetched from auth-service. restaurantId={}, ownerId={}, error={}",
                    restaurant.getRestaurantId(),
                    restaurant.getOwnerId(),
                    exception.getMessage());
        }

        return restaurantMapper.toAdminResponse(restaurant, owner);
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
