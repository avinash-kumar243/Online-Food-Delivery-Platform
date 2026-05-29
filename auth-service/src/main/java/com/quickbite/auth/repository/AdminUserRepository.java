package com.quickbite.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.auth.entity.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByAdminId(Long adminId);

    Optional<AdminUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
