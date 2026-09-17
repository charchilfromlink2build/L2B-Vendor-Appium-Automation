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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * OTP verify screen. Copy from live dump 17 Sep 2026 ({@code /tmp/l2b-otp-dumps/otp-empty.xml}):
 * title {@code Verify your OTP}, one {@code EditText} (no resource-id, not six separate boxes),
 * clickable {@code Edit number} TextView, resend {@code OTP Resend in Ns} (enabled=false while
 * counting), {@code Continue} enabled on the clickable outer View (inner TextView/Button stay
 * {@code enabled=true} decoys — same as Sign up Get OTP).
 */
public class OtpPage extends SplashScreen {

    private static final Pattern RESEND_SECONDS = Pattern.compile("(\\d+)\\s*s", Pattern.CASE_INSENSITIVE);

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Verify your OTP']")
    private WebElement screenTitle;

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true'][.//android.widget.TextView[@text='Continue']]")
    private WebElement continueButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Edit number']")
    private WebElement editNumber;

    @Override
    @Step("Wait for OTP screen")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() && otpEditTextCount() > 0 ? Boolean.TRUE : null,
                "OTP screen did not appear after Get OTP", java.time.Duration.ofSeconds(15));
    }

    @Step("Check 'Verify your OTP' is visible")
    public boolean isDisplayedNow() {
        return isPresent(ComposeLocators.textView("Verify your OTP"));
    }

    @Step("Check OTP subtitle is visible")
    public boolean isSubtitleVisible() {
        return isPresent(ComposeLocators.textViewContains("Please enter the OTP sent to"));
    }

    @Step("Check 'Enter your OTP' is visible")
    public boolean isEnterOtpLabelVisible() {
        return isPresent(ComposeLocators.textView("Enter your OTP"));
    }

    @Step("Check 'Edit number' is visible")
    public boolean isEditNumberVisible() {
        return isPresent(ComposeLocators.textView("Edit number"));
    }

    @Step("Check resend countdown is visible")
    public boolean isResendVisible() {
        return isPresent(ComposeLocators.textViewContains("OTP Resend"))
                || isPresent(ComposeLocators.textViewContains("Resend"));
    }

    @Step("Check Continue is visible")
    public boolean isContinueVisible() {
        return isPresent(ComposeLocators.textView("Continue"));
    }

    /** Count of OTP EditText nodes in the tree. Dump 17 Sep: 1, not six boxes. */
    @Step("Count OTP EditText nodes")
    public int otpEditTextCount() {
        return driver.findElements(By.className("android.widget.EditText")).size();
    }

    /**
     * Live EditText {@code text}. Empty when the field is blank or the node is gone.
     * Always re-finds — Compose rebuilds this node after sendKeys (case 2 stale proxy).
     */
    @Step("Read OTP EditText text")
    public String otpFieldText() {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                List<WebElement> fields = liveOtpFields();
                if (fields.isEmpty()) {
                    return "";
                }
                String raw = fields.get(0).getAttribute("text");
                return raw == null || "null".equalsIgnoreCase(raw) ? "" : raw;
            } catch (org.openqa.selenium.StaleElementReferenceException e) {
                // Compose rebuilds the EditText after sendKeys / rotate
            }
        }
        return "";
    }

    private List<WebElement> liveOtpFields() {
        return driver.findElements(By.className("android.widget.EditText"));
    }

    /**
     * Dump 17 Sep: clickable outer View wrapping Continue. {@code enabled=false} on empty
     * field; inner TextView/Button stay enabled=true decoys.
     */
    @Step("Read Continue enabled on the clickable outer View")
    public boolean isContinueEnabled() {
        try {
            List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Continue"));
            if (rows.isEmpty()) {
                return false;
            }
            String raw = rows.get(0).getAttribute("enabled");
            return Boolean.parseBoolean(raw);
        } catch (org.openqa.selenium.StaleElementReferenceException e) {
            return false;
        }
    }

    @Step("Focus the OTP EditText")
    public void focusOtpField() {
        requireOtpField().click();
    }

    @Step("Enter OTP digits via sendKeys")
    public void enterOtp(String otp) {
        WebElement field = requireOtpField();
        field.click();
        field.clear();
        field.sendKeys(otp);
    }

    private WebElement requireOtpField() {
        Waits.until(driver, d -> liveOtpFields().isEmpty() ? null : Boolean.TRUE,
                "OTP EditText not on screen", java.time.Duration.ofSeconds(8));
        return liveOtpFields().get(0);
    }

    /**
     * Extra digit keys on the already-focused field. Do not click/sendKeys again — Compose
     * EditText on Sign up replaced the value on a second sendKeys.
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

    @Step("Paste into the OTP field via clipboard + KEYCODE_PASTE")
    public void pasteOtp(String raw) {
        AndroidDriver android = (AndroidDriver) driver;
        android.setClipboardText(raw);
        WebElement field = requireOtpField();
        field.click();
        field.clear();
        android.pressKey(new KeyEvent(AndroidKey.PASTE));
    }

    @Step("Clear OTP field")
    public void clearOtp() {
        WebElement field = requireOtpField();
        field.click();
        field.clear();
    }

    @Step("Tap Continue")
    public void tapContinue() {
        tap(continueButton);
    }

    @Step("Tap Continue rapidly {times} times")
    public void tapContinueRapidly(int times) {
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Continue"));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Continue not on screen — refusing rapid tap");
        }
        org.openqa.selenium.Rectangle box = rows.get(0).getRect();
        int x = box.x + box.width / 2;
        int y = box.y + box.height / 2;
        for (int i = 0; i < times; i++) {
            driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
        }
    }

    @Step("Tap Edit number")
    public void tapEditNumber() {
        tap(editNumber);
    }

    /** Visible resend / countdown copy. Empty when the node is gone. */
    @Step("Read resend TextView/Button text")
    public String resendText() {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                return readResendTextOnce();
            } catch (org.openqa.selenium.StaleElementReferenceException e) {
                // countdown TextView rebuilds every tick / after rotate
            }
        }
        return "";
    }

    private String readResendTextOnce() {
        List<WebElement> countdown = driver.findElements(By.xpath(
                "//android.widget.TextView[contains(@text,'OTP Resend in')]"));
        if (!countdown.isEmpty()) {
            String raw = countdown.get(0).getAttribute("text");
            if (raw != null && !"null".equalsIgnoreCase(raw) && !raw.isBlank()) {
                return raw;
            }
        }
        List<WebElement> buttons = driver.findElements(By.xpath("//android.widget.Button[@text='Resend OTP']"));
        if (!buttons.isEmpty()) {
            return "Resend OTP";
        }
        List<WebElement> nodes = driver.findElements(By.xpath(
                "//android.widget.TextView[contains(@text,'OTP Resend') or contains(@text,'Resend')]"));
        if (nodes.isEmpty()) {
            return "";
        }
        String raw = nodes.get(0).getAttribute("text");
        return raw == null || "null".equalsIgnoreCase(raw) ? "" : raw;
    }

    /**
     * Dump 17 Sep empty screen: resend TextView {@code enabled=false} {@code clickable=false}
     * while {@code OTP Resend in Ns}. True only when the node is enabled or clickable.
     */
    @Step("Read resend enabled/clickable")
    public boolean isResendEnabled() {
        for (int attempt = 0; attempt < 4; attempt++) {
            try {
                return readResendEnabledOnce();
            } catch (org.openqa.selenium.StaleElementReferenceException e) {
                // countdown rebuilds every tick
            }
        }
        return false;
    }

    private boolean readResendEnabledOnce() {
        List<WebElement> nodes = driver.findElements(By.xpath(
                "//*[contains(@text,'OTP Resend') or contains(@text,'Resend')]"));
        for (WebElement node : nodes) {
            if (Boolean.parseBoolean(node.getAttribute("enabled"))
                    || Boolean.parseBoolean(node.getAttribute("clickable"))) {
                return true;
            }
        }
        List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText("Resend OTP"));
        if (!rows.isEmpty()) {
            return true;
        }
        rows = driver.findElements(ComposeLocators.clickableWithText("Resend"));
        return !rows.isEmpty();
    }

    @Step("Tap resend control")
    public void tapResend() {
        List<WebElement> buttons = driver.findElements(By.xpath("//android.widget.Button[@text='Resend OTP']"));
        List<WebElement> nodes = !buttons.isEmpty() ? buttons : driver.findElements(By.xpath(
                "//android.widget.TextView[contains(@text,'OTP Resend') or contains(@text,'Resend')]"));
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Resend control not on screen");
        }
        org.openqa.selenium.Rectangle box = nodes.get(0).getRect();
        driver.executeScript("mobile: clickGesture", Map.of(
                "x", box.x + box.width / 2,
                "y", box.y + box.height / 2));
    }

    @Step("Tap resend rapidly {times} times")
    public void tapResendRapidly(int times) {
        List<WebElement> nodes = driver.findElements(By.xpath("//android.widget.Button[@text='Resend OTP']"));
        if (nodes.isEmpty()) {
            nodes = driver.findElements(By.xpath(
                    "//*[contains(@text,'OTP Resend') or contains(@text,'Resend')]"));
        }
        if (nodes.isEmpty()) {
            throw new IllegalStateException("Resend not on screen — refusing rapid tap");
        }
        org.openqa.selenium.Rectangle box = nodes.get(0).getRect();
        int x = box.x + box.width / 2;
        int y = box.y + box.height / 2;
        for (int i = 0; i < times; i++) {
            driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
        }
    }

    /** Seconds left on {@code OTP Resend in Ns}, or -1 if not parseable. */
    @Step("Parse resend countdown seconds")
    public int resendSecondsRemaining() {
        Matcher matcher = RESEND_SECONDS.matcher(resendText());
        if (!matcher.find()) {
            return -1;
        }
        return Integer.parseInt(matcher.group(1));
    }

    @Step("Hide keyboard if shown")
    public void hideKeyboard() {
        try {
            ((AndroidDriver) driver).hideKeyboard();
        } catch (RuntimeException ignored) {
            // Already hidden.
        }
    }

    @Step("Attach OTP screen screenshot")
    public void attachScreen() {
        attachScreenshot("OTP screen");
    }

    @Step("Attach screenshot")
    public void attachScreenshot(String name) {
        super.attachScreenshot(name);
    }
}
