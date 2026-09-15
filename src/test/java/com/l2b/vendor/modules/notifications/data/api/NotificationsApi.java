package com.l2b.vendor.modules.notifications.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger tag Notifications. */
public class NotificationsApi {

    public static final String FEED = "/api/v1/notifications/feed";
    public static final String UNREAD = "/api/v1/notifications/unread-count";

    private final HttpClient http;

    public NotificationsApi() {
        this(new HttpClient());
    }

    public NotificationsApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET notification feed")
    public Response feed(String token) {
        return http.get(FEED, token);
    }
}
