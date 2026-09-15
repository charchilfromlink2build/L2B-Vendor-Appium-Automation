package com.l2b.vendor.modules.onboarding.data.dto;

/**
 * POST /api/v1/auth/send-otp and /resend-otp body.
 * Swagger: {@code SendOtpRequest} — {@code phone} required, {@code phone_country_code} default +91.
 */
public class SendOtpRequest {

    public String phone_country_code = "+91";
    public String phone;

    public SendOtpRequest() {
    }

    public SendOtpRequest(String phone) {
        this.phone = phone;
    }
}
