package com.quickbite.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.auth.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long>{
	Optional<Customer> findByCustomerId(Long customerId);
	Optional<Customer> findByPhone(String phone);
	Optional<Customer> findByEmail(String email);
	boolean existsByEmail(String email);
	boolean existsByPhone(String phone);
	Customer deleteByCustomerId(Long customerId);
	 
}

