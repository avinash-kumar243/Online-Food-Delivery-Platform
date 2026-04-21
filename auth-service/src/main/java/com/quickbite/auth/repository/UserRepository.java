package com.quickbite.auth.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.auth.entity.User;

public interface UserRepository extends JpaRepository<User, Long>{
	Optional<User> findByUserId(Long userId);
	Optional<User> findByPhone(String phone);
	Optional<User> findByEmail(String email);
	boolean existsByEmail(String email);
	boolean existsByPhone(String phone);
	List<User> findAllByRole(String role); 
	User deleteByUserId(Long userId);
	 
}  