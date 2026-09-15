package com.l2b.vendor.modules.settings.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger tags Account + App Settings. */
public class AccountApi {

    public static final String PROFILE = "/api/v1/account/profile";
    public static final String LANGUAGE = "/api/v1/account/language";
    public static final String KYC = "/api/v1/account/kyc";
    public static final String PERMISSIONS = "/api/v1/app-settings/permissions";

    private final HttpClient http;

    public AccountApi() {
        this(new HttpClient());
    }

    public AccountApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET vendor profile")
    public Response profile(String token) {
        return http.get(PROFILE, token);
    }
}
