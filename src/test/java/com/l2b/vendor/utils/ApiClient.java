package com.l2b.vendor.utils;

import static io.restassured.RestAssured.given;

import io.qameta.allure.Step;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;

/** RestAssured wrapper. Smoke does not call OTP. */
public class ApiClient {

    private final String baseUrl;

    public ApiClient() {
        this(Config.get("api.base.url"));
    }

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Step("GET QA backend root")
    public Response getRoot() {
        return given()
                .baseUri(baseUrl)
                .when()
                .get("/")
                .then()
                .extract()
                .response();
    }

    @Step("POST send-otp for phone {phone}")
    public Response sendOtp(String phone) {
        return given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(Map.of("phone", phone))
                .when()
                .post(Config.get("api.otp.path"))
                .then()
                .extract()
                .response();
    }

    @Step("POST verify-otp for phone {phone}")
    public Response verifyOtp(String phone, String otp) {
        return given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(Map.of("phone", phone, "otp_code", otp))
                .when()
                .post(Config.get("api.verify.otp.path"))
                .then()
                .extract()
                .response();
    }

    @Step("GET onboarding status")
    public Response getOnboardingStatus(String token) {
        return given()
                .baseUri(baseUrl)
                .accept(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .when()
                .get(Config.get("api.onboarding.status.path"))
                .then()
                .extract()
                .response();
    }
}
