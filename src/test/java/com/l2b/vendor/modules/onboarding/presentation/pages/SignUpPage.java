package com.l2b.vendor.modules.onboarding.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.qameta.allure.Step;
import java.util.List;
import org.openqa.selenium.WebElement;

/**
 * Phone Sign up screen. Copy from live vendor dump 16 Sep 2026 ({@code /tmp/l2b-signup-dumps}):
 * title {@code Sign up}, one {@code EditText} (no resource-id), {@code +91}, placeholder
 * {@code Enter mobile number}, CheckBox content-desc {@code I agree to the terms of service
 * and privacy policy}. {@code Get OTP} enabled lives on the clickable outer View
 * ({@code enabled=false} empty; inner TextView/Button stay {@code enabled=true} decoys).
 */
public class SignUpPage extends SplashScreen {

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Sign up']")
    private WebElement screenTitle;

    @AndroidFindBy(xpath = "//android.widget.EditText")
    private WebElement phoneNumberField;

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true'][.//android.widget.TextView[@text='Get OTP']]")
    private WebElement getOtpButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='+91']")
    private WebElement countryCode;

    @AndroidFindBy(className = "android.widget.CheckBox")
    private WebElement termsCheckbox;

    @Override
    @Step("Wait for Sign up screen")
    public void waitUntilLoaded() {
        Waits.visible(driver, screenTitle);
        Waits.visible(driver, phoneNumberField);
    }

    @Step("Check Sign up screen is visible")
    public boolean isDisplayedNow() {
        return isPresent(ComposeLocators.textView("Sign up"));
    }

    @Step("Check Sign up subtitle is visible")
    public boolean isSubtitleVisible() {
        return isPresent(ComposeLocators.textViewContains("Start your journey as a machine owner"));
    }

    @Step("Check 'Mobile Number' label is visible")
    public boolean isMobileNumberLabelVisible() {
        return isPresent(ComposeLocators.textView("Mobile Number"));
    }

    @Step("Check phone placeholder is visible")
    public boolean isPhonePlaceholderVisible() {
        return isPresent(ComposeLocators.textView("Enter mobile number"));
    }

    @Step("Check terms checkbox copy is visible")
    public boolean isTermsVisible() {
        return isPresent(ComposeLocators.textViewContains("I agree to the terms of service"));
    }

    @Step("Check 'Get OTP' is visible")
    public boolean isGetOtpVisible() {
        return isPresent(ComposeLocators.clickableWithText("Get OTP"));
    }

    @Step("Check '+91' is visible")
    public boolean isCountryCodeVisible() {
        return isPresent(ComposeLocators.textView("+91"));
    }

    @Step("Paste into the phone field via clipboard + KEYCODE_PASTE")
    public void pastePhone(String raw) {
        AndroidDriver android = (AndroidDriver) driver;
        android.setClipboardText(raw);
        Waits.visible(driver, phoneNumberField).click();
        phoneNumberField.clear();
        android.pressKey(new KeyEvent(AndroidKey.PASTE));
    }

    @Step("Focus the phone EditText")
    public void focusPhoneField() {
        Waits.visible(driver, phoneNumberField).click();
    }

    @Step("Enter mobile number")
    public void enterPhone(String phone) {
        Waits.visible(driver, phoneNumberField).click();
        phoneNumberField.clear();
        phoneNumberField.sendKeys(phone);
    }

    /**
     * Extra digit keys on the already-focused field. Do not click/sendKeys again — on this
     * Compose EditText a second sendKeys replaces the value instead of appending (observed
     * case 3: field became {@code 1}).
     */
    @Step("Press digit keys without replacing the field")
    public void pressDigitKeys(String digits) {
        AndroidDriver android = (AndroidDriver) driver;
        for (char c : digits.toCharArray()) {
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("pressDigitKeys expects digits, got: " + digits);
            }
            android.pressKey(new KeyEvent(AndroidKey.valueOf("DIGIT_" + c)));
        }
    }

    @Step("Accept terms and privacy checkbox")
    public void acceptTerms() {
        tap(termsCheckbox);
    }

    @Step("Tap Get OTP")
    public void tapGetOtp() {
        tap(getOtpButton);
    }

    /**
     * Rapid 3x tap at the Get OTP clickable outer View — same slot, no wait.
     * Used to probe duplicate send-otp.
     */
    @Step("Tap Get OTP rapidly {times} times")
    public void tapGetOtpRapidly(int times) {
        java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Get OTP"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Get OTP not on screen — refusing rapid tap");
        }
        org.openqa.selenium.Rectangle box = rows.get(0).getRect();
        int x = box.x + box.width / 2;
        int y = box.y + box.height / 2;
        for (int i = 0; i < times; i++) {
            driver.executeScript("mobile: clickGesture", java.util.Map.of("x", x, "y", y));
        }
    }

    /**
     * Dump 16 Sep: clickable outer View wrapping Get OTP. {@code enabled=false} on empty
     * screen; {@code enabled=true} after a valid 10-digit number + terms. Do not read the
     * inner TextView or decoy Button — both stay enabled=true even when the CTA is grey.
     */
    @Step("Read Get OTP enabled on the clickable outer View")
    public boolean isGetOtpEnabled() {
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Get OTP"));
        if (rows.isEmpty()) {
            return false;
        }
        String raw = rows.get(0).getAttribute("enabled");
        return Boolean.parseBoolean(raw);
    }

    /** EditText {@code text} from the dump. Empty string when the field is blank. */
    @Step("Read phone EditText text")
    public String phoneFieldText() {
        String raw = phoneNumberField.getAttribute("text");
        return raw == null || "null".equalsIgnoreCase(raw) ? "" : raw;
    }

    /**
     * CheckBox {@code checked}. Dump 16 Sep: stays {@code false} after a successful tap
     * (Get OTP still enables), so this is observational — same family as Language CheckBox.
     */
    @Step("Read terms CheckBox checked")
    public Boolean termsChecked() {
        String raw = termsCheckbox.getAttribute("checked");
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw)) {
            return null;
        }
        return Boolean.parseBoolean(raw);
    }

    @Step("Hide keyboard if shown")
    public void hideKeyboard() {
        try {
            ((AndroidDriver) driver).hideKeyboard();
        } catch (RuntimeException ignored) {
            // Already hidden.
        }
    }

    @Step("Attach Sign up screen screenshot")
    public void attachScreen() {
        attachScreenshot("Sign up screen");
    }

    @Step("Attach screenshot")
    public void attachScreenshot(String name) {
        super.attachScreenshot(name);
    }
}
