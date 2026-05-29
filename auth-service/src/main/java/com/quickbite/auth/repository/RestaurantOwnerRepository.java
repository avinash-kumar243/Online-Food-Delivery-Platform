package com.quickbite.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.auth.entity.RestaurantOwner;

public interface RestaurantOwnerRepository extends JpaRepository<RestaurantOwner, Long>{
	Optional<RestaurantOwner> findByOwnerId(Long ownerId);
	Optional<RestaurantOwner> findByPhone(String phone);
	Optional<RestaurantOwner> findByEmail(String email);
	boolean existsByEmail(String email);
	boolean existsByPhone(String phone);
	RestaurantOwner deleteByOwnerId(Long ownerId);
	 
}

