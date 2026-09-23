package com.l2b.vendor.modules.bookings.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;

/** Swagger: Rentals Operator bookings start / end (job OTP). */
public class OperatorBookingsApi {

    public static final String LIST = "/api/v1/rentals/operator/bookings";
    public static final String START = "/api/v1/rentals/operator/bookings/{booking_id}/start";
    public static final String END = "/api/v1/rentals/operator/bookings/{booking_id}/end";

    private final HttpClient http;

    public OperatorBookingsApi() {
        this(new HttpClient());
    }

    public OperatorBookingsApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET operator bookings")
    public Response list(String token) {
        return http.get(LIST, token);
    }

    @Step("POST start task {bookingId}")
    public Response start(String token, String bookingId, String otp, Double lat, Double lng) {
        return http.post(START.replace("{booking_id}", bookingId), taskBody(otp, lat, lng), token);
    }

    @Step("POST end task {bookingId}")
    public Response end(String token, String bookingId, String otp, Double lat, Double lng) {
        return http.post(END.replace("{booking_id}", bookingId), taskBody(otp, lat, lng), token);
    }

    private static Map<String, Object> taskBody(String otp, Double lat, Double lng) {
        Map<String, Object> body = new HashMap<>();
        body.put("otp", otp);
        if (lat != null) {
            body.put("lat", lat);
        }
        if (lng != null) {
            body.put("lng", lng);
        }
        return body;
    }
}
