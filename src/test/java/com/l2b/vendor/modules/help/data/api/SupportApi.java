package com.l2b.vendor.modules.help.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger tag Support. */
public class SupportApi {

    public static final String CONFIG = "/api/v1/support/config";
    public static final String TICKETS = "/api/v1/support/tickets";

    private final HttpClient http;

    public SupportApi() {
        this(new HttpClient());
    }

    public SupportApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET support config")
    public Response config(String token) {
        return http.get(CONFIG, token);
    }
}
