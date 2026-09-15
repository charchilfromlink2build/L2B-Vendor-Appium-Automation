package com.l2b.vendor.modules.operator.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger tags Rentals Operator + Operator Individual. */
public class OperatorApi {

    public static final String DASHBOARD = "/api/v1/rentals/operator/dashboard";
    public static final String BOOKINGS = "/api/v1/rentals/operator/bookings";
    public static final String SCHEDULE = "/api/v1/rentals/operator/schedule";

    private final HttpClient http;

    public OperatorApi() {
        this(new HttpClient());
    }

    public OperatorApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET operator dashboard")
    public Response dashboard(String token) {
        return http.get(DASHBOARD, token);
    }
}
