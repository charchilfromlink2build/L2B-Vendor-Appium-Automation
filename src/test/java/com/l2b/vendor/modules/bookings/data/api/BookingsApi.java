package com.l2b.vendor.modules.bookings.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger: Rentals Vendor bookings + Rentals Bookings detail/invoice. */
public class BookingsApi {

    public static final String LIST = "/api/v1/rentals/vendor/bookings";
    public static final String ACCEPT = "/api/v1/rentals/vendor/bookings/{booking_id}/accept";
    public static final String DECLINE = "/api/v1/rentals/vendor/bookings/{booking_id}/decline";
    public static final String ASSIGN = "/api/v1/rentals/vendor/bookings/{booking_id}/assign";
    public static final String DETAIL = "/api/v1/rentals/bookings/{booking_id}";
    public static final String INVOICE = "/api/v1/rentals/bookings/{booking_id}/invoice";

    private final HttpClient http;

    public BookingsApi() {
        this(new HttpClient());
    }

    public BookingsApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET vendor bookings")
    public Response list(String token) {
        return http.get(LIST, token);
    }
}
