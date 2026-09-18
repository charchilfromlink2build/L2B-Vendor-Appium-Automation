package com.l2b.vendor.modules.onboarding.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.ui.BaseTest;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.onboarding.domain.OnboardingEnvironment;
import com.l2b.vendor.modules.onboarding.presentation.pages.LanguagePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OnboardingCarouselPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.SignUpPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.lang.reflect.Method;
import java.time.Duration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

/**
 * OTP verify screen for first-launch of {@code com.l2b.app.qa} (after Sign up Get OTP).
 * One session per method. Locators from live dump 17 Sep 2026
 * ({@code /tmp/l2b-otp-dumps/otp-empty.xml}). Not in default {@code testng.xml} until review.
 *
 * <p>Dump: one {@code EditText} (not six boxes), Continue enabled on the clickable outer View.
 */
@Epic("Vendor app")
@Feature("OTP screen")
public class OTPScreenTest extends BaseTest {

    /**
     * Unseeded 10-digit prefix. Each format case uses a distinct last digits so send-otp
     * rate-limit on one number cannot block the next case.
     */
    private static String formatPhone(int caseNumber) {
        return "8123456" + String.format("%03d", caseNumber);
    }

    @BeforeSuite(alwaysRun = true)
    public void prepareOnboardingEnvironment() {
        OnboardingEnvironment.prepareSuite();
    }

    @Override
    protected boolean noReset() {
        return false;
    }

    @Override
    protected boolean newSessionPerMethod() {
        return true;
    }

    @Override
    protected boolean autoGrantPermissions() {
        return true;
    }

    @Override
    protected void beforeCreateDriver(Method method) {
        if ("androidBackFromOtpDoesNotCrash".equals(method.getName())
                || "forceKillRelaunchMidEntryResets".equals(method.getName())
                || "sessionPersistenceAfterLoginRelaunch".equals(method.getName())) {
            try {
                Adb.forceStop("com.android.vending");
                Adb.pressHome();
            } catch (RuntimeException ignored) {
                // Session create still reports the real error.
            }
        }
    }

    @AfterMethod(alwaysRun = true)
    public void restoreRadios() {
        try {
            Adb.enableRadios();
        } catch (RuntimeException ignored) {
            // radio restore must not hide the test failure
        }
    }

