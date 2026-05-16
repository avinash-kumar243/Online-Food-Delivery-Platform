package com.quickbite.restaurant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RestaurantServiceImplTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private AuthServiceClient authServiceClient;

    @InjectMocks
    private RestaurantServiceImpl restaurantService;

    @Captor
    private ArgumentCaptor<Restaurant> restaurantCaptor;

    private final RestaurantMapper restaurantMapper = new RestaurantMapper();

    private RestaurantRequest request;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        restaurantService = new RestaurantServiceImpl(restaurantRepository, restaurantMapper, authServiceClient);

        request = new RestaurantRequest(
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

        restaurant = Restaurant.builder()
            .restaurantId(1L)
            .ownerId(11L)
            .name("QuickBite Cafe")
            .description("All day dining")
            .cuisine("Indian")
            .address("12 Main Street")
            .city("Pune")
            .latitude(18.5204)
            .longitude(73.8567)
            .phone("9876543210")
            .avgRating(4.4)
            .deliveryRadius(6.5)
            .isOpen(true)
            .isApproved(true)
            .approvalStatus(ApprovalStatus.APPROVED)
            .rejectionReason("old reason")
            .reviewedByAdminId(99L)
            .reviewedAt(LocalDateTime.now().minusDays(1))
            .submittedAt(LocalDateTime.now().minusDays(2))
            .minOrderAmount(150)
            .estimatedDeliveryMin(35)
            .build();
    }

    @Test
    @DisplayName("Register Restaurant - Creates New Restaurant With Workflow Defaults")
    void registerRestaurant_NewRestaurant() {
        when(restaurantRepository.findFirstByOwnerIdOrderByRestaurantIdAsc(11L)).thenReturn(Optional.empty());
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant saved = invocation.getArgument(0);
            saved.setRestaurantId(1L);
            return saved;
        });

        RestaurantResponse response = restaurantService.registerRestaurant(request);

        verify(restaurantRepository).save(restaurantCaptor.capture());
        Restaurant savedRestaurant = restaurantCaptor.getValue();
        assertEquals(0.0, savedRestaurant.getAvgRating());
        assertFalse(savedRestaurant.getIsOpen());
        assertFalse(savedRestaurant.getIsApproved());
        assertEquals(ApprovalStatus.PENDING, savedRestaurant.getApprovalStatus());
        assertNull(savedRestaurant.getRejectionReason());
        assertNull(savedRestaurant.getReviewedByAdminId());
        assertNull(savedRestaurant.getReviewedAt());
        assertEquals("QuickBite Cafe", response.name());
        assertEquals("PENDING", response.approvalStatus());
    }

    @Test
    @DisplayName("Register Restaurant - Reuses Existing Draft For Owner")
    void registerRestaurant_ExistingRestaurant() {
        Restaurant existing = Restaurant.builder()
            .restaurantId(44L)
            .ownerId(11L)
            .avgRating(4.8)
            .build();
        when(restaurantRepository.findFirstByOwnerIdOrderByRestaurantIdAsc(11L)).thenReturn(Optional.of(existing));
        when(restaurantRepository.save(existing)).thenReturn(existing);

        RestaurantResponse response = restaurantService.registerRestaurant(request);

        assertEquals(44L, response.restaurantId());
        assertEquals("QuickBite Cafe", existing.getName());
        assertEquals(4.8, existing.getAvgRating());
        assertFalse(existing.getIsOpen());
        assertFalse(existing.getIsApproved());
        assertEquals(ApprovalStatus.PENDING, existing.getApprovalStatus());
    }

    @Test
    @DisplayName("Get Restaurant By Id - Success")
    void getRestaurantById_Success() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        RestaurantResponse response = restaurantService.getRestaurantById(1L);

        assertEquals(1L, response.restaurantId());
        assertEquals("QuickBite Cafe", response.name());
    }

    @Test
    @DisplayName("Get Restaurant By Id - Not Found")
    void getRestaurantById_NotFound() {
        when(restaurantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RestaurantNotFoundException.class, () -> restaurantService.getRestaurantById(99L));
    }

    @Test
    @DisplayName("Get Restaurants By Owner - Sorted By Name")
    void getRestaurantsByOwner_Sorted() {
        Restaurant zulu = cloneRestaurant(2L, "zulu cafe", ApprovalStatus.APPROVED, true, true);
        Restaurant alpha = cloneRestaurant(3L, "Alpha Bistro", ApprovalStatus.APPROVED, true, true);
        when(restaurantRepository.findByOwnerId(11L)).thenReturn(List.of(zulu, alpha));

        List<RestaurantResponse> responses = restaurantService.getRestaurantsByOwner(11L);

        assertEquals(2, responses.size());
        assertEquals("Alpha Bistro", responses.get(0).name());
        assertEquals("zulu cafe", responses.get(1).name());
    }

    @Test
    @DisplayName("Get Approved Restaurants - Sorted By Name")
    void getApprovedRestaurants_Sorted() {
        Restaurant zulu = cloneRestaurant(2L, "zulu cafe", ApprovalStatus.APPROVED, true, true);
        Restaurant alpha = cloneRestaurant(3L, "Alpha Bistro", ApprovalStatus.APPROVED, true, true);
        when(restaurantRepository.findByApprovalStatusAndIsApprovedTrue(ApprovalStatus.APPROVED))
            .thenReturn(List.of(zulu, alpha));

        List<RestaurantResponse> responses = restaurantService.getApprovedRestaurants();

        assertEquals(2, responses.size());
        assertEquals("Alpha Bistro", responses.get(0).name());
    }

    @Test
    @DisplayName("Search Restaurants - Filters By Name City And Cuisine")
    void searchRestaurants_FiltersResults() {


        Restaurant target = cloneRestaurant(1L, "QuickBite Cafe", ApprovalStatus.APPROVED, true, true);
        Restaurant otherCity = cloneRestaurant(2L, "QuickBite Deli", ApprovalStatus.APPROVED, true, true);
        otherCity.setCity("Mumbai");
        Restaurant otherCuisine = cloneRestaurant(3L, "Spice House", ApprovalStatus.APPROVED, true, true);
        otherCuisine.setCuisine("Chinese");
        when(restaurantRepository.findByApprovalStatusAndIsApprovedTrue(ApprovalStatus.APPROVED))
            .thenReturn(List.of(otherCuisine, otherCity, target));

        List<RestaurantResponse> responses = restaurantService.searchRestaurants("quick", "pune", "indian");

        assertEquals(1, responses.size());
        assertEquals("QuickBite Cafe", responses.get(0).name());

        when(authServiceClient.getUserSummary(any(), anyLong()))
                .thenReturn(new UserSummaryDto(
                        11L,
                        "Owner",
                        "owner@test.com",
                        "9999999999",
                        "RESTAURANT_OWNER",
                        "ACTIVE",
                        true
                ));
    }

    @Test
    @DisplayName("Search Restaurants - Blank Filters Return All Approved")
    void searchRestaurants_BlankFilters() {
        Restaurant zulu = cloneRestaurant(2L, "zulu cafe", ApprovalStatus.APPROVED, true, true);
        Restaurant alpha = cloneRestaurant(3L, "Alpha Bistro", ApprovalStatus.APPROVED, true, true);
        when(restaurantRepository.findByApprovalStatusAndIsApprovedTrue(ApprovalStatus.APPROVED))
            .thenReturn(List.of(zulu, alpha));

        List<RestaurantResponse> responses = restaurantService.searchRestaurants(" ", null, "");

        assertEquals(2, responses.size());
        assertEquals("Alpha Bistro", responses.get(0).name());
    }

    @Test
    @DisplayName("Find Nearby Restaurants - Returns Only Restaurants Within Effective Radius")
    void findNearbyRestaurants_FiltersByDistance() {
        Restaurant nearby = cloneRestaurant(1L, "Nearby Kitchen", ApprovalStatus.APPROVED, true, true);
        nearby.setLatitude(18.5205);
        nearby.setLongitude(73.8568);
        nearby.setDeliveryRadius(2.0);

        Restaurant tooFar = cloneRestaurant(2L, "Far Kitchen", ApprovalStatus.APPROVED, true, true);
        tooFar.setLatitude(19.0760);
        tooFar.setLongitude(72.8777);
        tooFar.setDeliveryRadius(20.0);

        Restaurant radiusTooSmall = cloneRestaurant(3L, "Short Radius", ApprovalStatus.APPROVED, true, true);
        radiusTooSmall.setLatitude(18.5300);
        radiusTooSmall.setLongitude(73.8700);
        radiusTooSmall.setDeliveryRadius(0.5);

        when(restaurantRepository.findByIsOpenTrueAndIsApprovedTrue())
            .thenReturn(List.of(tooFar, nearby, radiusTooSmall));

        List<RestaurantResponse> responses = restaurantService.findNearbyRestaurants(18.5204, 73.8567, 5.0);

        assertEquals(1, responses.size());
        assertEquals("Nearby Kitchen", responses.get(0).name());
    }

    @Test
    @DisplayName("Update Restaurant - Resets Workflow For Non Approved Restaurant")
    void updateRestaurant_ResetsWorkflowForNonApprovedRestaurant() {
        Restaurant pendingRestaurant = cloneRestaurant(4L, "Old Name", ApprovalStatus.REJECTED, false, false);
        pendingRestaurant.setRejectionReason("missing docs");
        pendingRestaurant.setReviewedByAdminId(5L);
        pendingRestaurant.setReviewedAt(LocalDateTime.now().minusDays(2));
        when(restaurantRepository.findById(4L)).thenReturn(Optional.of(pendingRestaurant));
        when(restaurantRepository.save(pendingRestaurant)).thenReturn(pendingRestaurant);

        RestaurantResponse response = restaurantService.updateRestaurant(4L, request);

        assertEquals("QuickBite Cafe", pendingRestaurant.getName());
        assertEquals(ApprovalStatus.PENDING, pendingRestaurant.getApprovalStatus());
        assertFalse(pendingRestaurant.getIsApproved());
        assertNull(pendingRestaurant.getRejectionReason());
        assertNull(pendingRestaurant.getReviewedByAdminId());
        assertNull(pendingRestaurant.getReviewedAt());
        assertEquals("PENDING", response.approvalStatus());
    }

    @Test
    @DisplayName("Update Restaurant - Keeps Approved Workflow Intact")
    void updateRestaurant_KeepsApprovedWorkflow() {
        Restaurant approvedRestaurant = cloneRestaurant(5L, "Approved Name", ApprovalStatus.APPROVED, true, true);
        approvedRestaurant.setReviewedByAdminId(7L);
        approvedRestaurant.setReviewedAt(LocalDateTime.now().minusDays(1));
        when(restaurantRepository.findById(5L)).thenReturn(Optional.of(approvedRestaurant));
        when(restaurantRepository.save(approvedRestaurant)).thenReturn(approvedRestaurant);

        restaurantService.updateRestaurant(5L, request);

        assertEquals(ApprovalStatus.APPROVED, approvedRestaurant.getApprovalStatus());
        assertTrue(approvedRestaurant.getIsApproved());
        assertEquals(7L, approvedRestaurant.getReviewedByAdminId());
    }

    @Test
    @DisplayName("Get Pending Restaurants - Includes Owner Details And Sorts Latest First")
    void getPendingRestaurants_Success() {
        Restaurant older = cloneRestaurant(6L, "Older", ApprovalStatus.PENDING, false, false);
        older.setSubmittedAt(LocalDateTime.now().minusDays(2));
        Restaurant latest = cloneRestaurant(7L, "Latest", ApprovalStatus.PENDING, false, false);
        latest.setSubmittedAt(LocalDateTime.now().minusHours(1));
        when(restaurantRepository.findByApprovalStatus(ApprovalStatus.PENDING)).thenReturn(List.of(older, latest));
        when(authServiceClient.getUserSummary(eq("RESTAURANT_OWNER"), eq(11L)))
            .thenReturn(new UserSummaryDto(11L, "Owner Name", "owner@test.com", "9999999999", "RESTAURANT_OWNER", "ACTIVE", true));

        List<AdminRestaurantResponse> responses = restaurantService.getPendingRestaurants();

        assertEquals(2, responses.size());
        assertEquals("Latest", responses.get(0).restaurantName());
        assertEquals("Owner Name", responses.get(0).ownerName());
    }

    @Test
    @DisplayName("Get All Restaurants For Admin - Falls Back When Owner Fetch Fails")
    void getAllRestaurantsForAdmin_FallbackWithoutOwner() {
        when(restaurantRepository.findAll()).thenReturn(List.of(restaurant));
        when(authServiceClient.getUserSummary("RESTAURANT_OWNER", 11L)).thenThrow(new RuntimeException("auth down"));

        List<AdminRestaurantResponse> responses = restaurantService.getAllRestaurantsForAdmin();

        assertEquals(1, responses.size());
        assertNull(responses.get(0).ownerName());
        assertNull(responses.get(0).ownerEmail());
    }

    @Test
    @DisplayName("Approve Restaurant - Marks Approved And Saves Reviewer")
    void approveRestaurant_Success() {
        Restaurant pendingRestaurant = cloneRestaurant(8L, "Pending", ApprovalStatus.PENDING, false, false);
        when(restaurantRepository.findById(8L)).thenReturn(Optional.of(pendingRestaurant));
        when(restaurantRepository.save(pendingRestaurant)).thenReturn(pendingRestaurant);

        RestaurantResponse response = restaurantService.approveRestaurant(8L, 100L);

        assertTrue(pendingRestaurant.getIsApproved());
        assertEquals(ApprovalStatus.APPROVED, pendingRestaurant.getApprovalStatus());
        assertEquals(100L, pendingRestaurant.getReviewedByAdminId());
        assertNull(pendingRestaurant.getRejectionReason());
        assertEquals("APPROVED", response.approvalStatus());
    }

    @Test
    @DisplayName("Reject Restaurant - Marks Rejected And Closed")
    void rejectRestaurant_Success() {
        Restaurant pendingRestaurant = cloneRestaurant(9L, "Pending", ApprovalStatus.PENDING, true, false);
        when(restaurantRepository.findById(9L)).thenReturn(Optional.of(pendingRestaurant));
        when(restaurantRepository.save(pendingRestaurant)).thenReturn(pendingRestaurant);

        RestaurantResponse response = restaurantService.rejectRestaurant(9L, 200L, "documents missing");

        assertFalse(pendingRestaurant.getIsApproved());
        assertFalse(pendingRestaurant.getIsOpen());
        assertEquals(ApprovalStatus.REJECTED, pendingRestaurant.getApprovalStatus());
        assertEquals("documents missing", pendingRestaurant.getRejectionReason());
        assertEquals(200L, pendingRestaurant.getReviewedByAdminId());
        assertEquals("REJECTED", response.approvalStatus());
    }

    @Test
    @DisplayName("Toggle Restaurant Status - Updates Open Flag")
    void toggleRestaurantStatus_Success() {
        Restaurant closedRestaurant = cloneRestaurant(10L, "Closed", ApprovalStatus.APPROVED, false, true);
        when(restaurantRepository.findById(10L)).thenReturn(Optional.of(closedRestaurant));
        when(restaurantRepository.save(closedRestaurant)).thenReturn(closedRestaurant);

        RestaurantResponse response = restaurantService.toggleRestaurantStatus(10L, true);

        assertTrue(closedRestaurant.getIsOpen());
        assertTrue(response.isOpen());
    }

    @Test
    @DisplayName("Update Average Rating - Saves Valid Rating")
    void updateAverageRating_Success() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.save(restaurant)).thenReturn(restaurant);

        RestaurantResponse response = restaurantService.updateAverageRating(1L, 4.9);

        assertEquals(4.9, restaurant.getAvgRating());
        assertEquals(4.9, response.avgRating());
    }

    @Test
    @DisplayName("Update Average Rating - Rejects Invalid Rating")
    void updateAverageRating_Invalid() {
        assertThrows(BadRequestException.class, () -> restaurantService.updateAverageRating(1L, 5.6));

        verify(restaurantRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Delete Restaurant - Deletes Existing Restaurant")
    void deleteRestaurant_Success() {
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        restaurantService.deleteRestaurant(1L);

        verify(restaurantRepository).delete(restaurant);
    }

    private Restaurant cloneRestaurant(Long restaurantId, String name, ApprovalStatus approvalStatus, boolean isOpen, boolean isApproved) {
        return Restaurant.builder()
            .restaurantId(restaurantId)
            .ownerId(11L)
            .name(name)
            .description("All day dining")
            .cuisine("Indian")
            .address("12 Main Street")
            .city("Pune")
            .latitude(18.5204)
            .longitude(73.8567)
            .phone("9876543210")
            .avgRating(4.4)
            .deliveryRadius(6.5)
            .isOpen(isOpen)
            .isApproved(isApproved)
            .approvalStatus(approvalStatus)
            .submittedAt(LocalDateTime.now())
            .minOrderAmount(150)
            .estimatedDeliveryMin(35)
            .build();
    }
}
