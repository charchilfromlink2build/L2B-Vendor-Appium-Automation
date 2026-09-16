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
 * Sign up phone screen for first-launch of {@code com.l2b.app.qa} (after language + carousel).
 * Scope: format, abuse patterns, paste, terms checkbox, Get OTP enablement. Does not open OTP
 * except where a later case explicitly taps Get OTP. One session per method.
 *
 * <p>Locators from live dump 16 Sep 2026: title {@code Sign up}, one EditText, {@code +91},
 * Get OTP enabled on the clickable outer View (not the inner TextView/Button decoys).
 */
@Epic("Vendor app")
@Feature("Sign up screen")
public class SignUpTest extends BaseTest {

    /** Valid 10-digit Indian mobile starting 8 — not all-same, not sequential (those are later cases). */
    private static final String VALID_PHONE = "8123456098";

    @BeforeSuite(alwaysRun = true)
    public void prepareOnboardingEnvironment() {
        OnboardingEnvironment.prepareSuite();
    }

    /** Fresh install path so first-launch Sign up is reachable. */
    @Override
    protected boolean noReset() {
        return false;
    }

    /** One session per case — same leak-avoidance as Language / Splash / carousel. */
    @Override
    protected boolean newSessionPerMethod() {
        return true;
    }

    /** Auto-grant so we test Sign up, not the notification dialog. */
    @Override
    protected boolean autoGrantPermissions() {
        return true;
    }

    @Override
    protected void beforeCreateDriver(Method method) {
        if ("androidBackWithPartialNumberDoesNotCrash".equals(method.getName())
                || "forceKillRelaunchMidEntryResets".equals(method.getName())) {
            try {
                Adb.forceStop("com.android.vending");
                Adb.pressHome();
            } catch (RuntimeException ignored) {
                // Session create still reports the real error.
            }
        }
    }

