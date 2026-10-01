package com.l2b.vendor.modules.help.data.api;

import com.l2b.vendor.core.api.HttpClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;

/**
 * OpenAPI help-legal (FAQ / Terms / Policies / Refer). Locale query is
 * {@code en|hi|kn|te}. Used to distinguish missing translations from a
 * failed language preference.
 */
public class HelpLegalApi {

    public static final String FAQS = "/api/v1/help-legal/faqs";
    public static final String DOCUMENT = "/api/v1/help-legal/documents/{doc_type}";
    public static final String REFERRALS_ME = "/api/v1/help-legal/referrals/me";

    private final HttpClient http;

    public HelpLegalApi() {
        this(new HttpClient());
    }

    public HelpLegalApi(HttpClient http) {
        this.http = http;
    }

    @Step("GET help-legal FAQs locale={locale}")
    public Response faqs(String token, String locale) {
        return http.get(withLocale(FAQS, locale), token);
    }

    @Step("GET help-legal document {docType} locale={locale}")
    public Response document(String token, String docType, String locale) {
        return http.get(withLocale(DOCUMENT.replace("{doc_type}", docType), locale), token);
    }

    @Step("GET help-legal referrals/me locale={locale}")
    public Response referralsMe(String token, String locale) {
        return http.get(withLocale(REFERRALS_ME, locale), token);
    }

    private static String withLocale(String path, String locale) {
        if (locale == null || locale.isBlank()) {
            return path;
        }
        return path + (path.contains("?") ? "&" : "?") + "locale=" + locale;
    }
}
