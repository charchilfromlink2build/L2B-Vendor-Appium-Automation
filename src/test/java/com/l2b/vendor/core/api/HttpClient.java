package com.l2b.vendor.core.api;

import static io.restassured.RestAssured.given;

import com.l2b.vendor.environment.Config;
import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

/**
 * Shared RestAssured client for QA ({@code https://qa.waardian.com}).
 * Feature modules own request/response types; this class only does HTTP.
 */
public class HttpClient {

    private final String baseUrl;

    public HttpClient() {
        this(Config.get("api.base.url"));
    }

    public HttpClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Step("GET QA backend root")
    public Response getRoot() {
        return given().baseUri(baseUrl).when().get("/").then().extract().response();
    }

    @Step("GET {path}")
    public Response get(String path) {
        return spec().when().get(path).then().extract().response();
    }

    @Step("GET {path} (bearer)")
    public Response get(String path, String bearerToken) {
        return spec().header("Authorization", "Bearer " + bearerToken)
                .when().get(path).then().extract().response();
    }

    @Step("POST {path}")
    public Response post(String path, Object body) {
        return spec().body(body).when().post(path).then().extract().response();
    }

    @Step("POST {path} (bearer)")
    public Response post(String path, Object body, String bearerToken) {
        return spec().header("Authorization", "Bearer " + bearerToken)
                .body(body).when().post(path).then().extract().response();
    }

    @Step("PUT {path}")
    public Response put(String path, Object body, String bearerToken) {
        return spec().header("Authorization", "Bearer " + bearerToken)
                .body(body).when().put(path).then().extract().response();
    }

    @Step("PATCH {path}")
    public Response patch(String path, Object body, String bearerToken) {
        return spec().header("Authorization", "Bearer " + bearerToken)
                .body(body).when().patch(path).then().extract().response();
    }

    @Step("DELETE {path}")
    public Response delete(String path, String bearerToken) {
        return spec().header("Authorization", "Bearer " + bearerToken)
                .when().delete(path).then().extract().response();
    }

    private RequestSpecification spec() {
        return given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }
}
