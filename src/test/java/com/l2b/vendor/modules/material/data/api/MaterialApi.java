package com.l2b.vendor.modules.material.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger tags Materials + Materials Delivery. */
public class MaterialApi {

    public static final String INVENTORY = "/api/v1/materials/inventory";
    public static final String ORDERS = "/api/v1/materials/orders";
    public static final String SUB_ORDERS = "/api/v1/materials/sub-orders";

    private final HttpClient http;

    public MaterialApi() {
        this(new HttpClient());
    }

    public MaterialApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET material inventory")
    public Response inventory(String token) {
        return http.get(INVENTORY, token);
    }

    @Step("GET material orders")
    public Response orders(String token) {
        return http.get(ORDERS, token);
    }
}
