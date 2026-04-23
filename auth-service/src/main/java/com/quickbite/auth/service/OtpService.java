package com.quickbite.auth.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final Map<String, OtpData> otpStore = new ConcurrentHashMap<>();
    private static final long OTP_VALIDITY_MS = 60 * 1000;

    public String generateOtp(String email) {
        OtpData existing = otpStore.get(email);

        if (existing != null && System.currentTimeMillis() - existing.timestamp < OTP_VALIDITY_MS) {
            return existing.otp;
        }

        String otp = String.format("%06d", new Random().nextInt(1000000));
        otpStore.put(email, new OtpData(otp, System.currentTimeMillis()));
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        OtpData data = otpStore.get(email);

        if (data == null) {
            return false;
        }

        if (System.currentTimeMillis() - data.timestamp > OTP_VALIDITY_MS) {
            otpStore.remove(email);
            return false;
        }

        if (!data.otp.equals(otp)) {
            return false;
        }

        otpStore.remove(email);
        return true;
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