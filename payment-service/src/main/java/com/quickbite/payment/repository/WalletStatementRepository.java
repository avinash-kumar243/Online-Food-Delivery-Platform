package com.quickbite.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.quickbite.payment.entity.WalletStatement;

public interface WalletStatementRepository extends JpaRepository<WalletStatement, Long> {

    List<WalletStatement> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<WalletStatement> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
