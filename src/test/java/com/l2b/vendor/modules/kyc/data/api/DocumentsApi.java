package com.l2b.vendor.modules.kyc.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/** Swagger tags Documents + Verification. */
public class DocumentsApi {

    public static final String STAGED = "/api/v1/documents/staged";
    public static final String UPLOAD = "/api/v1/documents/upload";
    public static final String IFSC = "/api/v1/verification/verify/ifsc";

    private final HttpClient http;

    public DocumentsApi() {
        this(new HttpClient());
    }

    public DocumentsApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET staged documents")
    public Response staged(String token) {
        return http.get(STAGED, token);
    }
}
