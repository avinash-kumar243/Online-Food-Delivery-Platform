package com.quickbite.payment.gateway;

import java.math.BigDecimal;

import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.quickbite.payment.config.RazorpayConfig;
import com.quickbite.payment.exception.PaymentException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Refund;
import com.razorpay.Utils;

@Component
public class RazorpayGateway {

    private final RazorpayConfig razorpayConfig;

    public RazorpayGateway(RazorpayConfig razorpayConfig) {
        this.razorpayConfig = razorpayConfig;
    }

    public Order createOrder(BigDecimal amount, String currency, String receipt) {
        try {
            JSONObject options = new JSONObject();
            options.put("amount", toPaise(amount));
            options.put("currency", currency);
            options.put("receipt", receipt);

            return buildClient().orders.create(options);
        } catch (RazorpayException exception) {
            throw new PaymentException("Unable to create Razorpay order");
        }
    }

    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);
            return Utils.verifyPaymentSignature(attributes, razorpayConfig.getKeySecret());
        } catch (Exception exception) {
            throw new PaymentException("Unable to verify Razorpay payment signature");
        }
    }

    public Refund refundPayment(String razorpayPaymentId, BigDecimal amount) {
        try {
            JSONObject options = new JSONObject();
            options.put("amount", toPaise(amount));
            return buildClient().payments.refund(razorpayPaymentId, options);
        } catch (RazorpayException exception) {
            throw new PaymentException("Unable to initiate Razorpay refund");
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            return Utils.verifyWebhookSignature(payload, signature, razorpayConfig.getWebhookSecret());
        } catch (Exception exception) {
            throw new PaymentException("Invalid Razorpay webhook signature");
        }
    }

    public String getKeyId() {
        return razorpayConfig.getKeyId();
    }

    private RazorpayClient buildClient() throws RazorpayException {
        if (!StringUtils.hasText(razorpayConfig.getKeyId()) || !StringUtils.hasText(razorpayConfig.getKeySecret())) {
            throw new PaymentException("Razorpay credentials are not configured");
        }
        return new RazorpayClient(razorpayConfig.getKeyId(), razorpayConfig.getKeySecret());
    }

    private long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
