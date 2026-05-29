package com.quickbite.payment.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.quickbite.payment.client.OrderServiceClient;
import com.quickbite.payment.dto.OrderPaymentStatusRequest;
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

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletStatementRepository walletStatementRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void addMoney_UpdatesBalanceAndCreatesStatement() {
        Wallet wallet = wallet(1L, 99L, new BigDecimal("100.00"));
        when(walletRepository.findByCustomerId(99L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        WalletBalanceResponse response = walletService.addMoney(new WalletTopUpRequest(99L, new BigDecimal("50.00"), "Top up"));

        assertEquals(new BigDecimal("150.00"), response.balance());
        verify(walletStatementRepository).save(any(WalletStatement.class));
    }

    @Test
    void payFromWallet_CreatesPaidPaymentAndUpdatesOrderStatus() {
        Wallet wallet = wallet(1L, 99L, new BigDecimal("500.00"));
        when(walletRepository.findByCustomerId(99L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(paymentRepository.findByOrderId(77L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = walletService.payFromWallet(new WalletPaymentRequest(77L, 99L, new BigDecimal("250.00")));

        assertEquals(new BigDecimal("250.00"), response.amount());
        assertEquals(PaymentStatus.PAID, response.status());
        assertEquals(PaymentMode.WALLET, response.mode());
        assertNotNull(response.transactionId());
        verify(orderServiceClient).updateOrderPaymentStatus(eq(77L), any(OrderPaymentStatusRequest.class));
        verify(walletStatementRepository).save(any(WalletStatement.class));
    }

    @Test
    void payFromWallet_RejectsInsufficientBalance() {
        Wallet wallet = wallet(1L, 99L, new BigDecimal("40.00"));
        when(walletRepository.findByCustomerId(99L)).thenReturn(Optional.of(wallet));

        PaymentException exception = assertThrows(PaymentException.class,
            () -> walletService.payFromWallet(new WalletPaymentRequest(77L, 99L, new BigDecimal("250.00"))));

        assertEquals("Insufficient wallet balance", exception.getMessage());
    }

    @Test
    void payFromWallet_RejectsAlreadyPaidExistingPayment() {
        Wallet wallet = wallet(1L, 99L, new BigDecimal("500.00"));
        Payment payment = Payment.builder()
            .paymentId(8L)
            .orderId(77L)
            .customerId(99L)
            .amount(new BigDecimal("250.00"))
            .status(PaymentStatus.PAID)
            .mode(PaymentMode.CARD)
            .currency("INR")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        when(walletRepository.findByCustomerId(99L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(paymentRepository.findByOrderId(77L)).thenReturn(Optional.of(payment));

        PaymentException exception = assertThrows(PaymentException.class,
            () -> walletService.payFromWallet(new WalletPaymentRequest(77L, 99L, new BigDecimal("250.00"))));

        assertEquals("Payment is already completed for this order", exception.getMessage());
    }

    @Test
    void getBalance_CreatesWalletWhenMissing() {
        Wallet wallet = wallet(2L, 100L, BigDecimal.ZERO);
        when(walletRepository.findByCustomerId(100L)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);

        WalletBalanceResponse response = walletService.getBalance(100L);

        assertEquals(100L, response.customerId());
        assertEquals(BigDecimal.ZERO, response.balance());
    }

    @Test
    void getStatements_MapsRepositoryResults() {
        WalletStatement statement = WalletStatement.builder()
            .statementId(1L)
            .walletId(2L)
            .customerId(100L)
            .amount(new BigDecimal("75.00"))
            .transactionType(WalletTransactionType.CREDIT)
            .description("Top up")
            .referenceId("TOPUP-1")
            .createdAt(LocalDateTime.now())
            .build();
        when(walletStatementRepository.findByCustomerIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(statement));

        List<WalletStatementResponse> responses = walletService.getStatements(100L);

        assertEquals(1, responses.size());
        assertEquals("TOPUP-1", responses.get(0).referenceId());
    }

    @Test
    void creditRefund_UpdatesWalletAndCreatesRefundStatement() {
        Wallet wallet = wallet(1L, 99L, new BigDecimal("100.00"));
        when(walletRepository.findByCustomerId(99L)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(wallet)).thenReturn(wallet);

        walletService.creditRefund(99L, new BigDecimal("25.00"), "Refund", "REF-1");

        assertEquals(new BigDecimal("125.00"), wallet.getBalance());
        verify(walletStatementRepository).save(any(WalletStatement.class));
    }

    private Wallet wallet(Long walletId, Long customerId, BigDecimal balance) {
        return Wallet.builder()
            .walletId(walletId)
            .customerId(customerId)
            .balance(balance)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }
}
