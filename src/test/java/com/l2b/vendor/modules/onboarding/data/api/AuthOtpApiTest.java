package com.l2b.vendor.modules.onboarding.data.api;

import static org.assertj.core.api.Assertions.assertThat;
import org.testng.annotations.Test;

import com.l2b.vendor.environment.Config;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;

/**
 * Direct RestAssured pass against QA auth OTP endpoints. No Appium session.
 * Isolated {@code otp-api.xml} only — not in default {@code testng.xml}.
 */
@Epic("Vendor app")
@Feature("OTP API")
public class AuthOtpApiTest {

    private final AuthApi api = new AuthApi();

    @Test(priority = 1, description = "send-otp valid 10-digit phone → 200 + OtpResponse shape")
    @Severity(SeverityLevel.CRITICAL)
    @Description("POST /api/v1/auth/send-otp with an unseeded 10-digit Indian mobile.")
    public void sendOtpValidPhone() {
        String phone = "8123456801";
        Response response = api.sendOtp(phone);
        record("send-otp valid " + phone, response);

        assertThat(response.statusCode())
                .as("valid send-otp must not 500")
                .isNotEqualTo(500);
        assertThat(response.statusCode()).as("valid send-otp").isEqualTo(200);
        assertThat(response.jsonPath().getBoolean("success")).isTrue();
        assertThat(response.jsonPath().getString("message")).isNotBlank();
    }

    @Test(priority = 2, description = "send-otp invalid/malformed phones → 4xx, not 500")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Empty, letters, 9-digit, 11-digit. Each must be 4xx, never 500.")
    public void sendOtpInvalidPhones() {
        String[] phones = {"", "abc", "12345", "81234568011", "900000000a"};
        for (String phone : phones) {
            Response response = api.sendOtp(phone);
            record("send-otp invalid [" + phone + "]", response);
            assertThat(response.statusCode())
                    .as("malformed send-otp must not 500. phone=[" + phone + "]")
                    .isNotEqualTo(500);
            assertThat(response.statusCode())
                    .as("malformed send-otp should be 4xx. phone=[" + phone + "]")
                    .isBetween(400, 499);
        }
    }

    @Test(priority = 3, description = "verify-otp correct 1234 → 200 + token; wrong 0000 repeated")
    @Severity(SeverityLevel.CRITICAL)
    @Description("QA phone static OTP. Wrong 0000 three times for consistency. Expired not testable without SMS.")
    public void verifyOtpCorrectAndWrong() {
        String qa = Config.get("user.rental.company.phone");
        api.sendOtp(qa);

        Response wrong1 = api.verifyOtp(qa, "0000");
        record("verify-otp wrong 0000 #1", wrong1);
        Response wrong2 = api.verifyOtp(qa, "0000");
        record("verify-otp wrong 0000 #2", wrong2);
        Response wrong3 = api.verifyOtp(qa, "0000");
        record("verify-otp wrong 0000 #3", wrong3);

        assertThat(wrong1.statusCode()).as("wrong #1").isEqualTo(wrong2.statusCode());
        assertThat(wrong2.statusCode()).as("wrong #2 vs #3").isEqualTo(wrong3.statusCode());
        assertThat(wrong1.statusCode()).as("wrong OTP").isEqualTo(400);

        Response correct = api.verifyOtp(qa, "1234");
        record("verify-otp correct 1234", correct);
        assertThat(correct.statusCode()).as("correct static OTP").isEqualTo(200);
        assertThat(correct.jsonPath().getString("access_token")).isNotBlank();
        assertThat(correct.jsonPath().getString("refresh_token")).isNotBlank();
    }

    @Test(priority = 4, description = "resend-otp 200; prior OTP invalidated server-side?")
    @Severity(SeverityLevel.CRITICAL)
    @Description("resend-otp on QA phone, then verify 1234. If 1234 still 200, static seed was not invalidated.")
    public void resendOtpInvalidatesPrior() {
        String qa = Config.get("user.rental.company.phone");
        Response send = api.sendOtp(qa);
        record("send-otp before resend", send);

        Response resend = api.resendOtp(qa);
        record("resend-otp valid QA phone", resend);
        assertThat(resend.statusCode()).as("resend-otp valid").isNotEqualTo(500);

        Response after = api.verifyOtp(qa, "1234");
        record("verify-otp 1234 after resend", after);
        Allure.parameter("priorInvalidated",
                after.statusCode() == 200
                        ? "NO — 1234 still accepted after resend (static seed or resend does not rotate)"
                        : "YES — 1234 rejected after resend status=" + after.statusCode());
    }

    @Test(priority = 5, description = "resend-otp invalid phone → 4xx, not 500")
    @Severity(SeverityLevel.NORMAL)
    @Description("Malformed phone on resend-otp.")
    public void resendOtpInvalidPhone() {
        Response response = api.resendOtp("abc");
        record("resend-otp invalid [abc]", response);
        assertThat(response.statusCode()).isNotEqualTo(500);
        assertThat(response.statusCode()).isBetween(400, 499);
    }

    private static void record(String label, Response response) {
        String body = response.asPrettyString();
        System.out.println("=== " + label + " ===");
        System.out.println("STATUS " + response.statusCode());
        System.out.println(body);
        Allure.parameter(label + ".status", String.valueOf(response.statusCode()));
        Allure.addAttachment(label + ".body", "application/json", body, ".json");
    }
}
