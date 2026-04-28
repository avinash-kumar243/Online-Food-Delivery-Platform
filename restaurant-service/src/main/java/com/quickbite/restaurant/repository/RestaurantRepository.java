package com.quickbite.restaurant.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.restaurant.entity.ApprovalStatus;
import com.quickbite.restaurant.entity.Restaurant;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    List<Restaurant> findByOwnerId(Long ownerId);

    java.util.Optional<Restaurant> findFirstByOwnerIdOrderByRestaurantIdAsc(Long ownerId);

    List<Restaurant> findByCuisine(String cuisine);

    List<Restaurant> findByCity(String city);

    List<Restaurant> findByIsOpenTrueAndIsApprovedTrue();

    List<Restaurant> findByApprovalStatus(ApprovalStatus approvalStatus);

    List<Restaurant> findByApprovalStatusAndIsApprovedTrue(ApprovalStatus approvalStatus);

    List<Restaurant> findByNameContaining(String name);
}
