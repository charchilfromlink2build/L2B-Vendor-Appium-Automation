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
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

/**
 * Vendor onboarding carousel (2 slides). Copy from {@code com.l2b.app.qa} dumps
 * 2026-09-11 and re-checked 2026-09-15:
 *
 * <p>Slide 1 — {@code Grow Your Machine & Material Business With Us}: Skip (clickable
 * TextView) + Next (clickable outer View). Footer {@code Are you Customer?} /
 * {@code Book Machines or order material} sits in a clickable View with
 * content-desc {@code Customer app}.
 * <p>Slide 2 — {@code Manage Everything In One Place}: {@code Get started} only (no Skip).
 */
public class OnboardingCarouselPage extends SplashScreen {

    @AndroidFindBy(xpath = "//android.widget.TextView[contains(@text,'Grow Your Machine')]")
    private WebElement slide1Headline;

    @AndroidFindBy(xpath = "//android.widget.TextView[contains(@text,'Manage Everything')]")
    private WebElement slide2Headline;

    /**
     * Slide 1 Skip. Live dump 2026-09-15: the TextView itself is clickable
     * ({@code android.widget.TextView[@text='Skip']}), not a wrapping View.
     */
    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Skip']")
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

    /**
     * Same on-screen point, no wait between taps — a real double/triple tap.
     * Skip is top-right; Next/Get started share the bottom CTA slot, so a fast
     * Next can land on Get started.
     */
    @Step("Tap Skip rapidly {times} times")
    public void tapSkipRapidly(int times) {
        tapPointRapidly(skipButton, times);
    }

    @Step("Tap Next rapidly {times} times")
    public void tapNextRapidly(int times) {
        tapPointRapidly(nextButton, times);
    }

    @Step("Tap Get started on onboarding slide 2")
    public void tapGetStarted() {
        if (!isOnSlideTwo()) {
            throw new IllegalStateException("Not on onboarding slide 2 — refusing Get started");
        }
        tap(getStartedButton);
    }

    @Step("Check Skip is absent (slide 2 must not keep a Skip CTA)")
    public boolean isSkipAbsent() {
        return !isSkipVisible();
    }

    @Step("Check onboarding carousel is gone")
    public boolean isGone() {
        return !isOnSlideOne() && !isOnSlideTwo();
    }

    /**
     * Tap the customer footer. Dump 2026-09-15: clickable parent View wrapping
     * {@code Are you Customer?} + {@code Book Machines or order material},
     * child content-desc {@code Customer app}. Prefer that wrap; fall back to the label.
     */
    @Step("Tap 'Are you Customer?'")
    public void tapCustomerPrompt() {
        List<WebElement> wrap = driver.findElements(
                ComposeLocators.clickableWithText("Are you Customer?"));
        if (!wrap.isEmpty()) {
            tap(wrap.get(0));
            return;
        }
        List<WebElement> byDesc = driver.findElements(
                org.openqa.selenium.By.xpath("//*[@content-desc='Customer app']"));
        if (!byDesc.isEmpty()) {
            byDesc.get(0).click();
            return;
        }
        List<WebElement> label = driver.findElements(ComposeLocators.textView("Are you Customer?"));
        if (label.isEmpty()) {
            throw new IllegalStateException("'Are you Customer?' not on screen — refusing tap");
        }
        label.get(0).click();
    }

    /** Horizontal pager swipe toward slide 2 (left). Confirmed only by the live gesture, not by copy. */
    @Step("Swipe toward next onboarding slide")
    public void swipeTowardNext() {
        swipe("left");
    }

    /** Horizontal pager swipe toward slide 1 (right). */
    @Step("Swipe toward previous onboarding slide")
    public void swipeTowardPrevious() {
        swipe("right");
    }

    @Step("Press Android Back on onboarding")
    public void pressBack() {
        ((AndroidDriver) driver).pressKey(new KeyEvent(AndroidKey.BACK));
    }

    @Step("Read current Android package")
    public String currentPackage() {
        return ((AndroidDriver) driver).getCurrentPackage();
    }

    /**
     * Dump 15 Sep: two 126×126 Views sit just above Next/Get started (the visual
     * pill + dot). They have no text, content-desc, or {@code selected=true}, so
     * highlight-sync cannot be asserted from UiAutomator — only presence.
     */
    @Step("Count pager-indicator Views above the bottom CTA")
    public int pagerIndicatorNodeCount() {
        org.openqa.selenium.Rectangle cta = ctaBounds();
        if (cta == null) {
            return 0;
        }
        int count = 0;
        for (WebElement view : driver.findElements(org.openqa.selenium.By.className("android.view.View"))) {
            org.openqa.selenium.Rectangle box;
            try {
                box = view.getRect();
            } catch (RuntimeException e) {
                continue;
            }
            boolean sizeLooksLikeDot = box.width >= 80 && box.width <= 160
                    && box.height >= 80 && box.height <= 160;
            boolean sitsJustAboveCta = box.y < cta.y && box.y > cta.y - 220;
            if (sizeLooksLikeDot && sitsJustAboveCta) {
                count++;
            }
        }
        return count;
    }

    @Step("Check whether any pager-indicator node is selected or labelled")
    public boolean pagerIndicatorExposesSelectedState() {
        org.openqa.selenium.Rectangle cta = ctaBounds();
        if (cta == null) {
            return false;
        }
        for (WebElement view : driver.findElements(org.openqa.selenium.By.className("android.view.View"))) {
            org.openqa.selenium.Rectangle box;
            try {
                box = view.getRect();
            } catch (RuntimeException e) {
                continue;
            }
            boolean sizeLooksLikeDot = box.width >= 80 && box.width <= 160
                    && box.height >= 80 && box.height <= 160;
            boolean sitsJustAboveCta = box.y < cta.y && box.y > cta.y - 220;
            if (!sizeLooksLikeDot || !sitsJustAboveCta) {
                continue;
            }
            if (meaningful(view.getAttribute("selected"), true)
                    || meaningful(view.getAttribute("contentDescription"), false)) {
                return true;
            }
        }
        return false;
    }

    /** Appium often returns the literal string "null" for missing attributes. */
    private static boolean meaningful(String value, boolean mustBeTrue) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return false;
        }
        if (mustBeTrue) {
            return "true".equalsIgnoreCase(value);
        }
        return true;
    }

    private org.openqa.selenium.Rectangle ctaBounds() {
        List<WebElement> next = driver.findElements(ComposeLocators.clickableWithText("Next"));
        if (!next.isEmpty()) {
            return next.get(0).getRect();
        }
        List<WebElement> started = driver.findElements(ComposeLocators.clickableWithText("Get started"));
        if (!started.isEmpty()) {
            return started.get(0).getRect();
        }
        return null;
    }

    private void tapPointRapidly(WebElement element, int times) {
        org.openqa.selenium.Rectangle box = Waits.clickable(driver, element).getRect();
        int x = box.x + box.width / 2;
        int y = box.y + box.height / 2;
        for (int i = 0; i < times; i++) {
            driver.executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
        }
    }

    private void swipe(String direction) {
        Dimension size = driver.manage().window().getSize();
        int left = (int) (size.width * 0.10);
        int top = (int) (size.height * 0.35);
        int width = (int) (size.width * 0.80);
        int height = (int) (size.height * 0.25);
        driver.executeScript("mobile: swipeGesture", Map.of(
                "left", left,
                "top", top,
                "width", width,
                "height", height,
                "direction", direction,
                "percent", 0.75));
    }
}
