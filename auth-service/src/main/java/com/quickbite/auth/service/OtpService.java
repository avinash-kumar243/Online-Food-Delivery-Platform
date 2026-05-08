package com.quickbite.auth.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_VALIDITY_MS = 60 * 1000;

    public String generateOtp(String email) {
        String otp = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        otpStore.put(email, new OtpData(otp, System.currentTimeMillis()));
        return otp;
    }

    public OtpVerificationResult verifyOtp(String email, String otp) {
        OtpData data = otpStore.get(email);

        if (data == null) {
            return OtpVerificationResult.EXPIRED;
        }

        if (System.currentTimeMillis() - data.timestamp > OTP_VALIDITY_MS) {
            otpStore.remove(email);
            return OtpVerificationResult.EXPIRED;
        }

        if (!data.otp.equals(otp)) {
            return OtpVerificationResult.INVALID;
        }

        otpStore.remove(email);
        return OtpVerificationResult.VALID;
    }

    public enum OtpVerificationResult {
        VALID,
        INVALID,
        EXPIRED
    }

    private static class OtpData {
        String otp;
        long timestamp;

        OtpData(String otp, long timestamp) {
            this.otp = otp;
            this.timestamp = timestamp;
        }
    }
}
