package com.l2b.vendor.modules.home.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger: Rentals Vendor dashboard/stats + Materials vendor dashboard. */
public class HomeApi {

    public static final String RENTAL_DASHBOARD = "/api/v1/rentals/vendor/dashboard";
    public static final String RENTAL_STATS = "/api/v1/rentals/vendor/stats";
    public static final String MATERIAL_DASHBOARD = "/api/v1/materials/vendor/dashboard";
    public static final String MATERIAL_STATS = "/api/v1/materials/vendor/stats";

    private final HttpClient http;

    public HomeApi() {
        this(new HttpClient());
    }

    public HomeApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET rental vendor dashboard")
    public Response rentalDashboard(String token) {
        return http.get(RENTAL_DASHBOARD, token);
    }

    @Step("GET rental vendor stats")
    public Response rentalStats(String token) {
        return http.get(RENTAL_STATS, token);
    }
}
