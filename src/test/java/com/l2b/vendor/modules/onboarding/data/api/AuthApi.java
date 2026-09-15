package com.l2b.vendor.modules.onboarding.data.api;

import com.l2b.vendor.core.api.HttpClient;
import com.l2b.vendor.modules.onboarding.data.dto.SendOtpRequest;
import com.l2b.vendor.modules.onboarding.data.dto.VerifyOtpRequest;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger tag Authentication — vendor OTP login. */
public class AuthApi {

    public static final String SEND_OTP = "/api/v1/auth/send-otp";
    public static final String VERIFY_OTP = "/api/v1/auth/verify-otp";
    public static final String RESEND_OTP = "/api/v1/auth/resend-otp";
    public static final String REFRESH = "/api/v1/auth/refresh";
    public static final String LOGOUT = "/api/v1/auth/logout";
    public static final String LOGOUT_ALL = "/api/v1/auth/logout-all";

    private final HttpClient http;

    public AuthApi() {
        this(new HttpClient());
    }

    public AuthApi(HttpClient http) {
        this.http = http;
    }

    @Step("POST send-otp")
    public Response sendOtp(String phone) {
        return http.post(SEND_OTP, new SendOtpRequest(phone));
    }

    @Step("POST verify-otp")
    public Response verifyOtp(String phone, String otpCode) {
        return http.post(VERIFY_OTP, new VerifyOtpRequest(phone, otpCode));
    }

    @Step("POST resend-otp")
    public Response resendOtp(String phone) {
        return http.post(RESEND_OTP, new SendOtpRequest(phone));
    }

    @Step("POST logout")
    public Response logout(String accessToken) {
        return http.post(LOGOUT, java.util.Map.of(), accessToken);
    }
}
