package com.l2b.vendor.modules.onboarding.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger tag Onboarding — status + role / KYC / machines after login. */
public class OnboardingApi {

    public static final String STATUS = "/api/v1/onboarding/status";
    public static final String STATUS_SUMMARY = "/api/v1/onboarding/status-summary";
    public static final String ROLE_CATEGORY = "/api/v1/onboarding/role-category";
    public static final String VENDOR_BASIC_INFO = "/api/v1/onboarding/vendor/basic-info";
    public static final String VENDOR_MACHINES = "/api/v1/onboarding/vendor/machines";
    public static final String VENDOR_KYC = "/api/v1/onboarding/vendor/kyc";
    public static final String VENDOR_COMPANY_KYC = "/api/v1/onboarding/vendor/company-kyc";
    public static final String COMPLETE = "/api/v1/onboarding/complete";

    private final HttpClient http;

    public OnboardingApi() {
        this(new HttpClient());
    }

    public OnboardingApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET onboarding status")
    public Response status(String accessToken) {
        return http.get(STATUS, accessToken);
    }

    @Step("GET onboarding status-summary")
    public Response statusSummary(String accessToken) {
        return http.get(STATUS_SUMMARY, accessToken);
    }
}
