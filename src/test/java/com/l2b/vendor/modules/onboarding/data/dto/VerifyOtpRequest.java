package com.l2b.vendor.modules.onboarding.data.dto;

/**
 * POST /api/v1/auth/verify-otp body.
 * Swagger: {@code VerifyOtpRequest} — {@code phone} + {@code otp_code} required.
 */
public class VerifyOtpRequest {

    public String phone_country_code = "+91";
    public String phone;
    public String otp_code;
    public String fcm_token;

    public VerifyOtpRequest() {
    }

    public VerifyOtpRequest(String phone, String otpCode) {
        this.phone = phone;
        this.otp_code = otpCode;
    }
}
