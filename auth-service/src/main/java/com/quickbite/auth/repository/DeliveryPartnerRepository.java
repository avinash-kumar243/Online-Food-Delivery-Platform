package com.quickbite.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.auth.entity.DeliveryPartner;

public interface DeliveryPartnerRepository extends JpaRepository<DeliveryPartner, Long>{
	Optional<DeliveryPartner> findByPartnerId(Long partnerId);
	Optional<DeliveryPartner> findByPhone(String phone);
	Optional<DeliveryPartner> findByEmail(String email);
	boolean existsByEmail(String email);
	boolean existsByPhone(String phone);
	DeliveryPartner deleteByPartnerId(Long partnerId);
	 
}

