package com.quickbite.payment.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.quickbite.payment.client.OrderServiceClient;
import com.quickbite.payment.dto.PaymentResponse;
import com.quickbite.payment.dto.WalletBalanceResponse;
import com.quickbite.payment.dto.WalletPaymentRequest;
import com.quickbite.payment.dto.WalletStatementResponse;
import com.quickbite.payment.dto.WalletTopUpRequest;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.Wallet;
import com.quickbite.payment.entity.WalletStatement;
import com.quickbite.payment.enums.PaymentMode;
import com.quickbite.payment.enums.PaymentStatus;
import com.quickbite.payment.enums.WalletTransactionType;
import com.quickbite.payment.exception.PaymentException;
import com.quickbite.payment.repository.PaymentRepository;
import com.quickbite.payment.repository.WalletRepository;
import com.quickbite.payment.repository.WalletStatementRepository;
import com.quickbite.payment.service.WalletService;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletStatementRepository walletStatementRepository;
    private final PaymentRepository paymentRepository;
    private final OrderServiceClient orderServiceClient;

    public WalletServiceImpl(WalletRepository walletRepository,
                             WalletStatementRepository walletStatementRepository,
                             PaymentRepository paymentRepository,
                             OrderServiceClient orderServiceClient) {
        this.walletRepository = walletRepository;
        this.walletStatementRepository = walletStatementRepository;
        this.paymentRepository = paymentRepository;
        this.orderServiceClient = orderServiceClient;
    }

    @Override
    @Transactional
    public WalletBalanceResponse addMoney(WalletTopUpRequest request) {
        Wallet wallet = getOrCreateWallet(request.customerId());
        wallet.setBalance(wallet.getBalance().add(request.amount()));
        Wallet savedWallet = walletRepository.save(wallet);

        createStatement(
            savedWallet.getWalletId(),
            savedWallet.getCustomerId(),
            request.amount(),
            WalletTransactionType.CREDIT,
            request.description(),
            buildReference("TOPUP")
        );

        return mapToWalletBalance(savedWallet);
    }

    @Override
    @Transactional
    public PaymentResponse payFromWallet(WalletPaymentRequest request) {
        Wallet wallet = getOrCreateWallet(request.customerId());
        if (wallet.getBalance().compareTo(request.amount()) < 0) {
            throw new PaymentException("Insufficient wallet balance");
        }

        wallet.setBalance(wallet.getBalance().subtract(request.amount()));
        Wallet savedWallet = walletRepository.save(wallet);

        String referenceId = "WALLET-PAY-" + request.orderId() + "-" + UUID.randomUUID();
        createStatement(
            savedWallet.getWalletId(),
            savedWallet.getCustomerId(),
            request.amount(),
            WalletTransactionType.DEBIT,
            "Wallet payment for order " + request.orderId(),
            referenceId
        );

        Payment payment = paymentRepository.findByOrderId(request.orderId())
            .map(existingPayment -> updateExistingWalletPayment(existingPayment, request, referenceId))
            .orElseGet(() -> buildWalletPayment(request, referenceId));

        Payment savedPayment = paymentRepository.save(payment);
        orderServiceClient.updateOrderPaymentStatus(savedPayment.getOrderId(), savedPayment.getStatus().name());
        return mapToPaymentResponse(savedPayment);
    }

    @Override
    public WalletBalanceResponse getBalance(Long customerId) {
        Wallet wallet = getOrCreateWallet(customerId);
        return mapToWalletBalance(wallet);
    }

    @Override
    public List<WalletStatementResponse> getStatements(Long customerId) {
        return walletStatementRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
            .map(this::mapToStatementResponse)
            .toList();
    }

    @Override
    @Transactional
    public void creditRefund(Long customerId, BigDecimal amount, String description, String referenceId) {
        Wallet wallet = getOrCreateWallet(customerId);
        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet savedWallet = walletRepository.save(wallet);

        createStatement(
            savedWallet.getWalletId(),
            savedWallet.getCustomerId(),
            amount,
            WalletTransactionType.REFUND,
            description,
            referenceId
        );
    }

    private Payment buildWalletPayment(WalletPaymentRequest request, String referenceId) {
        LocalDateTime now = LocalDateTime.now();
        return Payment.builder()
            .orderId(request.orderId())
            .customerId(request.customerId())
            .amount(request.amount())
            .status(PaymentStatus.PAID)
            .mode(PaymentMode.WALLET)
            .transactionId(referenceId)
            .currency("INR")
            .paidAt(now)
            .build();
    }

    private Payment updateExistingWalletPayment(Payment payment, WalletPaymentRequest request, String referenceId) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new PaymentException("Payment is already completed for this order");
        }
        payment.setCustomerId(request.customerId());
        payment.setAmount(request.amount());
        payment.setStatus(PaymentStatus.PAID);
        payment.setMode(PaymentMode.WALLET);
        payment.setTransactionId(referenceId);
        payment.setCurrency("INR");
        payment.setPaidAt(LocalDateTime.now());
        payment.setRefundedAt(null);
        return payment;
    }

    private Wallet getOrCreateWallet(Long customerId) {
        return walletRepository.findByCustomerId(customerId)
            .orElseGet(() -> walletRepository.save(Wallet.builder()
                .customerId(customerId)
                .balance(BigDecimal.ZERO)
                .build()));
    }

    private void createStatement(Long walletId,
                                 Long customerId,
                                 BigDecimal amount,
                                 WalletTransactionType transactionType,
                                 String description,
                                 String referenceId) {
        walletStatementRepository.save(WalletStatement.builder()
            .walletId(walletId)
            .customerId(customerId)
            .amount(amount)
            .transactionType(transactionType)
            .description(description)
            .referenceId(referenceId)
            .build());
    }

    private String buildReference(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    private WalletBalanceResponse mapToWalletBalance(Wallet wallet) {
        return new WalletBalanceResponse(
            wallet.getWalletId(),
            wallet.getCustomerId(),
            wallet.getBalance(),
            wallet.getUpdatedAt()
        );
    }

    private WalletStatementResponse mapToStatementResponse(WalletStatement statement) {
        return new WalletStatementResponse(
            statement.getStatementId(),
            statement.getWalletId(),
            statement.getCustomerId(),
            statement.getAmount(),
            statement.getTransactionType(),
            statement.getDescription(),
            statement.getReferenceId(),
            statement.getCreatedAt()
        );
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return new PaymentResponse(
            payment.getPaymentId(),
            payment.getOrderId(),
            payment.getCustomerId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getMode(),
            payment.getTransactionId(),
            payment.getRazorpayOrderId(),
            payment.getRazorpayPaymentId(),
            payment.getCurrency(),
            payment.getPaidAt(),
            payment.getRefundedAt(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}
