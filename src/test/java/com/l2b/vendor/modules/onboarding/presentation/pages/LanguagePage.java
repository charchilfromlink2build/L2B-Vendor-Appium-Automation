package com.l2b.vendor.modules.onboarding.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.qameta.allure.Step;
import java.time.Duration;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * First-launch language screen. Copy from live vendor dump 2026-09-11
 * ({@code com.l2b.app.qa}): title {@code Welcome to L2B}, CTA {@code Get started}.
 */
public class LanguagePage extends SplashScreen {

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Welcome to L2B']")
    private WebElement screenTitle;

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true'][.//android.widget.TextView[@text='English']]")
    private WebElement englishOption;

    @AndroidFindBy(xpath = "(//android.view.View[@clickable='true'][.//android.widget.TextView[@text='Get started']])[last()]")
    private WebElement getStartedButton;

    @Override
    @Step("Wait for language screen")
    public void waitUntilLoaded() {
        dismissNotificationPromptIfPresent();
        Waits.visible(driver, screenTitle);
    }

    @Step("Wait for language after splash, timeout {timeout}")
    public void waitUntilVisible(Duration timeout) {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Splash did not proceed to language within " + timeout.toSeconds() + "s",
                timeout);
    }

    @Step("Check language screen is visible")
    public boolean isDisplayedNow() {
        return isPresent(ComposeLocators.textView("Welcome to L2B"));
    }

    @Step("Check partner logo is visible")
    public boolean isPartnerLogoVisible() {
        return isPresent(By.xpath("//*[@content-desc='Welcome to Link2Build Partner Portal']"));
    }

    @Step("Check language subtitle is visible")
    public boolean isSubtitleVisible() {
        return isPresent(ComposeLocators.textView(
                "A smart platform to manage machines, jobs, and earnings."));
    }

    @Step("Check 'Choose your language' is visible")
    public boolean isChooseLanguageVisible() {
        return isPresent(ComposeLocators.textView("Choose your language"));
    }

    @Step("Check English option is visible")
    public boolean isEnglishVisible() {
        return isPresent(ComposeLocators.textView("English"));
    }

    @Step("Check Default badge is visible")
    public boolean isDefaultBadgeVisible() {
        return isPresent(ComposeLocators.textView("Default"));
    }

    @Step("Check Hindi option is visible")
    public boolean isHindiVisible() {
        return isPresent(ComposeLocators.textView("हिंदी"));
    }

    @Step("Check Telugu option is visible")
    public boolean isTeluguVisible() {
        return isPresent(ComposeLocators.textView("తెలుగు"));
    }

    @Step("Check Kannada option is visible")
    public boolean isKannadaVisible() {
        return isPresent(ComposeLocators.textView("ಕನ್ನಡ"));
    }

    @Step("Check 'Get started' is visible on language screen")
    public boolean isGetStartedVisible() {
        return isPresent(ComposeLocators.textView("Get started"));
    }

    @Step("Attach language screen screenshot")
    public void attachScreen() {
        attachScreenshot("02 Language screen");
    }

    @Step("Attach screenshot")
    public void attachScreenshot(String name) {
        super.attachScreenshot(name);
    }

    @Step("Select language row {languageLabel}")
    public void selectLanguage(String languageLabel) {
        java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(languageLabel));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Language row not on screen: " + languageLabel);
        }
        tap(rows.get(0));
    }

    @Step("Select English")
    public void selectEnglish() {
        selectLanguage("English");
    }

    @Step("Select Hindi")
    public void selectHindi() {
        selectLanguage("हिंदी");
    }

    @Step("Select Telugu")
    public void selectTelugu() {
        selectLanguage("తెలుగు");
    }

    @Step("Select Kannada")
    public void selectKannada() {
        selectLanguage("ಕನ್ನಡ");
    }

    @Step("Tap language rows rapidly in order")
    public void tapLanguageRowsRapidly(String... labels) {
        for (String label : labels) {
            java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(label));
            if (rows.isEmpty()) {
                continue;
            }
            org.openqa.selenium.Rectangle box = rows.get(0).getRect();
            driver.executeScript("mobile: clickGesture", java.util.Map.of(
                    "x", box.x + box.width / 2,
                    "y", box.y + box.height / 2));
        }
    }

    @Step("Check title is English 'Welcome to L2B'")
    public boolean isTitleEnglish() {
        return isPresent(ComposeLocators.textView("Welcome to L2B"));
    }

    @Step("Check title is Hindi")
    public boolean isTitleHindi() {
        return isPresent(ComposeLocators.textView("L2B में आपका स्वागत है"));
    }

    @Step("Check title is Telugu")
    public boolean isTitleTelugu() {
        return isPresent(ComposeLocators.textView("L2B కి స్వాగతం"));
    }

    @Step("Check title is Kannada")
    public boolean isTitleKannada() {
        return isPresent(ComposeLocators.textView("L2B ಗೆ ಸ್ವಾಗತ"));
    }

    @Step("Tap Get started on language screen")
    public void tapGetStarted() {
        if (!(isTitleEnglish() || isTitleHindi() || isTitleTelugu() || isTitleKannada())) {
            throw new IllegalStateException("Language screen is not displayed — refusing Get started");
        }
        for (String cta : java.util.List.of("Get started", "शुरू करें", "ప్రారంభించండి", "ಪ್ರಾರಂಭಿಸಿ")) {
            java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(cta));
            if (!rows.isEmpty()) {
                tap(rows.get(rows.size() - 1));
                return;
            }
        }
        throw new IllegalStateException("Get started CTA not on language screen");
    }

    /**
     * Dump 15 Sep 2026: {@code Default} sits on the English clickable row, not on
     * Hindi/Telugu/Kannada. CheckBox {@code checked} is often unset in UiAutomator.
     */
    @Step("Check Default badge is on the English row")
    public boolean isDefaultBadgeOnEnglishRow() {
        return !driver.findElements(By.xpath(
                "//android.view.View[@clickable='true']"
                        + "[.//android.widget.TextView[@text='English']]"
                        + "[.//android.widget.TextView[@text='Default' or @text='डिफ़ॉल्ट']]"))
                .isEmpty();
    }

    /**
     * {@code true}/{@code false} when the row CheckBox exposes checked; {@code null}
     * when the node is missing or Appium returns the string {@code null}.
     */
    @Step("Read language row bounds for {languageLabel}")
    public org.openqa.selenium.Rectangle languageRowBounds(String languageLabel) {
        java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(languageLabel));
        return rows.isEmpty() ? null : rows.get(0).getRect();
    }

    @Step("Read language CheckBox bounds for {languageLabel}")
    public org.openqa.selenium.Rectangle languageCheckboxBounds(String languageLabel) {
        java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(languageLabel));
        if (rows.isEmpty()) {
            return null;
        }
        java.util.List<WebElement> boxes = rows.get(0).findElements(org.openqa.selenium.By.className("android.widget.CheckBox"));
        return boxes.isEmpty() ? null : boxes.get(0).getRect();
    }

    @Step("Read CheckBox checked for language {languageLabel}")
    public Boolean checkboxChecked(String languageLabel) {
        java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(languageLabel));
        if (rows.isEmpty()) {
            return null;
        }
        java.util.List<WebElement> boxes = rows.get(0).findElements(By.className("android.widget.CheckBox"));
        if (boxes.isEmpty()) {
            return null;
        }
        String raw = boxes.get(0).getAttribute("checked");
        if (raw == null || raw.isBlank() || "null".equalsIgnoreCase(raw)) {
            return null;
        }
        return Boolean.parseBoolean(raw);
    }
}
