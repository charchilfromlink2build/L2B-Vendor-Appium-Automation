package com.l2b.vendor.pages;

import com.l2b.vendor.utils.ComposeLocators;
import com.l2b.vendor.utils.Waits;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.qameta.allure.Step;
import org.openqa.selenium.WebElement;

/**
 * Vendor onboarding carousel (2 slides). Copy confirmed 2026-09-11.
 *
 * <p>Slide 1 — {@code Grow Your Machine & Material Business With Us}: Skip + Next.
 * Slide 2 — {@code Manage Everything In One Place}: {@code Get started} only (no Skip).
 */
public class OnboardingCarouselPage extends BaseScreen {

    @AndroidFindBy(xpath = "//android.widget.TextView[contains(@text,'Grow Your Machine')]")
    private WebElement slide1Headline;

    @AndroidFindBy(xpath = "//android.widget.TextView[contains(@text,'Manage Everything')]")
    private WebElement slide2Headline;

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true'][.//android.widget.TextView[@text='Skip']]")
    private WebElement skipButton;

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true'][.//android.widget.TextView[@text='Next']]")
    private WebElement nextButton;

    @AndroidFindBy(xpath = "//android.view.View[@clickable='true'][.//android.widget.TextView[@text='Get started']]")
    private WebElement getStartedButton;

    @Override
    @Step("Wait for onboarding carousel")
    public void waitUntilLoaded() {
        waitUntilSlideOne();
    }

    @Step("Wait for onboarding slide 1")
    public void waitUntilSlideOne() {
        Waits.visible(driver, slide1Headline);
        Waits.visible(driver, nextButton);
    }

    @Step("Wait for onboarding slide 2")
    public void waitUntilSlideTwo() {
        Waits.until(driver, d -> isOnSlideTwo() ? Boolean.TRUE : null,
                "Onboarding slide 2 (Manage Everything) to load");
    }

    @Step("Check onboarding carousel is loaded")
    public boolean isLoaded() {
        return isOnSlideOne() || isOnSlideTwo()
                || isPresent(ComposeLocators.clickableWithText("Next"))
                || isPresent(ComposeLocators.clickableWithText("Get started"));
    }

    @Step("Check onboarding is on slide 1")
    public boolean isOnSlideOne() {
        return isPresent(ComposeLocators.textViewContains("Grow Your Machine"));
    }

    @Step("Check onboarding is on slide 2")
    public boolean isOnSlideTwo() {
        return isPresent(ComposeLocators.textViewContains("Manage Everything"));
    }

    @Step("Check slide 1 body copy is visible")
    public boolean isSlideOneBodyVisible() {
        return isPresent(ComposeLocators.textViewContains("List your machines easily"));
    }

    @Step("Check Skip is visible on slide 1")
    public boolean isSkipVisible() {
        return isPresent(ComposeLocators.textView("Skip"));
    }

    @Step("Check Next is visible on slide 1")
    public boolean isNextVisible() {
        return isPresent(ComposeLocators.textView("Next"));
    }

    @Step("Check 'Are you Customer?' is visible")
    public boolean isCustomerPromptVisible() {
        return isPresent(ComposeLocators.textView("Are you Customer?"));
    }

    @Step("Check slide 2 body copy is visible")
    public boolean isSlideTwoBodyVisible() {
        return isPresent(ComposeLocators.textViewContains("Track orders, inventory, deliveries"));
    }

    @Step("Check 'Get started' is visible on slide 2")
    public boolean isGetStartedVisible() {
        return isPresent(ComposeLocators.textView("Get started"));
    }

    @Step("Attach onboarding screenshot")
    public void attachScreen(String label) {
        attachScreenshot(label);
    }

    @Step("Tap Next on onboarding")
    public void tapNext() {
        tap(nextButton);
    }

    @Step("Tap Skip on onboarding")
    public void tapSkip() {
        tap(skipButton);
    }

    @Step("Tap Get started on onboarding slide 2")
    public void tapGetStarted() {
        if (!isOnSlideTwo()) {
            throw new IllegalStateException("Not on onboarding slide 2 — refusing Get started");
        }
        tap(getStartedButton);
    }
}
