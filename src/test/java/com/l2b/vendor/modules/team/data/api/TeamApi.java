package com.l2b.vendor.modules.team.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger tag Team. */
public class TeamApi {

    public static final String LIST = "/api/v1/rentals/vendor/team";

    private final HttpClient http;

    public TeamApi() {
        this(new HttpClient());
    }

    public TeamApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET vendor team")
    public Response list(String token) {
        return http.get(LIST, token);
    }
}
