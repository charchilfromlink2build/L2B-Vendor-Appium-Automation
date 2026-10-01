package com.l2b.vendor.modules.settings.data.dto;

/**
 * POST /api/v1/account/language body.
 * OpenAPI {@code LanguageUpdateRequest} — {@code language} one of en, hi, kn, te.
 */
public class LanguageUpdateRequest {

    public String language;

    public LanguageUpdateRequest() {
    }

    public LanguageUpdateRequest(String language) {
        this.language = language;
    }
}