    /**
     * Simulates typing a valid 10-digit number (starts with 6/7/8/9) and checking terms.
     * Expected: Get OTP starts disabled on the empty screen, then the clickable outer View
     * becomes enabled. Does not tap Get OTP — this case is enablement only.
     */
    @Test(priority = 1, description = "Case 1: Valid 10-digit + terms checked enables Get OTP")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-sourced 16 Sep: Get OTP enabled lives on //android.view.View[@clickable='true']"
            + "[.//android.widget.TextView[@text='Get OTP']]. Empty = enabled=false. After "
            + VALID_PHONE + " + terms tap, enabled=true. Inner TextView/Button stay enabled=true decoys.")
    public void validTenDigitWithTermsEnablesGetOtp() {
        SignUpPage signUp = openSignUp();

        assertThat(signUp.isDisplayedNow()).as("Title 'Sign up'").isTrue();
        assertThat(signUp.isGetOtpEnabled())
                .as("Get OTP must start disabled on the empty Sign up screen")
                .isFalse();

        signUp.enterPhone(VALID_PHONE);
        signUp.hideKeyboard();
        Allure.parameter("phoneAfterType", signUp.phoneFieldText());
        Allure.parameter("getOtpEnabledAfterNumberOnly", String.valueOf(signUp.isGetOtpEnabled()));

        signUp.acceptTerms();
        Waits.until(DriverManager.get(),
                d -> signUp.isGetOtpEnabled() ? Boolean.TRUE : null,
                "Get OTP did not become enabled after valid 10-digit number + terms",
                Duration.ofSeconds(8));

        Allure.parameter("termsCheckedAttr", String.valueOf(signUp.termsChecked()));
        Allure.parameter("getOtpEnabledAfterTerms", String.valueOf(signUp.isGetOtpEnabled()));
        signUp.attachScreenshot("case1-valid-enables-get-otp");

        assertThat(signUp.phoneFieldText())
                .as("EditText must keep the 10-digit number")
                .isEqualTo(VALID_PHONE);
        assertThat(signUp.isGetOtpEnabled())
                .as("Get OTP clickable outer View must be enabled after valid number + terms")
                .isTrue();
    }

    /**
     * Simulates typing only 9 digits (valid first digit) and checking terms.
     * Expected: Get OTP stays disabled. Length 10 is required — a short number must not
     * enable the send-OTP CTA even with terms accepted.
     */
    @Test(priority = 2, description = "Case 2: 9-digit number leaves Get OTP disabled")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-sourced: same Get OTP outer View. Type 812345609 (9 digits) + terms. "
            + "Wait briefly for a wrong enable; CTA must stay enabled=false.")
    public void nineDigitNumberLeavesGetOtpDisabled() {
        SignUpPage signUp = openSignUp();
        String nineDigit = "812345609";

        signUp.enterPhone(nineDigit);
        signUp.hideKeyboard();
        signUp.acceptTerms();

        boolean becameEnabled = waitUntilGetOtpEnabled(signUp, Duration.ofSeconds(3));
        Allure.parameter("phoneAfterType", signUp.phoneFieldText());
        Allure.parameter("getOtpEnabled", String.valueOf(signUp.isGetOtpEnabled()));
        signUp.attachScreenshot("case2-nine-digit-disabled");

        assertThat(signUp.phoneFieldText()).as("Field should keep the 9 digits").isEqualTo(nineDigit);
        assertThat(becameEnabled)
                .as("Get OTP must stay disabled for a 9-digit number even with terms checked")
                .isFalse();
    }

    /**
     * Simulates typing a valid 10-digit number, then one extra digit.
     * First isolated run: sendKeys of 11 chars at once left the field empty (whole string
     * rejected). This case types 10, confirms them, then types the 11th key.
     * Expected (ideal): field stays 10. Actual fate is recorded.
     */
    @Test(priority = 3, description = "Case 3: 11th digit — truncated, blocked, or accepted")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Type 10 digits, then one more key. Record EditText. First probe: sendKeys of 11 "
            + "at once emptied the field; this case uses 10 then +1 so we see max-length vs accept.")
    public void eleventhDigitTruncatedOrBlocked() {
        SignUpPage signUp = openSignUp();

        signUp.enterPhone(VALID_PHONE);
        String afterTen = signUp.phoneFieldText();
        Allure.parameter("phoneAfterTen", afterTen);
        assertThat(afterTen)
                .as("Precondition: 10-digit sendKeys must land before probing the 11th key")
                .isEqualTo(VALID_PHONE);

        signUp.pressDigitKeys("1");
        signUp.hideKeyboard();

        String actual = signUp.phoneFieldText();
        int length = actual == null ? -1 : actual.length();
        String fate = length <= 0 ? "cleared"
                : length == 10 ? "truncated-or-blocked-to-10"
                : length >= 11 ? "accepted-11-or-more"
                : "other-length-" + length;
        Allure.parameter("phoneAfterEleventhKey", actual);
        Allure.parameter("phoneLength", String.valueOf(length));
        Allure.parameter("eleventhDigitFate", fate);
        signUp.attachScreenshot("case3-eleventh-digit-" + fate);

        assertThat(length)
                .as("11th digit must not stay in the field. Actual text=" + actual)
                .isEqualTo(10);
        assertThat(actual).as("Kept value should still be the first 10 digits").isEqualTo(VALID_PHONE);
    }

    /**
     * Simulates leaving the phone field empty and checking terms.
     * Expected: Get OTP stays disabled. Terms alone must not enable send-OTP.
     */
    @Test(priority = 4, description = "Case 4: Empty phone field leaves Get OTP disabled")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Empty EditText + terms tap. Get OTP outer View must stay enabled=false.")
    public void emptyFieldLeavesGetOtpDisabled() {
        SignUpPage signUp = openSignUp();

        Allure.parameter("phoneAtStart", signUp.phoneFieldText());
        assertThat(signUp.phoneFieldText()).as("Field starts empty").isEmpty();
        assertThat(signUp.isGetOtpEnabled()).as("Get OTP starts disabled").isFalse();

        signUp.acceptTerms();
        boolean becameEnabled = waitUntilGetOtpEnabled(signUp, Duration.ofSeconds(3));
        Allure.parameter("getOtpEnabledAfterEmptyTerms", String.valueOf(signUp.isGetOtpEnabled()));
        signUp.attachScreenshot("case4-empty-disabled");

        assertThat(becameEnabled)
                .as("Get OTP must stay disabled when the phone field is empty, even with terms checked")
                .isFalse();
    }

    /**
     * Simulates attempting letters/symbols in the phone field.
     * Expected (ideal): numeric keyboard only; non-digits never land; Get OTP stays disabled.
     * Appium sendKeys can inject past an IME, so this also tries a letter keyevent and records
     * whether qwerty keys are on the IME dump.
     */
    @Test(priority = 5, description = "Case 5: Non-numeric input rejected; keyboard numeric-only")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap the EditText, record whether qwerty keys are in the hierarchy, sendKeys letters/"
            + "symbols, press KEYCODE_A. Field must not keep letters; Get OTP stays disabled.")
    public void nonNumericInputRejected() {
        SignUpPage signUp = openSignUp();
        signUp.focusPhoneField();

        String source = DriverManager.get().getPageSource();
        boolean qwertyVisible = source.contains("text=\"q\"") || source.contains("text=\"Q\"")
                || source.contains("text=\"w\"") || source.contains("text=\"W\"");
        boolean digitOneVisible = source.contains("text=\"1\"");
        Allure.parameter("qwertyKeysVisible", String.valueOf(qwertyVisible));
        Allure.parameter("digitOneVisible", String.valueOf(digitOneVisible));

        signUp.enterPhone("abcXYZ!@#");
        String afterSendKeys = signUp.phoneFieldText();
        Allure.parameter("phoneAfterLetterSendKeys", afterSendKeys);

        signUp.focusPhoneField();
        ((io.appium.java_client.android.AndroidDriver) DriverManager.get())
                .pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                        io.appium.java_client.android.nativekey.AndroidKey.A));
        String afterLetterKey = signUp.phoneFieldText();
        Allure.parameter("phoneAfterLetterKeyevent", afterLetterKey);
        signUp.hideKeyboard();
        signUp.acceptTerms();
        boolean becameEnabled = waitUntilGetOtpEnabled(signUp, Duration.ofSeconds(3));
        Allure.parameter("getOtpEnabled", String.valueOf(signUp.isGetOtpEnabled()));
        signUp.attachScreenshot("case5-non-numeric");

        assertThat(qwertyVisible)
                .as("QWERTY keys should not be on the phone-field IME (numeric-only)")
                .isFalse();
        assertThat(afterSendKeys)
                .as("Letters/symbols from sendKeys must not stay in the field")
                .doesNotContainPattern("[A-Za-z!@#]");
        assertThat(afterLetterKey)
                .as("KEYCODE_A must not land a letter in the field")
                .doesNotContainPattern("[A-Za-z]");
        assertThat(becameEnabled)
                .as("Get OTP must stay disabled after non-numeric attempts")
                .isFalse();
    }

    /**
     * Simulates all-same digits 9999999999 (valid length + starts with 9) and checking terms.
     * Expected (ideal): app blocks junk/repeat numbers so Get OTP stays disabled.
     * Actual is recorded — if Get OTP enables, that is a potential SMS-cost / abuse finding.
     */
    @Test(priority = 6, description = "Case 6: All-same 9999999999 — blocked or Get OTP enables")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9999999999 + terms. Record whether Get OTP enables. Not blocked = SMS abuse risk.")
    public void allSameNinesEnablesOrBlocksGetOtp() {
        SignUpPage signUp = openSignUp();
        String phone = "9999999999";
        signUp.enterPhone(phone);
        signUp.hideKeyboard();
        boolean becameEnabled = waitUntilGetOtpEnabled(signUp, Duration.ofSeconds(1));
        Allure.parameter("getOtpEnabledAfterNumberOnly", String.valueOf(becameEnabled));
        signUp.acceptTerms();
        becameEnabled = waitUntilGetOtpEnabled(signUp, Duration.ofSeconds(5));
        Allure.parameter("phoneAfterType", signUp.phoneFieldText());
        Allure.parameter("getOtpEnabled", String.valueOf(signUp.isGetOtpEnabled()));
        Allure.parameter("allSameBlocked", String.valueOf(!becameEnabled));
        signUp.attachScreenshot("case6-all-same-nines-" + (becameEnabled ? "enabled" : "blocked"));

        assertThat(signUp.phoneFieldText()).as("Field keeps 9999999999").isEqualTo(phone);
        assertThat(signUp.isDisplayedNow()).as("Sign up still showing (no crash)").isTrue();
        // If this is true, Category C finding: junk number can reach send-OTP.
        Allure.parameter("smsAbuseRisk", becameEnabled ? "YES-Get-OTP-enabled" : "no-blocked");
        // Observed 16 Sep: Get OTP enabled. Logged as BUGS_FOUND #7 (junk 10-digit enables send-OTP CTA).
    }

    /**
     * Simulates a second all-repeat pattern 6666666666 to check consistency with case 6.
     * Expected (ideal): blocked. Actual is recorded.
     */
    @Test(priority = 7, description = "Case 7: All-same 6666666666 — blocked or Get OTP enables")
    @Severity(SeverityLevel.CRITICAL)
    @Description("6666666666 + terms. Confirm same enablement as 9999999999.")
    public void allSameSixesEnablesOrBlocksGetOtp() {
        SignUpPage signUp = openSignUp();
        boolean enabled = fillPhoneAndTerms(signUp, "6666666666");
        Allure.parameter("phoneAfterType", signUp.phoneFieldText());
        Allure.parameter("getOtpEnabled", String.valueOf(enabled));
        Allure.parameter("smsAbuseRisk", enabled ? "YES-Get-OTP-enabled" : "no-blocked");
        signUp.attachScreenshot("case7-all-same-sixes-" + (enabled ? "enabled" : "blocked"));
        assertThat(signUp.phoneFieldText()).isEqualTo("6666666666");
        assertThat(signUp.isDisplayedNow()).isTrue();
    }

    /**
     * Simulates sequential ascending 1234567890 + terms.
     * Expected (ideal): blocked. Also starts with 1 (outside 6-9) — prefix is case 10.
     */
    @Test(priority = 8, description = "Case 8: Sequential 1234567890 — blocked or Get OTP enables")
    @Severity(SeverityLevel.CRITICAL)
    @Description("1234567890 + terms. Record Get OTP enabled. Sequential + leading-1.")
    public void sequentialAscendingEnablesOrBlocksGetOtp() {
        SignUpPage signUp = openSignUp();
        boolean enabled = fillPhoneAndTerms(signUp, "1234567890");
        Allure.parameter("phoneAfterType", signUp.phoneFieldText());
        Allure.parameter("getOtpEnabled", String.valueOf(enabled));
        Allure.parameter("smsAbuseRisk", enabled ? "YES-Get-OTP-enabled" : "no-blocked");
        signUp.attachScreenshot("case8-sequential-asc-" + (enabled ? "enabled" : "blocked"));
        assertThat(signUp.phoneFieldText()).isEqualTo("1234567890");
        assertThat(signUp.isDisplayedNow()).isTrue();
    }

    /**
     * Simulates sequential descending 9876543210 + terms.
     * Expected (ideal): blocked. Starts with 9 so this is pattern-only, not prefix.
     */
    @Test(priority = 9, description = "Case 9: Sequential 9876543210 — blocked or Get OTP enables")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9876543210 + terms. Record Get OTP enabled.")
    public void sequentialDescendingEnablesOrBlocksGetOtp() {
        SignUpPage signUp = openSignUp();
        boolean enabled = fillPhoneAndTerms(signUp, "9876543210");
        Allure.parameter("phoneAfterType", signUp.phoneFieldText());
        Allure.parameter("getOtpEnabled", String.valueOf(enabled));
        Allure.parameter("smsAbuseRisk", enabled ? "YES-Get-OTP-enabled" : "no-blocked");
        signUp.attachScreenshot("case9-sequential-desc-" + (enabled ? "enabled" : "blocked"));
        assertThat(signUp.phoneFieldText()).isEqualTo("9876543210");
        assertThat(signUp.isDisplayedNow()).isTrue();
    }

    /**
     * Simulates a 10-digit number starting 0-5 (5432109876 starts with 5).
     * Indian mobiles should start 6-9. Case 8 already showed leading 1 is accepted.
     */
    @Test(priority = 10, description = "Case 10: Leading 5 (5432109876) — 6-9 prefix enforced or not")
    @Severity(SeverityLevel.CRITICAL)
    @Description("5432109876 + terms. Record whether the app rejects 0-5 prefix or enables Get OTP.")
    public void leadingFiveEnablesOrBlocksGetOtp() {
        SignUpPage signUp = openSignUp();
        boolean enabled = fillPhoneAndTerms(signUp, "5432109876");
        Allure.parameter("phoneAfterType", signUp.phoneFieldText());
        Allure.parameter("getOtpEnabled", String.valueOf(enabled));
        Allure.parameter("prefix6to9Enforced", String.valueOf(!enabled));
        signUp.attachScreenshot("case10-leading-five-" + (enabled ? "enabled" : "blocked"));
        assertThat(signUp.phoneFieldText()).isEqualTo("5432109876");
        assertThat(signUp.isDisplayedNow()).isTrue();
        // Observed 16 Sep: Get OTP enabled. Prefix 6-9 is not enforced.
    }

    /**
     * Simulates pasting a spaced number {@code 98765 43210}.
     * Expected (ideal): spaces stripped, 9876543210 validates, or a clean reject.
     */
    @Test(priority = 11, description = "Case 11: Paste number with spaces")
    @Severity(SeverityLevel.NORMAL)
    @Description("Paste '98765 43210'. Record field text and Get OTP after terms. "
            + "Sanitized 10-digit vs rejected vs kept-with-spaces.")
    public void pasteNumberWithSpaces() {
        SignUpPage signUp = openSignUp();
        String pasted = "98765 43210";
        signUp.pastePhone(pasted);
        signUp.hideKeyboard();
        String actual = signUp.phoneFieldText();
        signUp.acceptTerms();
        boolean enabled = waitUntilGetOtpEnabled(signUp, Duration.ofSeconds(5));
        String fate = "9876543210".equals(actual) ? "sanitized-to-10"
                : pasted.equals(actual) ? "kept-spaces"
                : actual == null || actual.isEmpty() ? "rejected-empty"
                : "other:" + actual;
        Allure.parameter("pasted", pasted);
        Allure.parameter("phoneAfterPaste", actual);
        Allure.parameter("pasteFate", fate);
        Allure.parameter("getOtpEnabled", String.valueOf(enabled));
        signUp.attachScreenshot("case11-paste-spaces-" + fate.replace(":", "-"));
        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .as("Paste with spaces must not crash Vendor")
                .isEqualTo("com.l2b.app.qa");
        assertThat(signUp.isDisplayedNow()).as("Still on Sign up").isTrue();
    }

    /**
     * Simulates pasting a number that already includes +91.
     * Expected (ideal): no duplicate country-code (not +91 +91 / 9198…).
     */
    @Test(priority = 12, description = "Case 12: Paste +91 9876543210 — no duplicate country code")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Paste '+91 9876543210'. Record field text. Duplicate +91 or 919876543210 is a finding.")
    public void pasteNumberWithCountryCode() {
        SignUpPage signUp = openSignUp();
        String pasted = "+91 9876543210";
        signUp.pastePhone(pasted);
        signUp.hideKeyboard();
        String actual = signUp.phoneFieldText();
        signUp.acceptTerms();
        boolean enabled = waitUntilGetOtpEnabled(signUp, Duration.ofSeconds(5));
        boolean duplicate = actual != null && (actual.contains("+91+91")
                || actual.startsWith("91") && actual.replace(" ", "").length() >= 12
                || actual.replace(" ", "").equals("919876543210"));
        Allure.parameter("pasted", pasted);
        Allure.parameter("phoneAfterPaste", actual);
        Allure.parameter("duplicateCountryCode", String.valueOf(duplicate));
        Allure.parameter("getOtpEnabled", String.valueOf(enabled));
        signUp.attachScreenshot("case12-paste-country-code");
        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .isEqualTo("com.l2b.app.qa");
        assertThat(signUp.isDisplayedNow()).isTrue();
        assertThat(duplicate)
                .as("Pasting '+91 9876543210' must not duplicate the on-screen +91. Actual=" + actual)
                .isFalse();
    }

    /**
     * Simulates pasting a 50+ character string into the phone field.
     * Expected: no crash; rejected or truncated; Sign up still showing.
     */
    @Test(priority = 13, description = "Case 13: Paste 50+ characters — no crash")
    @Severity(SeverityLevel.NORMAL)
    @Description("Paste a 60-char string. Record length/text. Must not crash. Truncation or reject is OK.")
    public void pasteLongStringDoesNotCrash() {
        SignUpPage signUp = openSignUp();
        String pasted = "9".repeat(60);
        signUp.pastePhone(pasted);
        signUp.hideKeyboard();
        String actual = signUp.phoneFieldText();
        int length = actual == null ? -1 : actual.length();
        Allure.parameter("pastedLength", String.valueOf(pasted.length()));
        Allure.parameter("phoneAfterPaste", actual);
        Allure.parameter("phoneLength", String.valueOf(length));
        signUp.attachScreenshot("case13-paste-long");
        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .as("Long paste must not crash Vendor")
                .isEqualTo("com.l2b.app.qa");
        assertThat(signUp.isDisplayedNow()).as("Still on Sign up after 60-char paste").isTrue();
        assertThat(length)
                .as("Long paste must not keep 50+ digits in the field. Actual=" + actual)
                .isLessThanOrEqualTo(10);
    }

    /**
     * Simulates pasting emoji / unicode into the phone field.
     * Expected: no crash; rejected (field empty or digits-only); Sign up still showing.
     */
    @Test(priority = 14, description = "Case 14: Paste emoji/unicode — no crash, rejected")
    @Severity(SeverityLevel.NORMAL)
    @Description("Paste '😀9876543210नमस्ते'. Record field. Must not crash. Letters/emoji must not stay.")
    public void pasteEmojiUnicodeDoesNotCrash() {
        SignUpPage signUp = openSignUp();
        String pasted = "😀9876543210नमस्ते";
        signUp.pastePhone(pasted);
        signUp.hideKeyboard();
        String actual = signUp.phoneFieldText();
        Allure.parameter("pasted", pasted);
        Allure.parameter("phoneAfterPaste", actual);
        signUp.attachScreenshot("case14-paste-emoji");
        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .as("Emoji paste must not crash Vendor")
                .isEqualTo("com.l2b.app.qa");
        assertThat(signUp.isDisplayedNow()).as("Still on Sign up after emoji paste").isTrue();
        assertThat(actual == null ? "" : actual)
                .as("Emoji/unicode must not stay in the field. Actual=" + actual)
                .doesNotContain("😀")
                .doesNotContain("नमस्ते");
    }

    /**
     * Simulates a valid 10-digit number with the terms checkbox left unchecked.
     * Expected: Get OTP stays disabled.
     */
    @Test(priority = 15, description = "Case 15: Valid number, terms unchecked — Get OTP disabled")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Type " + VALID_PHONE + ", do not tap terms. Get OTP outer View must stay enabled=false.")
    public void validNumberTermsUncheckedLeavesGetOtpDisabled() {
        SignUpPage signUp = openSignUp();
        signUp.enterPhone(VALID_PHONE);
        signUp.hideKeyboard();
        boolean enabled = waitUntilGetOtpEnabled(signUp, Duration.ofSeconds(3));
        Allure.parameter("phoneAfterType", signUp.phoneFieldText());
        Allure.parameter("termsCheckedAttr", String.valueOf(signUp.termsChecked()));
        Allure.parameter("getOtpEnabled", String.valueOf(enabled));
        signUp.attachScreenshot("case15-terms-unchecked");
        assertThat(signUp.phoneFieldText()).isEqualTo(VALID_PHONE);
        assertThat(enabled)
                .as("Get OTP must stay disabled when terms are not checked")
                .isFalse();
    }

    /**
     * Simulates checking then unchecking terms with a valid number already entered.
     * Expected: Get OTP enables on check, then disables again on uncheck.
     */
    @Test(priority = 16, description = "Case 16: Check then uncheck terms disables Get OTP again")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valid number, tap terms (Get OTP enables), tap terms again (must disable). "
            + "CheckBox checked attr is unreliable on this dump — enablement is the evidence.")
    public void uncheckTermsDisablesGetOtpAgain() {
        SignUpPage signUp = openSignUp();
        signUp.enterPhone(VALID_PHONE);
        signUp.hideKeyboard();
        signUp.acceptTerms();
        boolean enabledAfterCheck = waitUntilGetOtpEnabled(signUp, Duration.ofSeconds(5));
        Allure.parameter("enabledAfterCheck", String.valueOf(enabledAfterCheck));
        assertThat(enabledAfterCheck).as("Precondition: Get OTP enables after terms check").isTrue();

        signUp.acceptTerms();
        boolean disabledAfterUncheck = waitUntilGetOtpDisabled(signUp, Duration.ofSeconds(5));
        Allure.parameter("enabledAfterUncheck", String.valueOf(signUp.isGetOtpEnabled()));
        signUp.attachScreenshot("case16-terms-unchecked-again");

        assertThat(disabledAfterUncheck)
                .as("Get OTP must disable again after unchecking terms")
                .isTrue();
        assertThat(signUp.isGetOtpEnabled()).isFalse();
    }

    /**
     * Always restore radios so a failed offline Get OTP case cannot leave the emulator dark.
     */
    @AfterMethod(alwaysRun = true)
    public void restoreRadios() {
        try {
            Adb.enableRadios();
        } catch (RuntimeException ignored) {
            // radio restore must not hide the test failure
        }
    }

    /**
     * Simulates three rapid taps on Get OTP as soon as it is enabled, using the QA rental phone
     * (not a random number). Expected: one OTP screen; logcat should not show multiple send-otp
     * hits. RestAssured cannot see the app's outbound calls — logcat is the proxy.
     */
    @Test(priority = 17, description = "Case 17: Rapid 3x Get OTP — only one send-otp")
    @Severity(SeverityLevel.CRITICAL)
    @Description("QA phone + terms, logcat -c, three clickGestures on Get OTP. Count send-otp in "
            + "logcat. OTP chrome should appear once. Does not submit OTP.")
    public void rapidTripleTapGetOtpSendsOnce() {
        SignUpPage signUp = openSignUp();
        String qaPhone = Config.get("user.rental.company.phone");
        boolean enabled = fillPhoneAndTerms(signUp, qaPhone);
        assertThat(enabled).as("Get OTP must be enabled before rapid tap").isTrue();

        Adb.run("logcat", "-c");
        signUp.tapGetOtpRapidly(3);

        OtpPage otp = new OtpPage();
        Waits.until(DriverManager.get(),
                d -> (otp.isDisplayedNow() || signUp.isDisplayedNow()) ? Boolean.TRUE : null,
                "After rapid Get OTP, neither OTP nor Sign up on screen",
                Duration.ofSeconds(12));

        String logcat = Adb.runAndRead("logcat", "-d", "-t", "400");
        int sendHits = countMatches(logcat, "send-otp", "sendotp", "/api/v1/auth/send-otp");
        Allure.parameter("qaPhone", qaPhone);
        Allure.parameter("otpScreenVisible", String.valueOf(otp.isDisplayedNow()));
        Allure.parameter("signupStillVisible", String.valueOf(signUp.isDisplayedNow()));
        Allure.parameter("logcatSendOtpHits", String.valueOf(sendHits));
        signUp.attachScreenshot("case17-rapid-get-otp");

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .as("Rapid Get OTP must not crash Vendor")
                .isEqualTo("com.l2b.app.qa");
        assertThat(otp.isDisplayedNow())
                .as("OTP verify screen should appear after Get OTP (do not submit OTP)")
                .isTrue();
        if (sendHits > 0) {
            assertThat(sendHits)
                    .as("logcat send-otp hits must be 1, not duplicate SMS. hits=" + sendHits)
                    .isEqualTo(1);
        }
    }

    /**
     * Simulates tapping Get OTP with Wi-Fi and mobile data already off.
     * Expected (ideal): error/retry UI, not a crash or infinite spinner.
     */
    @Test(priority = 18, description = "Case 18: No network on Get OTP — error/retry, no crash")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Fill QA phone + terms while online, disable radios, tap Get OTP. Record actual UI. "
            + "Must not crash. Radios restored in AfterMethod.")
    public void noNetworkOnGetOtpShowsErrorNotCrash() {
        SignUpPage signUp = openSignUp();
        String qaPhone = Config.get("user.rental.company.phone");
        assertThat(fillPhoneAndTerms(signUp, qaPhone)).as("Get OTP enabled before radios off").isTrue();

        Adb.disableRadios();
        signUp.tapGetOtp();

        Waits.until(DriverManager.get(),
                d -> {
                    OtpPage otp = new OtpPage();
                    return (otp.isDisplayedNow() || signUp.isDisplayedNow()
                            || hasOfflineCopy(d.getPageSource())) ? Boolean.TRUE : null;
                },
                "After offline Get OTP, no Sign up / OTP / error copy",
                Duration.ofSeconds(10));

        String source = DriverManager.get().getPageSource();
        boolean otp = new OtpPage().isDisplayedNow();
        boolean stillSignup = signUp.isDisplayedNow();
        boolean offlineCopy = hasOfflineCopy(source);
        Allure.parameter("otpScreenVisible", String.valueOf(otp));
        Allure.parameter("signupStillVisible", String.valueOf(stillSignup));
        Allure.parameter("offlineOrRetryCopy", String.valueOf(offlineCopy));
        signUp.attachScreenshot("case18-offline-get-otp");

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .as("Offline Get OTP must not crash Vendor")
                .isEqualTo("com.l2b.app.qa");
        Allure.parameter("offlineUx", otp ? "reached-otp-anyway"
                : offlineCopy ? "error-or-retry-copy"
                : stillSignup ? "stayed-on-signup-no-error-copy"
                : "unknown");
        // Observed 16 Sep: stayed-on-signup-no-error-copy. Logged as BUGS_FOUND #8.
    }

    /**
     * Intended to simulate Get OTP when the QA backend is down, without taking qa.waardian.com
     * offline for others. Ignored until a local stub exists — same reasoning as Splash.
     */
    @Ignore("No isolated stub for QA.waardian.com; enabling this would interfere with the shared QA env.")
    @Test(priority = 19, description = "Case 19: Backend unreachable on Get OTP")
    @Severity(SeverityLevel.NORMAL)
    @Description("Placeholder: simulate backend down without touching shared QA. Not run until a local stub exists.")
    public void backendUnreachableOnGetOtp() {
        throw new UnsupportedOperationException("No safe backend-down injection yet");
    }

    /**
     * Simulates typing a partial number then pressing device Back.
     * Expected: named landing, no crash. Actual screen is recorded (carousel vs language vs launcher).
     */
    @Test(priority = 20, description = "Case 20: Back with partial number — named landing, no crash")
    @Severity(SeverityLevel.NORMAL)
    @Description("Type 81234, Back. Record landing. Play Store is force-stopped first so recents leftover is not the landing.")
    public void androidBackWithPartialNumberDoesNotCrash() {
        SignUpPage signUp = openSignUp();
        signUp.enterPhone("81234");
        signUp.hideKeyboard();
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                io.appium.java_client.android.nativekey.AndroidKey.BACK));

        Waits.until(android,
                d -> {
                    String pkg = android.getCurrentPackage();
                    return (pkg != null && !pkg.isBlank()) ? Boolean.TRUE : null;
                },
                "After Back, no current package",
                Duration.ofSeconds(8));

        String pkg = android.getCurrentPackage();
        String landing = namedLanding(android, pkg, signUp);
        Allure.parameter("backLanding", landing);
        Allure.parameter("backPackage", String.valueOf(pkg));
        signUp.attachScreenshot("case20-back-" + landing);

        assertThat(pkg).as("Back from Sign up must not crash (package blank)").isNotBlank();
        assertThat(landing).as("Landing after Back must be named").isNotEqualTo("unknown");
        // Observed 16 Sep: launcher. Same family as BUGS_FOUND #4 (Back exits first-launch).
    }

    /**
     * Simulates rotating the device while a valid number and terms are on Sign up.
     * Expected: no crash; typed number and Get OTP enablement survive. Portrait-lock is OK.
     */
    @Test(priority = 21, description = "Case 21: Rotation mid-entry — number and terms survive")
    @Severity(SeverityLevel.NORMAL)
    @Description("Type valid number, check terms, rotate. Record orientation, phone text, Get OTP enabled.")
    public void rotationMidEntryPreservesPhoneAndTerms() {
        SignUpPage signUp = openSignUp();
        boolean enabled = fillPhoneAndTerms(signUp, VALID_PHONE);
        assertThat(enabled).as("Precondition: Get OTP enabled before rotate").isTrue();
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        org.openqa.selenium.ScreenOrientation before = android.getOrientation();
        try {
            android.rotate(org.openqa.selenium.ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            Allure.parameter("rotateThrew", e.getMessage());
        }
        org.openqa.selenium.ScreenOrientation after = android.getOrientation();
        Allure.parameter("orientationBefore", String.valueOf(before));
        Allure.parameter("orientationAfter", String.valueOf(after));
        Allure.parameter("phoneAfterRotate", signUp.phoneFieldText());
        Allure.parameter("getOtpEnabledAfterRotate", String.valueOf(signUp.isGetOtpEnabled()));
        signUp.attachScreenshot("case21-rotation");

        assertThat(android.getCurrentPackage()).isEqualTo("com.l2b.app.qa");
        assertThat(signUp.isDisplayedNow()).as("Sign up still showing after rotate").isTrue();
        assertThat(signUp.phoneFieldText()).as("Typed number survives rotation").isEqualTo(VALID_PHONE);
        assertThat(signUp.isGetOtpEnabled()).as("Get OTP enabled state survives rotation").isTrue();
        if (after != org.openqa.selenium.ScreenOrientation.PORTRAIT) {
            try {
                android.rotate(org.openqa.selenium.ScreenOrientation.PORTRAIT);
            } catch (RuntimeException ignored) {
                // restore best-effort
            }
        }
    }

    /**
     * Simulates Home (background) then activateApp while a valid number and terms are entered.
     * Expected: Sign up returns with the typed number and Get OTP still enabled. Not a full kill.
     */
    @Test(priority = 22, description = "Case 22: Background/foreground mid-typing — data persists")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Valid number + terms, HOME, activateApp. Field and Get OTP enabled must persist.")
    public void backgroundForegroundMidTypingPersists() {
        SignUpPage signUp = openSignUp();
        assertThat(fillPhoneAndTerms(signUp, VALID_PHONE)).as("Get OTP enabled before HOME").isTrue();
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                io.appium.java_client.android.nativekey.AndroidKey.HOME));
        android.activateApp(pkg);

        Waits.until(android,
                d -> signUp.isDisplayedNow() ? Boolean.TRUE : null,
                "After HOME + activateApp, Sign up did not return",
                Duration.ofSeconds(15));
        Allure.parameter("phoneAfterFg", signUp.phoneFieldText());
        Allure.parameter("getOtpEnabledAfterFg", String.valueOf(signUp.isGetOtpEnabled()));
        signUp.attachScreenshot("case22-bg-fg");

        assertThat(android.getCurrentPackage()).isEqualTo(pkg);
        assertThat(signUp.phoneFieldText()).as("Typed number survives background/foreground").isEqualTo(VALID_PHONE);
        assertThat(signUp.isGetOtpEnabled()).as("Get OTP enabled survives background/foreground").isTrue();
    }

    /**
     * Simulates force-stop then relaunch while a valid number and terms are entered.
     * Expected: in-progress entry is reset (empty Sign up, or full first-launch language).
     */
    @Test(priority = 23, description = "Case 23: Force-kill + relaunch mid-entry — expected reset")
    @Severity(SeverityLevel.NORMAL)
    @Description("Valid number + terms, terminateApp + activateApp without pm clear. Record landing. "
            + "Expected reset: empty Sign up or language first-launch.")
    public void forceKillRelaunchMidEntryResets() {
        SignUpPage signUp = openSignUp();
        assertThat(fillPhoneAndTerms(signUp, VALID_PHONE)).as("Get OTP enabled before kill").isTrue();
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
                    return (signUp.isDisplayedNow()
                            || new LanguagePage().isTitleEnglish()
                            || new OnboardingCarouselPage().isLoaded()) ? Boolean.TRUE : null;
                },
                "After force-kill relaunch, no Sign up / language / carousel",
                Duration.ofSeconds(20));

        String landing = namedLanding(android, android.getCurrentPackage(), signUp);
        String phone = signUp.isDisplayedNow() ? signUp.phoneFieldText() : "";
        Allure.parameter("relaunchLanding", landing);
        Allure.parameter("phoneAfterRelaunch", phone);
        signUp.attachScreenshot("case23-force-kill-" + landing);

        assertThat(android.getCurrentPackage()).as("Vendor must come back after force-kill").isEqualTo(pkg);
        assertThat(landing).as("Named landing after force-kill").isIn(
                "signup", "language", "carousel-slide1", "carousel-slide2");
        if ("signup".equals(landing)) {
            assertThat(phone)
                    .as("Expected reset: Sign up field should be empty after force-kill")
                    .isEmpty();
            assertThat(signUp.isGetOtpEnabled())
                    .as("Expected reset: Get OTP disabled on empty Sign up")
                    .isFalse();
        }
    }

    private static boolean hasOfflineCopy(String source) {
        String s = source == null ? "" : source.toLowerCase();
        return s.contains("no internet") || s.contains("no network") || s.contains("offline")
                || s.contains("retry") || s.contains("check your connection")
                || s.contains("something went wrong") || s.contains("unable to");
    }

    private static String namedLanding(
            io.appium.java_client.android.AndroidDriver android, String pkg, SignUpPage signUp) {
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
        return "vendor-other";
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

    private static boolean waitUntilGetOtpDisabled(SignUpPage signUp, Duration timeout) {
        try {
            Waits.until(DriverManager.get(),
                    d -> signUp.isGetOtpEnabled() ? null : Boolean.TRUE,
                    "Get OTP stayed enabled",
                    timeout);
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    private boolean fillPhoneAndTerms(SignUpPage signUp, String phone) {
        signUp.enterPhone(phone);
        signUp.hideKeyboard();
        signUp.acceptTerms();
        return waitUntilGetOtpEnabled(signUp, Duration.ofSeconds(5));
    }

    /**
     * True if Get OTP becomes enabled within {@code timeout}; false on timeout (does not throw).
     * Used by negative cases that must prove the CTA stays grey.
     */
    private static boolean waitUntilGetOtpEnabled(SignUpPage signUp, Duration timeout) {
        try {
            Waits.until(DriverManager.get(),
                    d -> signUp.isGetOtpEnabled() ? Boolean.TRUE : null,
                    "Get OTP stayed disabled",
                    timeout);
            return true;
        } catch (org.openqa.selenium.TimeoutException e) {
            return false;
        }
    }

    /** Language → Get started → slide 1 Next → slide 2 Get started → Sign up. */
    private SignUpPage openSignUp() {
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
        return signUp;
    }
}
