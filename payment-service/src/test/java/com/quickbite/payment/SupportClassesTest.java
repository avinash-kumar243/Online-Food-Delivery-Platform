package com.quickbite.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.quickbite.payment.aspect.LoggingAspect;
import com.quickbite.payment.dto.ApiResponse;
import com.quickbite.payment.entity.Payment;
import com.quickbite.payment.entity.Wallet;
import com.quickbite.payment.entity.WalletStatement;
import com.quickbite.payment.enums.PaymentMode;
import com.quickbite.payment.enums.PaymentStatus;
import com.quickbite.payment.enums.WalletTransactionType;
import com.quickbite.payment.exception.GlobalExceptionHandler;
import com.quickbite.payment.exception.PaymentException;
import com.quickbite.payment.exception.ResourceNotFoundException;
import com.quickbite.payment.messaging.GenericEventPublisher;
import com.quickbite.payment.messaging.QuickbiteOrderMessagingConstants;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@ExtendWith(MockitoExtension.class)
class SupportClassesTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    @Test
    void apiResponseFactoriesBuildSuccessAndFailureResponses() {
        ApiResponse<String> success = ApiResponse.success("ok", "data");
        ApiResponse<Object> failure = ApiResponse.failure("bad");

        assertEquals("ok", success.message());
        assertEquals("data", success.data());
        assertNotNull(success.timestamp());
        assertEquals("bad", failure.message());
        assertEquals(null, failure.data());
    }

    @Test
    void genericEventPublisher_UsesExpectedExchange() {
        GenericEventPublisher publisher = new GenericEventPublisher(rabbitTemplate);

        publisher.send("payment.success", "payload");

        verify(rabbitTemplate).convertAndSend(QuickbiteOrderMessagingConstants.ORDER_EXCHANGE, "payment.success", "payload");
    }

    @Test
    void loggingAspect_ReturnsProceedResult() throws Throwable {
        LoggingAspect aspect = new LoggingAspect();
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.quickbite.payment.TestService");
        when(signature.getName()).thenReturn("process");
        when(joinPoint.proceed()).thenReturn("done");

        Object result = aspect.logMethodExecution(joinPoint);

        assertSame("done", result);
    }

    @Test
    void loggingAspect_RethrowsExceptions() throws Throwable {
        LoggingAspect aspect = new LoggingAspect();
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getDeclaringTypeName()).thenReturn("com.quickbite.payment.TestService");
        when(signature.getName()).thenReturn("process");
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> aspect.logMethodExecution(joinPoint));

        assertEquals("boom", exception.getMessage());
    }

    @Test
    void entityLifecycleCallbacksPopulateDefaults() {
        Payment payment = Payment.builder()
            .orderId(1L)
            .customerId(2L)
            .amount(new BigDecimal("25.00"))
            .status(PaymentStatus.PENDING)
            .mode(PaymentMode.CARD)
            .build();
        payment.prePersist();
        payment.setCurrency(" ");
        payment.preUpdate();

        Wallet wallet = Wallet.builder()
            .customerId(10L)
            .build();
        wallet.prePersist();
        wallet.setBalance(null);
        wallet.preUpdate();

        WalletStatement statement = WalletStatement.builder()
            .walletId(1L)
            .customerId(10L)
            .amount(new BigDecimal("10.00"))
            .transactionType(WalletTransactionType.CREDIT)
            .description("Top up")
            .build();
        statement.prePersist();

        assertEquals("INR", payment.getCurrency());
        assertNotNull(payment.getCreatedAt());
        assertNotNull(payment.getUpdatedAt());
        assertEquals(BigDecimal.ZERO, wallet.getBalance());
        assertNotNull(wallet.getCreatedAt());
        assertNotNull(statement.getCreatedAt());
    }

    @Test
    void globalExceptionHandler_MapsKnownExceptionsToExpectedStatuses() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var notFound = handler.handleResourceNotFound(new ResourceNotFoundException("Missing"));
        var badRequest = handler.handlePaymentException(new PaymentException("Bad payment"));
        var invalidRequest = handler.handleBadRequest(new HttpMessageNotReadableException("bad request"));
        var generic = handler.handleGenericException(new IllegalStateException("boom"));

        assertEquals(HttpStatus.NOT_FOUND, notFound.getStatusCode());
        assertEquals("Missing", notFound.getBody().message());
        assertEquals(HttpStatus.BAD_REQUEST, badRequest.getStatusCode());
        assertEquals("Bad payment", badRequest.getBody().message());
        assertEquals("Invalid request", invalidRequest.getBody().message());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, generic.getStatusCode());
    }

    @Test
    void globalExceptionHandler_ReturnsConstraintViolationMessage() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        ConstraintViolation<?> violation = org.mockito.Mockito.mock(ConstraintViolation.class);

        var response = handler.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody().message());
    }
}
