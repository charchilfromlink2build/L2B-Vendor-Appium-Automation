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
 * First-launch language chooser for {@code com.l2b.app.qa}. Copy from live vendor dump 2026-09-11
 * (title {@code Welcome to L2B}, CTA {@code Get started}). Most locators match visible text and will
 * break if product copy changes or the screen is already localized.
 */
public class LanguagePage extends SplashScreen {

    /** English title only — missing after Hindi/Telugu/Kannada is selected. Fragile to copy change. */
    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Welcome to L2B']")
    private WebElement screenTitle;

    /** Clickable Compose row wrapping the English label. Text-matched; unused if selectLanguage() is used. */
    @AndroidFindBy(xpath = "//android.view.View[@clickable='true'][.//android.widget.TextView[@text='English']]")
    private WebElement englishOption;

    /** English Get started only. tapGetStarted() also tries localized CTAs because this field is English-only. */
    @AndroidFindBy(xpath = "(//android.view.View[@clickable='true'][.//android.widget.TextView[@text='Get started']])[last()]")
    private WebElement getStartedButton;

    /**
     * Dismiss the notification prompt whenever it appears, then wait for the English language title.
     * Polls both because a late permission dialog after a slow UiAutomator2 recreate can otherwise
     * time out waiting for {@code Welcome to L2B} while the dialog is still on top.
     */
    @Override
    @Step("Wait for language screen")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> {
            dismissNotificationPromptIfPresent();
            return isDisplayedNow() ? Boolean.TRUE : null;
        }, "Language screen did not appear after splash", Duration.ofSeconds(25));
    }

    /** Wait until the English title is present, with a splash-specific timeout message. */
    @Step("Wait for language after splash, timeout {timeout}")
    public void waitUntilVisible(Duration timeout) {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Splash did not proceed to language within " + timeout.toSeconds() + "s",
                timeout);
    }

    /** True when the English title "Welcome to L2B" is on screen. Text-matched; false after locale change. */
    @Step("Check language screen is visible")
    public boolean isDisplayedNow() {
        return isPresent(ComposeLocators.textView("Welcome to L2B"));
    }

    /** True when the partner-logo content-desc is present. Fragile if the a11y label is rewritten. */
    @Step("Check partner logo is visible")
    public boolean isPartnerLogoVisible() {
        return isPresent(By.xpath("//*[@content-desc='Welcome to Link2Build Partner Portal']"));
    }

    /** True when the English subtitle about machines, jobs, and earnings is present. Text-matched. */
    @Step("Check language subtitle is visible")
    public boolean isSubtitleVisible() {
        return isPresent(ComposeLocators.textView(
                "A smart platform to manage machines, jobs, and earnings."));
    }

    /** True when the English heading "Choose your language" is present. Text-matched. */
    @Step("Check 'Choose your language' is visible")
    public boolean isChooseLanguageVisible() {
        return isPresent(ComposeLocators.textView("Choose your language"));
    }

    /** True when the English language label is on screen. */
    @Step("Check English option is visible")
    public boolean isEnglishVisible() {
        return isPresent(ComposeLocators.textView("English"));
    }

    /** True when any "Default" badge TextView is on screen (English copy). Localized badge is not this locator. */
    @Step("Check Default badge is visible")
    public boolean isDefaultBadgeVisible() {
        return isPresent(ComposeLocators.textView("Default"));
    }

    /** True when the Hindi row label हिंदी is on screen. */
    @Step("Check Hindi option is visible")
    public boolean isHindiVisible() {
        return isPresent(ComposeLocators.textView("हिंदी"));
    }

    /** True when the Telugu row label తెలుగు is on screen. */
    @Step("Check Telugu option is visible")
    public boolean isTeluguVisible() {
        return isPresent(ComposeLocators.textView("తెలుగు"));
    }

    /** True when the Kannada row label ಕನ್ನಡ is on screen. */
    @Step("Check Kannada option is visible")
    public boolean isKannadaVisible() {
        return isPresent(ComposeLocators.textView("ಕನ್ನಡ"));
    }

    /** True when English "Get started" text is present. Localized CTAs are handled in tapGetStarted(). */
    @Step("Check 'Get started' is visible on language screen")
    public boolean isGetStartedVisible() {
        return isPresent(ComposeLocators.textView("Get started"));
    }

    /** Attach a screenshot named for the smoke language step. */
    @Step("Attach language screen screenshot")
    public void attachScreen() {
        attachScreenshot("02 Language screen");
    }

    /** Attach a PNG to Allure under the given name. */
    @Step("Attach screenshot")
    public void attachScreenshot(String name) {
        super.attachScreenshot(name);
    }

    /** Tap the clickable row whose child TextView matches the language label. Text-matched; throws if missing. */
    @Step("Select language row {languageLabel}")
    public void selectLanguage(String languageLabel) {
        java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(languageLabel));
        if (rows.isEmpty()) {
            throw new IllegalStateException("Language row not on screen: " + languageLabel);
        }
        tap(rows.get(0));
    }

    /** Tap the English language row. */
    @Step("Select English")
    public void selectEnglish() {
        selectLanguage("English");
    }

    /** Tap the Hindi language row (native label हिंदी). */
    @Step("Select Hindi")
    public void selectHindi() {
        selectLanguage("हिंदी");
    }

    /** Tap the Telugu language row (native label తెలుగు). */
    @Step("Select Telugu")
    public void selectTelugu() {
        selectLanguage("తెలుగు");
    }

    /** Tap the Kannada language row (native label ಕನ್ನಡ). */
    @Step("Select Kannada")
    public void selectKannada() {
        selectLanguage("ಕನ್ನಡ");
    }

    /** Tap the given language rows in order with no wait, using coordinate clicks to simulate rapid multi-tap. */
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

    /** True when the English welcome title is present. Text-matched. */
    @Step("Check title is English 'Welcome to L2B'")
    public boolean isTitleEnglish() {
        return isPresent(ComposeLocators.textView("Welcome to L2B"));
    }

    /** True when the Hindi welcome title is present. Text-matched; copy change will break this. */
    @Step("Check title is Hindi")
    public boolean isTitleHindi() {
        return isPresent(ComposeLocators.textView("L2B में आपका स्वागत है"));
    }

    /** True when the Telugu welcome title is present. Text-matched; copy change will break this. */
    @Step("Check title is Telugu")
    public boolean isTitleTelugu() {
        return isPresent(ComposeLocators.textView("L2B కి స్వాగతం"));
    }

    /** True when the Kannada welcome title is present. Text-matched; copy change will break this. */
    @Step("Check title is Kannada")
    public boolean isTitleKannada() {
        return isPresent(ComposeLocators.textView("L2B ಗೆ ಸ್ವಾಗತ"));
    }

    /** Tap Get started using English or localized CTA text. Fragile if any of those four strings change. */
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

    /** Bounding rectangle of the clickable row for the given language label, or null if missing. */
    @Step("Read language row bounds for {languageLabel}")
    public org.openqa.selenium.Rectangle languageRowBounds(String languageLabel) {
        java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(languageLabel));
        return rows.isEmpty() ? null : rows.get(0).getRect();
    }

    /** Bounding rectangle of the CheckBox inside that language row, or null if the box is missing. */
    @Step("Read language CheckBox bounds for {languageLabel}")
    public org.openqa.selenium.Rectangle languageCheckboxBounds(String languageLabel) {
        java.util.List<WebElement> rows = driver.findElements(ComposeLocators.clickableWithText(languageLabel));
        if (rows.isEmpty()) {
            return null;
        }
        java.util.List<WebElement> boxes = rows.get(0).findElements(org.openqa.selenium.By.className("android.widget.CheckBox"));
        return boxes.isEmpty() ? null : boxes.get(0).getRect();
    }

    /**
     * {@code true}/{@code false} when the row CheckBox exposes checked; {@code null}
     * when the node is missing or Appium returns the string {@code null}. Dumps often leave all four false.
     */
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
