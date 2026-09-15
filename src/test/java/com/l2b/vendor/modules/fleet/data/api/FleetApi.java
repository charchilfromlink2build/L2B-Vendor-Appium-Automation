package com.l2b.vendor.modules.fleet.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger tag Fleet + Machinery catalog. */
public class FleetApi {

    public static final String LIST = "/api/v1/rentals/vendor/fleet";
    public static final String ADD_MACHINES = "/api/v1/rentals/vendor/fleet/add-machines";
    public static final String CATALOG = "/api/v1/machinery/catalog";

    private final HttpClient http;

    public FleetApi() {
        this(new HttpClient());
    }

    public FleetApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET vendor fleet")
    public Response list(String token) {
        return http.get(LIST, token);
    }
}