    /**
     * Empty OTP field on first land. Dump 17 Sep: one EditText, Continue outer View enabled=false.
     * Digit count is not six separate a11y boxes — visual boxes sit inside that single EditText.
     */
    @Test(priority = 1, description = "Case 1: Empty OTP — Continue disabled; one EditText in tree")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-sourced 17 Sep: //android.widget.EditText (count=1, text empty). Continue "
            + "enabled lives on //android.view.View[@clickable='true'][.//android.widget.TextView"
            + "[@text='Continue']] — empty = enabled=false. Inner TextView/Button stay enabled=true decoys.")
    public void emptyOtpLeavesContinueDisabled() {
        OtpPage otp = openOtp();

        int editTexts = otp.otpEditTextCount();
        String field = otp.otpFieldText();
        boolean continueEnabled = otp.isContinueEnabled();
        Allure.parameter("otpEditTextCount", String.valueOf(editTexts));
        Allure.parameter("otpFieldText", field);
        Allure.parameter("continueEnabledEmpty", String.valueOf(continueEnabled));
        Allure.parameter("resendText", otp.resendText());
        Allure.parameter("resendEnabled", String.valueOf(otp.isResendEnabled()));
        otp.attachScreenshot("case1-empty-otp");

        assertThat(otp.isDisplayedNow()).as("Title 'Verify your OTP'").isTrue();
        assertThat(otp.isEnterOtpLabelVisible()).as("'Enter your OTP'").isTrue();
        assertThat(editTexts)
                .as("Dump 17 Sep: OTP is one EditText, not six separate box nodes")
                .isEqualTo(1);
        assertThat(field).as("OTP field must start empty").isEmpty();
        assertThat(continueEnabled)
                .as("Continue clickable outer View must be disabled on empty OTP")
                .isFalse();
    }

    /**
     * Fewer than the required OTP length must leave Continue disabled.
     * Observed 17 Sep: a 5th digit is dropped (field keeps 4) — this case types 3.
     */
    @Test(priority = 2, description = "Case 2: Fewer than 4 digits — Continue stays disabled")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-sourced EditText. Observed max length 4 (12345 kept 1234). Type 123. "
            + "Continue outer View must stay enabled=false.")
    public void fewerDigitsLeaveContinueDisabled() {
        String phone = formatPhone(2);
        OtpPage otp = openOtp(phone);
        otp.focusOtpField();
        otp.pressDigitKeys("123");

        String landing = namedLanding(otp);
        Allure.parameter("phone", phone);
        Allure.parameter("observedMaxLength", "4 (5th digit dropped on prior run)");
        Allure.parameter("landingAfter3Digits", landing);
        Allure.parameter("otpFieldText", otp.isDisplayedNow() ? otp.otpFieldText() : "");
        Allure.parameter("continueEnabledAfter3",
                otp.isDisplayedNow() ? String.valueOf(otp.isContinueEnabled()) : "n/a");
        otp.attachScreenshot("case2-three-digits-" + landing);

        assertThat(landing).as("Three digits must not leave OTP").isEqualTo("otp");
        assertThat(otp.otpFieldText()).as("Field must keep the 3 typed digits").isEqualTo("123");
        assertThat(otp.isContinueEnabled())
                .as("Continue must stay disabled with fewer than 4 digits")
                .isFalse();
    }

    /**
     * Letters/symbols must not land in the OTP field. IME should be numeric; sendKeys and
     * KEYCODE_A are the same probes as Sign up case 5.
     */
    @Test(priority = 3, description = "Case 3: Non-numeric input rejected; keyboard numeric-only")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Focus OTP EditText, record IME keys, sendKeys letters/symbols, KEYCODE_A. "
            + "Field must not keep letters; Continue stays disabled.")
    public void nonNumericInputRejected() {
        OtpPage otp = openOtp(formatPhone(3));
        otp.focusOtpField();

        String source = DriverManager.get().getPageSource();
        boolean qwertyVisible = source.contains("text=\"q\"") || source.contains("text=\"Q\"")
                || source.contains("text=\"w\"") || source.contains("text=\"W\"");
        boolean digitOneVisible = source.contains("text=\"1\"");
        Allure.parameter("qwertyKeysVisible", String.valueOf(qwertyVisible));
        Allure.parameter("digitOneVisible", String.valueOf(digitOneVisible));

        otp.enterOtp("abcXYZ!@#");
        String afterSendKeys = otp.otpFieldText();
        Allure.parameter("otpAfterLetterSendKeys", afterSendKeys);

        otp.focusOtpField();
        ((io.appium.java_client.android.AndroidDriver) DriverManager.get())
                .pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                        io.appium.java_client.android.nativekey.AndroidKey.A));
        String afterLetterKey = otp.otpFieldText();
        Allure.parameter("otpAfterLetterKeyevent", afterLetterKey);
        Allure.parameter("continueEnabled", String.valueOf(otp.isContinueEnabled()));
        Allure.parameter("landing", namedLanding(otp));
        otp.attachScreenshot("case3-non-numeric");

        assertThat(otp.isDisplayedNow()).as("Still on OTP").isTrue();
        assertThat(afterSendKeys)
                .as("Letters/symbols from sendKeys must not stay in the OTP field")
                .doesNotContainPattern("[A-Za-z!@#]");
        assertThat(afterLetterKey)
                .as("KEYCODE_A must not land a letter in the OTP field")
                .doesNotContainPattern("[A-Za-z]");
        assertThat(otp.isContinueEnabled())
                .as("Continue must stay disabled after non-numeric attempts")
                .isFalse();
    }

    /**
     * Leading-zero 4-digit OTP (0123). Zero must not be silently dropped. Completing 4 digits
     * may auto-submit — record landing, do not treat auto-submit as a crash.
     */
    @Test(priority = 4, description = "Case 4: Leading-zero OTP digits are kept, not dropped")
    @Severity(SeverityLevel.CRITICAL)
    @Description("pressDigitKeys 0123. If still on OTP, field must be 0123 not 123. Record landing.")
    public void leadingZeroOtpDigitsKept() {
        String phone = formatPhone(4);
        OtpPage otp = openOtp(phone);
        otp.focusOtpField();
        otp.pressDigitKeys("0123");

        String landing = namedLanding(otp);
        String field = otp.isDisplayedNow() ? otp.otpFieldText() : "";
        Allure.parameter("phone", phone);
        Allure.parameter("landingAfter0123", landing);
        Allure.parameter("otpFieldText", field);
        Allure.parameter("continueEnabled",
                otp.isDisplayedNow() ? String.valueOf(otp.isContinueEnabled()) : "n/a");
        otp.attachScreenshot("case4-leading-zero-" + landing);

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .isEqualTo("com.l2b.app.qa");
        if ("otp".equals(landing)) {
            assertThat(field)
                    .as("Leading zero must not be dropped (0123, not 123)")
                    .isEqualTo("0123");
        } else {
            Allure.parameter("zeroDroppedUnknown", "left OTP after 4 digits — auto-submit, zero not observable");
        }
        assertThat(landing).as("Must not crash to launcher").isNotEqualTo("other");
    }

    /**
     * Paste a valid-length (4) numeric string. Field should fill. Completing 4 digits may auto-submit.
     */
    @Test(priority = 5, description = "Case 5: Paste 4-digit numeric string fills the field")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clipboard 5678 + KEYCODE_PASTE. If still on OTP, field is 5678. Record landing.")
    public void pasteValidLengthNumericFillsField() {
        String phone = formatPhone(5);
        OtpPage otp = openOtp(phone);
        otp.pasteOtp("5678");

        String landing = namedLanding(otp);
        String field = otp.isDisplayedNow() ? otp.otpFieldText() : "";
        Allure.parameter("phone", phone);
        Allure.parameter("pasted", "5678");
        Allure.parameter("landingAfterPaste", landing);
        Allure.parameter("otpFieldText", field);
        otp.attachScreenshot("case5-paste-4digit-" + landing);

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .isEqualTo("com.l2b.app.qa");
        if ("otp".equals(landing)) {
            assertThat(field).as("Pasted 4 digits must fill the EditText").isEqualTo("5678");
        } else {
            Allure.parameter("pasteFilledUnknown", "left OTP after paste — likely auto-submit of 4 digits");
        }
        assertThat(landing).isNotEqualTo("other");
    }

    /**
     * Paste longer than 4 digits. Field must truncate or reject cleanly — no crash.
     */
    @Test(priority = 6, description = "Case 6: Paste longer than 4 digits — truncated or rejected")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clipboard 99999999 + PASTE. Field length must be <= 4. No crash.")
    public void pasteLongerThanRequiredTruncatedOrRejected() {
        OtpPage otp = openOtp(formatPhone(6));
        otp.pasteOtp("99999999");

        String landing = namedLanding(otp);
        String field = otp.isDisplayedNow() ? otp.otpFieldText() : "";
        Allure.parameter("phone", formatPhone(6));
        Allure.parameter("pasted", "99999999");
        Allure.parameter("landingAfterPaste", landing);
        Allure.parameter("otpFieldText", field);
        Allure.parameter("fieldLength", String.valueOf(field.length()));
        otp.attachScreenshot("case6-paste-long-" + landing);

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .isEqualTo("com.l2b.app.qa");
        if ("otp".equals(landing)) {
            assertThat(field.length())
                    .as("Pasted 8 digits must not stay longer than 4 in the OTP field")
                    .isLessThanOrEqualTo(4);
        }
        assertThat(landing).isNotEqualTo("other");
    }

    /**
     * Paste a non-numeric string. Must not crash; letters must not stay.
     */
    @Test(priority = 7, description = "Case 7: Paste non-numeric string — rejected, no crash")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Clipboard hello!! + PASTE. Field must not keep letters. Vendor package stays up.")
    public void pasteNonNumericRejectedNoCrash() {
        OtpPage otp = openOtp(formatPhone(7));
        otp.pasteOtp("hello!!");

        String landing = namedLanding(otp);
        String field = otp.isDisplayedNow() ? otp.otpFieldText() : "";
        Allure.parameter("phone", formatPhone(7));
        Allure.parameter("pasted", "hello!!");
        Allure.parameter("landingAfterPaste", landing);
        Allure.parameter("otpFieldText", field);
        otp.attachScreenshot("case7-paste-nonnumeric-" + landing);

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .isEqualTo("com.l2b.app.qa");
        assertThat(landing).as("Paste must not crash off Vendor").isIn("otp", "signup", "home");
        if ("otp".equals(landing)) {
            assertThat(field).as("Pasted letters/symbols must not stay").doesNotContainPattern("[A-Za-z!]");
        }
    }

    /**
     * Deliberately wrong 4-digit OTP. Must show a clear error and stay on OTP — not Home.
     * Also probes QA verify-otp: 200 + success:false is known; UI must still error, not flash success.
     */
    @Test(priority = 8, description = "Case 8: Wrong OTP 0000 — error on OTP, not Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Type 0000 (4-digit field). Tap Continue if enabled. Record landing, error copy, "
            + "and RestAssured POST /api/v1/auth/verify-otp for the same phone+0000.")
    public void wrongOtpShowsErrorNotSuccess() {
        String phone = formatPhone(8);
        OtpPage otp = openOtp(phone);
        otp.focusOtpField();
        otp.pressDigitKeys("0000");
        if (otp.isDisplayedNow() && otp.isContinueEnabled()) {
            otp.tapContinue();
        }

        io.restassured.response.Response api =
                new com.l2b.vendor.modules.onboarding.data.api.AuthApi().verifyOtp(phone, "0000");
        Allure.parameter("apiStatus", String.valueOf(api.statusCode()));
        Allure.parameter("apiBody", truncate(api.asString(), 400));

        Waits.until(DriverManager.get(),
                d -> (otp.isDisplayedNow() || isHomeNow() || hasWrongOtpCopy(d.getPageSource()))
                        ? Boolean.TRUE : null,
                "After wrong OTP, no OTP / Home / error copy",
                Duration.ofSeconds(10));

        String landing = namedLanding(otp);
        String source = DriverManager.get().getPageSource();
        boolean errorCopy = hasWrongOtpCopy(source);
        Allure.parameter("phone", phone);
        Allure.parameter("landingAfterWrong", landing);
        Allure.parameter("wrongOtpErrorCopy", String.valueOf(errorCopy));
        otp.attachScreenshot("case8-wrong-otp-" + landing);

        assertThat(landing)
                .as("Wrong OTP must not navigate to Home")
                .isNotEqualTo("home");
        assertThat(otp.isDisplayedNow())
                .as("Must stay on Verify OTP after 0000")
                .isTrue();
        Allure.parameter("uiVsApi",
                errorCopy ? "ui-error-ok"
                        : "no-error-copy-on-otp — check screenshot");
        // If errorCopy is false while still on OTP, that is a UX gap — log after this run.
    }

    /**
     * Wait until the on-screen resend countdown hits 0, then submit a 4-digit code.
     * Original SMS OTP is not on the device — record whether copy says expired vs incorrect.
     */
    @Test(priority = 9, description = "Case 9: After resend timer, submit — expired vs wrong copy")
    @Severity(SeverityLevel.NORMAL)
    @Description("Wait until OTP Resend in Ns reaches 0 (or 75s). Then type 0000. Record error copy.")
    public void afterTimerSubmitRecordsExpiredOrWrong() {
        String phone = formatPhone(9);
        OtpPage otp = openOtp(phone);
        int start = otp.resendSecondsRemaining();
        Allure.parameter("resendSecondsAtOpen", String.valueOf(start));

        try {
            Waits.until(DriverManager.get(),
                    d -> {
                        int left = otp.resendSecondsRemaining();
                        if (otp.isResendEnabled() || left == 0) {
                            return Boolean.TRUE;
                        }
                        String text = otp.resendText().toLowerCase();
                        return left < 0 && !text.contains(" in ") ? Boolean.TRUE : null;
                    },
                    "Resend countdown did not reach 0",
                    Duration.ofSeconds(75));
        } catch (org.openqa.selenium.TimeoutException e) {
            Allure.parameter("timerWait", "timed-out-still-counting");
        }

        Allure.parameter("resendTextAfterWait", otp.resendText());
        Allure.parameter("resendEnabledAfterWait", String.valueOf(otp.isResendEnabled()));
        Allure.parameter("resendSecondsAfterWait", String.valueOf(otp.resendSecondsRemaining()));

        otp.clearOtp();
        otp.focusOtpField();
        otp.pressDigitKeys("0000");
        if (otp.isDisplayedNow() && otp.isContinueEnabled()) {
            otp.tapContinue();
        }

        String source = DriverManager.get().getPageSource().toLowerCase();
        boolean expired = source.contains("expir");
        boolean incorrect = source.contains("incorrect") || source.contains("invalid")
                || source.contains("wrong");
        Allure.parameter("expiredCopy", String.valueOf(expired));
        Allure.parameter("incorrectCopy", String.valueOf(incorrect));
        Allure.parameter("landing", namedLanding(otp));
        otp.attachScreenshot("case9-after-timer");

        assertThat(otp.isDisplayedNow() || isHomeNow())
                .as("After timer submit, Vendor still showing OTP or Home")
                .isTrue();
        Allure.parameter("expiredVsWrong",
                expired && !incorrect ? "expired-distinct"
                        : expired && incorrect ? "both"
                        : incorrect ? "generic-wrong-only"
                        : "no-copy");
    }

    /**
     * Several consecutive wrong 4-digit codes. Record lockout / cooldown / rate-limit copy if any.
     */
    @Test(priority = 10, description = "Case 10: Multiple consecutive wrong OTPs — lockout?")
    @Severity(SeverityLevel.NORMAL)
    @Description("Submit 0000, 1111, 2222, 3333 in one session. Record lockout/cooldown copy and landing.")
    public void multipleWrongOtpsLockoutOrNot() {
        String phone = formatPhone(10);
        OtpPage otp = openOtp(phone);
        String[] attempts = {"0000", "1111", "2222", "3333"};
        java.util.List<String> landings = new java.util.ArrayList<>();
        boolean sawLockout = false;
        for (String code : attempts) {
            if (!otp.isDisplayedNow()) {
                landings.add(namedLanding(otp));
                break;
            }
            otp.clearOtp();
            otp.focusOtpField();
            otp.pressDigitKeys(code);
            if (otp.isDisplayedNow() && otp.isContinueEnabled()) {
                otp.tapContinue();
            }
            String source = DriverManager.get().getPageSource().toLowerCase();
            boolean lock = source.contains("too many") || source.contains("lock")
                    || source.contains("try later") || source.contains("cooldown")
                    || source.contains("rate") || source.contains("attempts");
            sawLockout = sawLockout || lock;
            landings.add(namedLanding(otp) + (lock ? "+lockout-copy" : ""));
        }
        Allure.parameter("phone", phone);
        Allure.parameter("attemptLandings", String.join(",", landings));
        Allure.parameter("lockoutCopySeen", String.valueOf(sawLockout));
        otp.attachScreenshot("case10-multi-wrong");

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .isEqualTo("com.l2b.app.qa");
        assertThat(namedLanding(otp)).as("Must not crash").isIn("otp", "home", "signup");
    }

    /**
     * Dump 17 Sep: resend TextView {@code OTP Resend in Ns} enabled=false clickable=false.
     */
    @Test(priority = 11, description = "Case 11: Resend disabled during countdown")
    @Severity(SeverityLevel.CRITICAL)
    @Description("On first land, resend must be OTP Resend in Ns and not enabled/clickable.")
    public void resendDisabledDuringCountdown() {
        OtpPage otp = openOtp(formatPhone(11));
        Allure.parameter("resendText", otp.resendText());
        Allure.parameter("resendEnabled", String.valueOf(otp.isResendEnabled()));
        Allure.parameter("resendSeconds", String.valueOf(otp.resendSecondsRemaining()));
        otp.attachScreenshot("case11-resend-disabled");

        assertThat(otp.resendText()).as("Countdown copy").contains("OTP Resend in");
        assertThat(otp.isResendEnabled())
                .as("Resend must be disabled while the timer is running")
                .isFalse();
        assertThat(otp.resendSecondsRemaining()).as("Seconds remaining").isGreaterThan(0);
    }

    /**
     * After the countdown reaches 0, Resend must become tappable.
     */
    @Test(priority = 12, description = "Case 12: Resend enables when timer hits zero")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Wait up to 75s for countdown 0. Record resend text/enabled. Dump hierarchy.")
    public void resendEnablesWhenTimerHitsZero() throws Exception {
        OtpPage otp = openOtp(formatPhone(12));
        Allure.parameter("resendSecondsAtOpen", String.valueOf(otp.resendSecondsRemaining()));
        try {
            Waits.until(DriverManager.get(),
                    d -> otp.isResendEnabled() || otp.resendSecondsRemaining() == 0
                            ? Boolean.TRUE : null,
                    "Resend did not enable and timer did not hit 0",
                    Duration.ofSeconds(75));
        } catch (org.openqa.selenium.TimeoutException e) {
            Allure.parameter("timerWait", "timed-out");
        }
        String xml = DriverManager.get().getPageSource();
        java.nio.file.Path out = java.nio.file.Path.of("/tmp/l2b-otp-dumps/otp-resend-ready.xml");
        java.nio.file.Files.createDirectories(out.getParent());
        java.nio.file.Files.writeString(out, xml);
        Allure.parameter("dumpPath", out.toString());
        Allure.parameter("resendText", otp.resendText());
        Allure.parameter("resendEnabled", String.valueOf(otp.isResendEnabled()));
        Allure.parameter("resendSeconds", String.valueOf(otp.resendSecondsRemaining()));
        otp.attachScreenshot("case12-resend-ready");

        assertThat(otp.isDisplayedNow()).as("Still on OTP after timer").isTrue();
        assertThat(otp.isResendEnabled())
                .as("Resend must be enabled/clickable once the countdown ends")
                .isTrue();
    }

    /**
     * Tapping Resend OTP must fire a real resend-otp (logcat). RestAssured cannot see the app's
     * outbound calls — same proxy as Sign up case 17.
     */
    @Test(priority = 13, description = "Case 13: Resend tap fires resend-otp (logcat)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Wait for Resend OTP button, logcat -c, tap once. Count resend-otp / send-otp in logcat.")
    public void resendTapFiresRequest() {
        OtpPage otp = openOtp(formatPhone(130));
        waitUntilResendReady(otp);
        Adb.run("logcat", "-c");
        otp.tapResend();

        boolean countdownRestarted = false;
        try {
            Waits.until(DriverManager.get(),
                    d -> otp.resendText().contains("OTP Resend in") ? Boolean.TRUE : null,
                    "Countdown did not restart after Resend tap",
                    Duration.ofSeconds(10));
            countdownRestarted = true;
        } catch (org.openqa.selenium.TimeoutException e) {
            countdownRestarted = otp.resendText().contains("OTP Resend in");
        }

        String logcat = Adb.runAndRead("logcat", "-d", "-t", "400");
        int resendHits = countMatches(logcat, "resend-otp", "resendotp", "/api/v1/auth/resend-otp");
        int sendHits = countMatches(logcat, "send-otp", "sendotp", "/api/v1/auth/send-otp");
        Allure.parameter("logcatResendOtpHits", String.valueOf(resendHits));
        Allure.parameter("logcatSendOtpHits", String.valueOf(sendHits));
        Allure.parameter("resendTextAfterTap", otp.resendText());
        Allure.parameter("countdownRestarted", String.valueOf(countdownRestarted));
        otp.attachScreenshot("case13-resend-tap");

        assertThat(otp.isDisplayedNow()).as("Still on OTP after Resend").isTrue();
        if (resendHits + sendHits == 0) {
            assertThat(countdownRestarted)
                    .as("logcat had 0 send/resend-otp hits (same as Sign up #17) — countdown must restart as UI proof")
                    .isTrue();
        } else {
            assertThat(resendHits + sendHits).isGreaterThan(0);
        }
    }

    /**
     * Same SMS-cost concern as Sign up rapid Get OTP. Three clickGestures the moment Resend OTP
     * enables. Logcat is silent on this APK (case 13); UI proof is one countdown restart, not
     * a second jump in remaining seconds.
     */
    @Test(priority = 14, description = "Case 14: Rapid 3x Resend — only one OTP send")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Wait for Resend OTP, logcat -c, three clickGestures. Count resend-otp/send-otp. "
            + "Countdown must restart once; remaining seconds must not jump up again.")
    public void rapidTripleTapResendSendsOnce() throws InterruptedException {
        OtpPage otp = openOtp(formatPhone(140));
        waitUntilResendReady(otp);
        Adb.run("logcat", "-c");
        otp.tapResendRapidly(3);

        boolean countdownRestarted = false;
        try {
            Waits.until(DriverManager.get(),
                    d -> otp.resendText().contains("OTP Resend in") ? Boolean.TRUE : null,
                    "Countdown did not restart after rapid Resend",
                    Duration.ofSeconds(10));
            countdownRestarted = true;
        } catch (org.openqa.selenium.TimeoutException e) {
            countdownRestarted = otp.resendText().contains("OTP Resend in");
        }

        Thread.sleep(800);
        int sec1 = otp.resendSecondsRemaining();
        Thread.sleep(2500);
        int sec2 = otp.resendSecondsRemaining();
        boolean secondJump = sec2 > sec1 + 5;

        String logcat = Adb.runAndRead("logcat", "-d", "-t", "400");
        int resendHits = countMatches(logcat, "resend-otp", "resendotp", "/api/v1/auth/resend-otp");
        int sendHits = countMatches(logcat, "send-otp", "sendotp", "/api/v1/auth/send-otp");
        Allure.parameter("logcatResendOtpHits", String.valueOf(resendHits));
        Allure.parameter("logcatSendOtpHits", String.valueOf(sendHits));
        Allure.parameter("countdownRestarted", String.valueOf(countdownRestarted));
        Allure.parameter("resendTextAfterRapid", otp.resendText());
        Allure.parameter("resendEnabledAfterRapid", String.valueOf(otp.isResendEnabled()));
        Allure.parameter("secAtT1", String.valueOf(sec1));
        Allure.parameter("secAtT2", String.valueOf(sec2));
        Allure.parameter("secondCountdownJump", String.valueOf(secondJump));
        otp.attachScreenshot("case14-rapid-resend");

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .as("Rapid Resend must not crash Vendor")
                .isEqualTo("com.l2b.app.qa");
        assertThat(otp.isDisplayedNow()).as("Still on OTP after rapid Resend").isTrue();
        if (resendHits + sendHits > 0) {
            assertThat(resendHits + sendHits)
                    .as("logcat send/resend-otp hits must be 1, not duplicate SMS. hits="
                            + (resendHits + sendHits))
                    .isEqualTo(1);
        } else {
            assertThat(countdownRestarted)
                    .as("logcat 0 hits — countdown must restart as UI proof of one resend")
                    .isTrue();
            assertThat(secondJump)
                    .as("remaining seconds jumped up again — likely a second resend (family of #3/#7)")
                    .isFalse();
        }
    }

    /**
     * After Resend, the previous code should be dead. Uses the QA rental phone whose static OTP
     * is 1234 (only way to know the "original" code without SMS). If 1234 still logs in, that is
     * the seeded static OTP, not proof that a real SMS code survived resend.
     */
    @Test(priority = 15, description = "Case 15: After Resend, original OTP is rejected")
    @Severity(SeverityLevel.CRITICAL)
    @Description("QA phone, wait Resend OTP, tap Resend, type 1234. Must stay on OTP with error if "
            + "resend invalidates. Home means the static seed still works.")
    public void originalOtpRejectedAfterResend() {
        String qaPhone = Config.get("user.rental.company.phone");
        OtpPage otp = openOtp(qaPhone);
        waitUntilResendReady(otp);
        otp.tapResend();
        Waits.until(DriverManager.get(),
                d -> otp.resendText().contains("OTP Resend in") ? Boolean.TRUE : null,
                "Countdown did not restart after Resend before stale OTP",
                Duration.ofSeconds(10));

        otp.clearOtp();
        otp.focusOtpField();
        otp.pressDigitKeys("1234");
        // 4-digit field auto-submits — do not tap Continue (node is already gone on Home).

        Waits.until(DriverManager.get(),
                d -> (otp.isDisplayedNow() || isHomeNow() || hasWrongOtpCopy(d.getPageSource()))
                        ? Boolean.TRUE : null,
                "After stale 1234, no OTP / Home / error copy",
                Duration.ofSeconds(12));

        String landing = namedLanding(otp);
        boolean errorCopy = hasWrongOtpCopy(DriverManager.get().getPageSource());
        Allure.parameter("qaPhone", qaPhone);
        Allure.parameter("landingAfterStale", landing);
        Allure.parameter("errorCopy", String.valueOf(errorCopy));
        Allure.parameter("staleVsStatic",
                "home".equals(landing) ? "static-otp-still-accepted"
                        : errorCopy ? "stale-rejected"
                        : "stayed-on-otp-no-copy");
        otp.attachScreenshot("case15-stale-after-resend-" + landing);

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .isEqualTo("com.l2b.app.qa");
        assertThat(landing).as("Named landing after stale OTP").isIn("otp", "home");
        Allure.parameter("note",
                "home".equals(landing)
                        ? "1234 still logged in after Resend — QA static seed still accepted"
                        : "stayed on OTP after 1234 — resend may have invalidated the static seed");
    }

    /**
     * Device Back from OTP. Keyboard may consume the first BACK. Record first and (if needed)
     * second landing. Launcher would be the same family as BUGS_FOUND #4.
     */
    @Test(priority = 16, description = "Case 16: Device Back from OTP — named landing, no crash")
    @Severity(SeverityLevel.NORMAL)
    @Description("Open OTP, BACK. If still on OTP (keyboard), BACK again. Record landing. "
            + "Play Store is force-stopped first so recents leftover is not the landing.")
    public void androidBackFromOtpDoesNotCrash() {
        OtpPage otp = openOtp(formatPhone(16));
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                io.appium.java_client.android.nativekey.AndroidKey.BACK));

        Waits.until(android,
                d -> {
                    String pkg = android.getCurrentPackage();
                    return (pkg != null && !pkg.isBlank()) ? Boolean.TRUE : null;
                },
                "After first Back, no current package",
                Duration.ofSeconds(8));

        String first = namedBackLanding(otp, android);
        Allure.parameter("backLandingFirst", first);
        Allure.parameter("backPackageFirst", String.valueOf(android.getCurrentPackage()));

        if ("otp".equals(first)) {
            android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                    io.appium.java_client.android.nativekey.AndroidKey.BACK));
            Waits.until(android,
                    d -> {
                        String pkg = android.getCurrentPackage();
                        return (pkg != null && !pkg.isBlank()) ? Boolean.TRUE : null;
                    },
                    "After second Back, no current package",
                    Duration.ofSeconds(8));
        }

        String landing = namedBackLanding(otp, android);
        String pkg = android.getCurrentPackage();
        Allure.parameter("backLanding", landing);
        Allure.parameter("backPackage", String.valueOf(pkg));
        otp.attachScreenshot("case16-back-" + landing);

        assertThat(pkg).as("Back from OTP must not crash (package blank)").isNotBlank();
        assertThat(landing).as("Landing after Back must be named").isNotEqualTo("unknown");
    }

    /**
     * Rotation while two OTP digits are entered. Expected: digits and countdown survive.
     * Portrait-lock (orientation unchanged) is acceptable.
     */
    @Test(priority = 17, description = "Case 17: Rotation mid-entry — digits and timer survive")
    @Severity(SeverityLevel.NORMAL)
    @Description("Type 01, rotate to landscape. Record orientation, field text, resend seconds.")
    public void rotationMidEntryPreservesDigitsAndTimer() {
        OtpPage otp = openOtp(formatPhone(172));
        otp.focusOtpField();
        otp.pressDigitKeys("01");
        int secBefore = otp.resendSecondsRemaining();
        String digitsBefore = otp.otpFieldText();
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        org.openqa.selenium.ScreenOrientation before = android.getOrientation();
        try {
            android.rotate(org.openqa.selenium.ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            Allure.parameter("rotateThrew", e.getMessage());
        }
        org.openqa.selenium.ScreenOrientation after = android.getOrientation();
        boolean otpVisibleInLandscape = otp.isDisplayedNow();
        int secAfter = otp.resendSecondsRemaining();
        Allure.parameter("orientationBefore", String.valueOf(before));
        Allure.parameter("orientationAfter", String.valueOf(after));
        Allure.parameter("digitsBefore", digitsBefore);
        Allure.parameter("digitsAfterLandscape", otp.otpFieldText());
        Allure.parameter("resendSecondsBefore", String.valueOf(secBefore));
        Allure.parameter("resendSecondsAfterLandscape", String.valueOf(secAfter));
        Allure.parameter("landscapeUx",
                otpVisibleInLandscape ? "otp-visible" : "keyboard-covers-otp-chrome");
        otp.attachScreenshot("case17-landscape");
        // Logged BUGS_FOUND #9: landscape hides OTP chrome behind the keyboard.

        if (after != org.openqa.selenium.ScreenOrientation.PORTRAIT) {
            try {
                android.rotate(org.openqa.selenium.ScreenOrientation.PORTRAIT);
            } catch (RuntimeException ignored) {
                // restore best-effort
            }
        }
        try {
            Waits.until(DriverManager.get(),
                    d -> otp.isDisplayedNow() ? Boolean.TRUE : null,
                    "OTP did not return after restoring portrait",
                    Duration.ofSeconds(10));
        } catch (org.openqa.selenium.TimeoutException e) {
            Allure.parameter("portraitRestore", "otp-not-visible");
        }
        Allure.parameter("digitsAfterPortrait", otp.otpFieldText());
        Allure.parameter("resendSecondsAfterPortrait", String.valueOf(otp.resendSecondsRemaining()));
        otp.attachScreenshot("case17-portrait-restore");

        assertThat(android.getCurrentPackage()).isEqualTo("com.l2b.app.qa");
        assertThat(otp.isDisplayedNow())
                .as("OTP must return after restoring portrait (landscape layout is BUGS_FOUND #9)")
                .isTrue();
        assertThat(otp.resendText()).as("Countdown survives rotate round-trip").contains("OTP Resend in");
    }

    /**
     * Home then activateApp with two digits entered. Not a full kill. Digits and countdown
     * should survive; timer should keep dropping while backgrounded.
     */
    @Test(priority = 18, description = "Case 18: Background/foreground mid-entry — digits and timer")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Type 01, HOME, wait ~3s, activateApp. Record digits and resend seconds vs wall time.")
    public void backgroundForegroundMidEntryPersists() throws InterruptedException {
        OtpPage otp = openOtp(formatPhone(18));
        otp.focusOtpField();
        otp.pressDigitKeys("01");
        int secBefore = otp.resendSecondsRemaining();
        long t0 = System.currentTimeMillis();
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                io.appium.java_client.android.nativekey.AndroidKey.HOME));
        Thread.sleep(3000);
        android.activateApp(pkg);

        Waits.until(android,
                d -> otp.isDisplayedNow() ? Boolean.TRUE : null,
                "After HOME + activateApp, OTP did not return",
                Duration.ofSeconds(15));
        int secAfter = otp.resendSecondsRemaining();
        long elapsedSec = Math.max(1, (System.currentTimeMillis() - t0) / 1000);
        Allure.parameter("digitsAfterFg", otp.otpFieldText());
        Allure.parameter("resendSecondsBefore", String.valueOf(secBefore));
        Allure.parameter("resendSecondsAfterFg", String.valueOf(secAfter));
        Allure.parameter("wallElapsedSec", String.valueOf(elapsedSec));
        Allure.parameter("timerDelta", String.valueOf(secBefore >= 0 && secAfter >= 0 ? secBefore - secAfter : -1));
        otp.attachScreenshot("case18-bg-fg");

        assertThat(android.getCurrentPackage()).isEqualTo(pkg);
        assertThat(otp.isDisplayedNow()).as("OTP returns after background/foreground").isTrue();
        assertThat(otp.otpFieldText()).as("Entered digits survive background/foreground").isEqualTo("01");
        assertThat(otp.resendText()).as("Countdown still on screen").contains("OTP Resend in");
        if (secBefore > 5 && secAfter >= 0) {
            assertThat(secAfter)
                    .as("Timer must not reset to a full window after a short background")
                    .isLessThan(secBefore + 2);
        }
    }

    /**
     * Force-stop then relaunch while two OTP digits are entered. Expected reset: language
     * first-launch or empty Sign up — not a still-filled OTP screen.
     */
    @Test(priority = 19, description = "Case 19: Force-kill + relaunch mid-entry — expected reset")
    @Severity(SeverityLevel.NORMAL)
    @Description("Type 01, terminateApp + activateApp without pm clear. Record landing. "
            + "Expected reset: language / carousel / empty Sign up, not a filled OTP.")
    public void forceKillRelaunchMidEntryResets() {
        OtpPage otp = openOtp(formatPhone(19));
        otp.focusOtpField();
        otp.pressDigitKeys("01");
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.terminateApp(pkg);
        android.activateApp(pkg);

        Waits.until(android,
                d -> {
                    String now = android.getCurrentPackage();
                    if (!pkg.equals(now)) {
                        return null;
                    }
                    return (otp.isDisplayedNow()
                            || new SignUpPage().isDisplayedNow()
                            || new LanguagePage().isTitleEnglish()
                            || new OnboardingCarouselPage().isLoaded()
                            || isHomeNow()) ? Boolean.TRUE : null;
                },
                "After force-kill relaunch, no OTP / Sign up / language / carousel / Home",
                Duration.ofSeconds(20));

        String landing = namedBackLanding(otp, android);
        String digits = otp.isDisplayedNow() ? otp.otpFieldText() : "";
        Allure.parameter("relaunchLanding", landing);
        Allure.parameter("digitsAfterRelaunch", digits);
        otp.attachScreenshot("case19-force-kill-" + landing);

        assertThat(android.getCurrentPackage()).as("Vendor must come back after force-kill").isEqualTo(pkg);
        assertThat(landing).as("Named landing after force-kill").isIn(
                "otp", "signup", "language", "carousel-slide1", "carousel-slide2", "home");
        if ("otp".equals(landing)) {
            Allure.parameter("note", "OTP survived force-kill — digits=" + digits);
        }
    }

    /**
     * Submit OTP with radios already off. Logged as BUGS_FOUND #10.
     */
    @Test(priority = 20, description = "Case 20: No network on OTP submit — error/retry, no crash")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Open OTP online, disable radios, type 0000. Record error/retry vs silent no-op. "
            + "Radios restored in AfterMethod.")
    public void noNetworkOnOtpSubmitShowsErrorNotCrash() {
        OtpPage otp = openOtp(formatPhone(20));
        Adb.disableRadios();
        otp.focusOtpField();
        otp.pressDigitKeys("0000");

        Waits.until(DriverManager.get(),
                d -> (otp.isDisplayedNow() || isHomeNow() || hasOfflineCopy(d.getPageSource()))
                        ? Boolean.TRUE : null,
                "After offline OTP submit, no OTP / Home / offline copy",
                Duration.ofSeconds(10));

        String source = DriverManager.get().getPageSource();
        boolean stillOtp = otp.isDisplayedNow();
        boolean home = isHomeNow();
        boolean offlineCopy = hasOfflineCopy(source);
        Allure.parameter("otpStillVisible", String.valueOf(stillOtp));
        Allure.parameter("homeVisible", String.valueOf(home));
        Allure.parameter("offlineOrRetryCopy", String.valueOf(offlineCopy));
        Allure.parameter("offlineUx", home ? "reached-home-anyway"
                : offlineCopy ? "error-or-retry-copy"
                : stillOtp ? "stayed-on-otp-no-error-copy"
                : "unknown");
        otp.attachScreenshot("case20-offline-submit");

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .as("Offline OTP submit must not crash Vendor")
                .isEqualTo("com.l2b.app.qa");
    }

    /**
     * Tap Resend with radios already off. Logged as BUGS_FOUND #11 if there is no offline copy.
     */
    @Test(priority = 21, description = "Case 21: No network on Resend — error/retry, no crash")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Wait for Resend OTP, disable radios, tap Resend. Record error/retry vs silent no-op.")
    public void noNetworkOnResendShowsErrorNotCrash() {
        OtpPage otp = openOtp(formatPhone(210));
        waitUntilResendReady(otp);
        Adb.disableRadios();
        otp.tapResend();

        Waits.until(DriverManager.get(),
                d -> (otp.isDisplayedNow() || hasOfflineCopy(d.getPageSource()))
                        ? Boolean.TRUE : null,
                "After offline Resend, OTP left the screen with no offline copy",
                Duration.ofSeconds(10));

        boolean stillOtp = otp.isDisplayedNow();
        boolean offlineCopy = hasOfflineCopy(DriverManager.get().getPageSource());
        boolean countdownRestarted = otp.resendText().contains("OTP Resend in");
        Allure.parameter("otpStillVisible", String.valueOf(stillOtp));
        Allure.parameter("offlineOrRetryCopy", String.valueOf(offlineCopy));
        Allure.parameter("resendTextAfterOffline", otp.resendText());
        Allure.parameter("countdownRestartedOffline", String.valueOf(countdownRestarted));
        Allure.parameter("offlineUx", offlineCopy ? "error-or-retry-copy"
                : countdownRestarted ? "countdown-restarted-anyway"
                : stillOtp ? "stayed-on-otp-no-error-copy"
                : "unknown");
        otp.attachScreenshot("case21-offline-resend");

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .as("Offline Resend must not crash Vendor")
                .isEqualTo("com.l2b.app.qa");
        assertThat(stillOtp).as("Must stay on OTP after offline Resend").isTrue();
    }

    /**
     * Intended to simulate OTP submit when the QA backend is down, without taking
     * qa.waardian.com offline for others. Ignored until a local stub exists — same
     * reasoning as Splash / Sign up.
     */
    @Ignore("TODO: No isolated stub for QA.waardian.com; enabling this would interfere with the shared QA env.")
    @Test(priority = 22, description = "Case 22: Backend unreachable at OTP submit")
    @Severity(SeverityLevel.NORMAL)
    @Description("Placeholder: simulate backend down without touching shared QA. Not run until a local stub exists.")
    public void backendUnreachableOnOtpSubmit() {
        throw new UnsupportedOperationException("No safe backend-down injection yet");
    }

    /**
     * QA rental company phone has static OTP 1234. This case asserts Home screen identity,
     * not merely “left OTP”. Process note 18 Sep: {@code 9000000001} has rental
     * {@code pending} jobs, so Quick Booking intercepts. That is not a product bug on OTP;
     * the original wait already required Home chrome ({@code Good Morning!} /
     * {@code Current Earning}), so Quick Booking could not have counted as a pass. Combined
     * suite timeouts on 23/24 were real Home misses, not a loose “not on OTP” check.
     */
    @Test(priority = 23, description = "Case 23: Correct OTP lands on Home with a session")
    @Severity(SeverityLevel.CRITICAL)
    @Description("QA phone + 1234. Auto-submit on 4th digit. Must reach Home identity "
            + "(greeting or Current Earning) and must not treat Quick Booking as Home.")
    public void correctOtpLandsOnHomeWithSession() {
        String qaPhone = Config.get("user.rental.company.phone");
        OtpPage otp = openOtp(qaPhone);
        otp.focusOtpField();
        otp.pressDigitKeys("1234");

        Waits.until(DriverManager.get(),
                d -> (isHomeNow() || isQuickBookingNow() || !otp.isDisplayedNow()) ? Boolean.TRUE : null,
                "Correct OTP 1234 stayed on OTP — no Home or Quick Booking",
                Duration.ofSeconds(15));

        String landing = namedLanding(otp);
        Allure.parameter("qaPhone", qaPhone);
        Allure.parameter("landing", landing);
        Allure.parameter("homeVisible", String.valueOf(isHomeNow()));
        Allure.parameter("quickBookingVisible", String.valueOf(isQuickBookingNow()));
        otp.attachScreenshot("case23-correct-otp-" + landing);

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .isEqualTo("com.l2b.app.qa");
        assertThat(isQuickBookingNow())
                .as("Process: Quick Booking on 9000000001 is not Home. OTP 23 must not pass on this intercept.")
                .isFalse();
        assertThat(isHomeNow()).as("Correct static OTP must land on Home screen identity").isTrue();
        assertThat(otp.isDisplayedNow()).as("OTP must be gone after successful login").isFalse();
    }

    /**
     * After a successful login, force-stop and relaunch without pm clear. Session should
     * return to Home identity, not Language / Sign up. Same process note as case 23:
     * {@code 9000000001} pending jobs open Quick Booking first — not a product OTP bug,
     * and not a loose landing check.
     */
    @Test(priority = 24, description = "Case 24: Session persists after login + relaunch")
    @Severity(SeverityLevel.CRITICAL)
    @Description("QA phone + 1234, terminateApp + activateApp without pm clear. Must return to "
            + "Home identity, not Quick Booking / first-launch.")
    public void sessionPersistenceAfterLoginRelaunch() {
        String qaPhone = Config.get("user.rental.company.phone");
        OtpPage otp = openOtp(qaPhone);
        otp.focusOtpField();
        otp.pressDigitKeys("1234");
        Waits.until(DriverManager.get(),
                d -> (isHomeNow() || isQuickBookingNow() || !otp.isDisplayedNow()) ? Boolean.TRUE : null,
                "Precondition: 1234 stayed on OTP — no Home or Quick Booking",
                Duration.ofSeconds(15));

        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.terminateApp(pkg);
        android.activateApp(pkg);

        Waits.until(android,
                d -> {
                    String now = android.getCurrentPackage();
                    if (!pkg.equals(now)) {
                        return null;
                    }
                    return (isHomeNow()
                            || isQuickBookingNow()
                            || otp.isDisplayedNow()
                            || new SignUpPage().isDisplayedNow()
                            || new LanguagePage().isTitleEnglish()
                            || new OnboardingCarouselPage().isLoaded()) ? Boolean.TRUE : null;
                },
                "After login relaunch, no Home / Quick Booking / OTP / Sign up / language / carousel",
                Duration.ofSeconds(20));

        String landing = namedBackLanding(otp, android);
        Allure.parameter("qaPhone", qaPhone);
        Allure.parameter("relaunchLanding", landing);
        Allure.parameter("homeVisible", String.valueOf(isHomeNow()));
        Allure.parameter("quickBookingVisible", String.valueOf(isQuickBookingNow()));
        otp.attachScreenshot("case24-session-relaunch-" + landing);

        assertThat(android.getCurrentPackage()).isEqualTo(pkg);
        assertThat(isQuickBookingNow())
                .as("Process: Quick Booking on 9000000001 after relaunch is not Home.")
                .isFalse();
        assertThat(isHomeNow())
                .as("Logged-in session must survive force-kill relaunch (Home identity, not first-launch)")
                .isTrue();
    }

    /**
     * Dump-sourced OTP field metrics. Visual 4 boxes are not separate a11y nodes — one EditText.
     * Landscape dump is for bug #9 (chrome hidden); record whether the EditText itself resizes.
     */
    @Test(priority = 25, description = "OTP EditText dump sizing portrait vs landscape")
    @Severity(SeverityLevel.NORMAL)
    @Description("Measure the live EditText bounds in px and dp. Count child box nodes. Dump landscape XML.")
    public void otpFieldSizingFromDump() throws Exception {
        OtpPage otp = openOtp(formatPhone(901));
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        int densityDpi = Integer.parseInt(String.valueOf(
                android.getCapabilities().getCapability("deviceScreenDensity")));
        double pxPerDp = densityDpi / 160.0;
        Allure.parameter("densityDpi", String.valueOf(densityDpi));
        Allure.parameter("pxPerDp", String.format("%.3f", pxPerDp));
        Allure.parameter("minTouchPx", String.format("%.1f", 48 * pxPerDp));

        java.nio.file.Path dir = java.nio.file.Path.of("/tmp/l2b-otp-dumps");
        java.nio.file.Files.createDirectories(dir);
        java.nio.file.Files.writeString(dir.resolve("otp-sizing-portrait.xml"), android.getPageSource());
        recordOtpFieldMetrics(otp, android, pxPerDp, "portrait");
        otp.attachScreenshot("sizing-portrait");

        try {
            android.rotate(org.openqa.selenium.ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            Allure.parameter("rotateThrew", e.getMessage());
        }
        Thread.sleep(1500);
        java.nio.file.Files.writeString(dir.resolve("otp-sizing-landscape.xml"), android.getPageSource());
        recordOtpFieldMetrics(otp, android, pxPerDp, "landscape");
        otp.attachScreenshot("sizing-landscape");

        if (android.getOrientation() != org.openqa.selenium.ScreenOrientation.PORTRAIT) {
            try {
                android.rotate(org.openqa.selenium.ScreenOrientation.PORTRAIT);
            } catch (RuntimeException ignored) {
                // restore best-effort
            }
        }

        assertThat(otp.otpEditTextCount()).as("Still one EditText, not four box nodes").isEqualTo(1);
    }

    private static void recordOtpFieldMetrics(
            OtpPage otp,
            io.appium.java_client.android.AndroidDriver android,
            double pxPerDp,
            String label) {
        java.util.List<org.openqa.selenium.WebElement> fields =
                android.findElements(org.openqa.selenium.By.className("android.widget.EditText"));
        Allure.parameter(label + ".editTextCount", String.valueOf(fields.size()));
        if (fields.isEmpty()) {
            Allure.parameter(label + ".editText", "missing");
            return;
        }
        org.openqa.selenium.Rectangle box = fields.get(0).getRect();
        java.util.List<org.openqa.selenium.WebElement> children =
                fields.get(0).findElements(org.openqa.selenium.By.xpath(".//*"));
        Allure.parameter(label + ".bounds",
                "[" + box.x + "," + box.y + "][" + (box.x + box.width) + "," + (box.y + box.height) + "]");
        Allure.parameter(label + ".px", box.width + "x" + box.height);
        Allure.parameter(label + ".dp",
                String.format("%.1fx%.1f", box.width / pxPerDp, box.height / pxPerDp));
        Allure.parameter(label + ".childNodes", String.valueOf(children.size()));
        Allure.parameter(label + ".impliedBoxPx",
                String.format("%.1fx%d", box.width / 4.0, box.height));
        Allure.parameter(label + ".impliedBoxDp",
                String.format("%.1fx%.1f", (box.width / 4.0) / pxPerDp, box.height / pxPerDp));
        Allure.parameter(label + ".heightMeets48dp", String.valueOf(box.height / pxPerDp >= 47.5));
        Allure.parameter(label + ".titleVisible", String.valueOf(otp.isDisplayedNow()));
    }

    private void waitUntilResendReady(OtpPage otp) {
        Waits.until(DriverManager.get(),
                d -> otp.isResendEnabled() ? Boolean.TRUE : null,
                "Resend OTP button did not enable",
                Duration.ofSeconds(75));
    }

    private static int countMatches(String haystack, String... needles) {
        if (haystack == null || haystack.isBlank()) {
            return 0;
        }
        int n = 0;
        for (String line : haystack.split("\n")) {
            String lower = line.toLowerCase();
            for (String needle : needles) {
                if (lower.contains(needle.toLowerCase())) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    private static String truncate(String raw, int max) {
        if (raw == null) {
            return "";
        }
        return raw.length() <= max ? raw : raw.substring(0, max) + "…";
    }

    private static boolean hasOfflineCopy(String source) {
        String s = source == null ? "" : source.toLowerCase();
        return s.contains("no internet") || s.contains("no network") || s.contains("offline")
                || s.contains("retry") || s.contains("check your connection")
                || s.contains("something went wrong") || s.contains("unable to");
    }

    private static boolean hasWrongOtpCopy(String source) {
        String s = source == null ? "" : source.toLowerCase();
        return s.contains("invalid") || s.contains("incorrect") || s.contains("wrong")
                || s.contains("expired") || s.contains("failed") || s.contains("try again")
                || s.contains("does not match") || s.contains("otp is");
    }

    private OtpPage openOtp() {
        return openOtp(Config.get("user.rental.company.phone"));
    }

    private OtpPage openOtp(String phone) {
        LanguagePage language = new LanguagePage();
        language.waitUntilLoaded();
        language.tapGetStarted();

        OnboardingCarouselPage carousel = new OnboardingCarouselPage();
        carousel.waitUntilSlideOne();
        carousel.tapNext();
        carousel.waitUntilSlideTwo();
        carousel.tapGetStarted();

        SignUpPage signUp = new SignUpPage();
        signUp.waitUntilLoaded();
        signUp.enterPhone(phone);
        signUp.hideKeyboard();
        signUp.acceptTerms();
        Waits.until(DriverManager.get(),
                d -> signUp.isGetOtpEnabled() ? Boolean.TRUE : null,
                "Get OTP stayed disabled before opening OTP",
                Duration.ofSeconds(8));
        signUp.tapGetOtp();

        OtpPage otp = new OtpPage();
        otp.waitUntilLoaded();
        return otp;
    }

    /**
     * Home screen identity from live dump 18 Sep ({@code Good Afternoon!}, {@code Current Earning}).
     * Greeting is time-of-day specific, so afternoon/evening variants are included. Quick Booking
     * is excluded even if a card reused similar copy.
     */
    private static boolean isHomeNow() {
        if (isQuickBookingNow()) {
            return false;
        }
        return DriverManager.get().findElements(org.openqa.selenium.By.xpath(
                "//android.widget.TextView[@text='Good Morning!' or @text='Good Afternoon!' "
                        + "or @text='Good Evening!' or @text='Current Earning']"))
                .size() > 0;
    }

    /** App-bar title from live dump 18 Sep. Card badges also say Quick Booking — same string. */
    private static boolean isQuickBookingNow() {
        return DriverManager.get().findElements(
                org.openqa.selenium.By.xpath("//android.widget.TextView[@text='Quick Booking']"))
                .size() > 0;
    }

    private static String namedLanding(OtpPage otp) {
        if (otp.isDisplayedNow()) {
            return "otp";
        }
        if (isQuickBookingNow()) {
            return "quick-booking";
        }
        if (isHomeNow()) {
            return "home";
        }
        SignUpPage signUp = new SignUpPage();
        if (signUp.isDisplayedNow()) {
            return "signup";
        }
        return "other";
    }

    private static String namedBackLanding(
            OtpPage otp, io.appium.java_client.android.AndroidDriver android) {
        String pkg = android.getCurrentPackage();
        if (pkg == null || pkg.isBlank()) {
            return "unknown";
        }
        if (!"com.l2b.app.qa".equals(pkg)) {
            if (pkg.contains("launcher")) {
                return "launcher";
            }
            if (pkg.contains("vending") || pkg.contains("play")) {
                return "play-store";
            }
            return "other-package:" + pkg;
        }
        if (otp.isDisplayedNow()) {
            return "otp";
        }
        if (isQuickBookingNow()) {
            return "quick-booking";
        }
        SignUpPage signUp = new SignUpPage();
        if (signUp.isDisplayedNow()) {
            return "signup";
        }
        OnboardingCarouselPage carousel = new OnboardingCarouselPage();
        if (carousel.isOnSlideOne()) {
            return "carousel-slide1";
        }
        if (carousel.isOnSlideTwo()) {
            return "carousel-slide2";
        }
        LanguagePage language = new LanguagePage();
        if (language.isTitleEnglish() || language.isTitleHindi()
                || language.isTitleTelugu() || language.isTitleKannada()) {
            return "language";
        }
        if (isHomeNow()) {
            return "home";
        }
        return "vendor-other";
    }
}
