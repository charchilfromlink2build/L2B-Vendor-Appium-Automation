package com.l2b.vendor.modules.earning.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger tags Wallet + Earnings (+ material vendor earnings). */
public class EarningApi {

    public static final String WALLET = "/api/v1/wallet";
    public static final String SUMMARY = "/api/v1/wallet/earning-summary";
    public static final String TRANSACTIONS = "/api/v1/wallet/transactions";
    public static final String EARNINGS = "/api/v1/wallet/earnings";
    public static final String MATERIAL_EARNINGS = "/api/v1/materials/vendor/earnings";

    private final HttpClient http;

    public EarningApi() {
        this(new HttpClient());
    }

    public EarningApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET wallet")
    public Response wallet(String token) {
        return http.get(WALLET, token);
    }

    @Step("GET wallet earning-summary")
    public Response earningSummary(String token) {
        return http.get(SUMMARY, token);
    }
}
