package com.l2b.vendor.pages;

import com.l2b.vendor.utils.ComposeLocators;
import com.l2b.vendor.utils.Waits;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * First-launch language screen. Copy from live vendor dump 2026-09-11
 * ({@code com.l2b.app.qa}): title {@code Welcome to L2B}, CTA {@code Get started}.
 */
public class LanguagePage extends BaseScreen {

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

    @Step("Select English")
    public void selectEnglish() {
        tap(englishOption);
    }

    @Step("Tap Get started on language screen")
    public void tapGetStarted() {
        if (!isDisplayedNow()) {
            throw new IllegalStateException("Language screen is not displayed — refusing Get started");
        }
        tap(getStartedButton);
    }
}
