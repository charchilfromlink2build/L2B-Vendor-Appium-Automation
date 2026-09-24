package com.l2b.vendor.modules.bookings.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;

/** Swagger: Rentals Vendor bookings + Rentals Bookings detail/invoice. */
public class BookingsApi {

    public static final String LIST = "/api/v1/rentals/vendor/bookings";
    public static final String ACCEPT = "/api/v1/rentals/vendor/bookings/{booking_id}/accept";
    public static final String DECLINE = "/api/v1/rentals/vendor/bookings/{booking_id}/decline";
    public static final String ASSIGN = "/api/v1/rentals/vendor/bookings/{booking_id}/assign";
    public static final String EXTENSION_ACCEPT =
            "/api/v1/rentals/vendor/bookings/{booking_id}/extension/accept";
    public static final String EXTENSION_DECLINE =
            "/api/v1/rentals/vendor/bookings/{booking_id}/extension/decline";
    public static final String CANDIDATES =
            "/api/v1/rentals/vendor/bookings/{booking_id}/assignment-candidates";
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

    @Step("GET vendor bookings (no token)")
    public Response listUnauthenticated() {
        return http.get(LIST);
    }

    @Step("GET booking detail {bookingId}")
    public Response detail(String token, String bookingId) {
        return http.get(DETAIL.replace("{booking_id}", bookingId), token);
    }

    @Step("GET booking detail unauthenticated {bookingId}")
    public Response detailUnauthenticated(String bookingId) {
        return http.get(DETAIL.replace("{booking_id}", bookingId));
    }

    @Step("GET assignment candidates {bookingId}")
    public Response assignmentCandidates(String token, String bookingId) {
        return http.get(CANDIDATES.replace("{booking_id}", bookingId), token);
    }

    @Step("POST accept {bookingId}")
    public Response accept(String token, String bookingId) {
        return http.post(ACCEPT.replace("{booking_id}", bookingId), Map.of(), token);
    }

    @Step("POST accept unauthenticated {bookingId}")
    public Response acceptUnauthenticated(String bookingId) {
        return http.post(ACCEPT.replace("{booking_id}", bookingId), Map.of());
    }

    @Step("POST decline {bookingId}")
    public Response decline(String token, String bookingId, String reason) {
        return http.post(
                DECLINE.replace("{booking_id}", bookingId), Map.of("reason", reason), token);
    }

    @Step("POST assign {bookingId}")
    public Response assign(
            String token,
            String bookingId,
            String machineId,
            String operatorId,
            boolean allowOperatorOverlap) {
        Map<String, Object> body = new HashMap<>();
        body.put("machine_id", machineId);
        body.put("operator_id", operatorId);
        body.put("allow_operator_overlap", allowOperatorOverlap);
        return http.post(ASSIGN.replace("{booking_id}", bookingId), body, token);
    }

    @Step("POST extension accept {bookingId}")
    public Response acceptExtension(String token, String bookingId) {
        return http.post(EXTENSION_ACCEPT.replace("{booking_id}", bookingId), Map.of(), token);
    }

    @Step("POST extension decline {bookingId}")
    public Response declineExtension(String token, String bookingId) {
        return http.post(EXTENSION_DECLINE.replace("{booking_id}", bookingId), Map.of(), token);
    }

    @Step("GET invoice {bookingId}")
    public Response invoice(String token, String bookingId) {
        return http.get(INVOICE.replace("{booking_id}", bookingId), token);
    }
}
