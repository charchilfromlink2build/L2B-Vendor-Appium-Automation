package com.l2b.vendor.modules.calendar.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger: GET /api/v1/rentals/vendor/schedule */
public class CalendarApi {

    public static final String SCHEDULE = "/api/v1/rentals/vendor/schedule";

    private final HttpClient http;

    public CalendarApi() {
        this(new HttpClient());
    }

    public CalendarApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET vendor schedule")
    public Response schedule(String token) {
        return http.get(SCHEDULE, token);
    }
}
