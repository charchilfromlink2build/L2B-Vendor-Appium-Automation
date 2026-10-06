package com.l2b.vendor.modules.terms.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

/**
 * Terms & Services from Profile drawer. Dump {@code /tmp/l2b-terms-0001-20261003}.
 * Labels from language dump 1 Oct. Never Log Out Confirm / Accept booking / Decline.
 */
public class TermsPage extends SplashScreen {

    public static final String SEC_ACCEPTING = "Accepting these terms";
    public static final String SEC_OBLIGATIONS = "Your obligations";
    public static final String SEC_MUST_NOT = "You must not";
    public static final String SEC_PAYMENTS = "Payments and deductions";
    public static final String SEC_SUSPEND = "Suspension and termination";
    public static final String BULLET_OFF_PLATFORM =
            "Ask a customer to settle a booking outside the platform";

    @Override
    @Step("Wait for Terms & Services")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Terms & Services screen did not appear", Duration.ofSeconds(20));
    }

    @Step("Check Terms identity")
    public boolean isDisplayedNow() {
        return isTermsTitleVisible()
                || isPresent(ComposeLocators.textView(SEC_ACCEPTING))
                || isPresent(ComposeLocators.textView(SEC_OBLIGATIONS))
                || isPresent(ComposeLocators.textView(SEC_PAYMENTS))
                || isErrorVisible();
    }

    @Step("Check Terms title")
    public boolean isTermsTitleVisible() {
        return isPresent(ComposeLocators.textView("Terms & Services"))
                || isPresent(ComposeLocators.textView("Terms and Services"))
                || isPresent(ComposeLocators.textView("नियम एवं सेवाएँ"));
    }

    @Step("Check load error empty state")
    public boolean isErrorVisible() {
        return isPresent(ComposeLocators.textViewContains("Something went wrong"))
                || isPresent(ComposeLocators.textViewContains("not available"));
    }

    @Step("Check Retry")
    public boolean isRetryVisible() {
        return isPresent(ComposeLocators.textView("Retry"));
    }

    @Step("Full legal chrome")
    public boolean isFullChromeVisible() {
        return isPresent(ComposeLocators.textView(SEC_ACCEPTING))
                && isPresent(ComposeLocators.textView(SEC_OBLIGATIONS));
    }

    @Step("Section visible")
    public boolean isSectionVisible(String section) {
        return isPresent(ComposeLocators.textView(section))
                || isPresent(ComposeLocators.textViewContains(section));
    }

    @Step("Tap Retry")
    public void tapRetry() {
        tap(driver.findElement(ComposeLocators.textView("Retry")));
    }

    @Step("Check Back")
    public boolean isBackVisible() {
        return isPresent(By.xpath("//*[@content-desc='Back']"));
    }

    @Step("Header Back left-aligned")
    public boolean headerBackAligned() {
        List<WebElement> backs = driver.findElements(By.xpath("//*[@content-desc='Back']"));
        if (backs.isEmpty()) {
            return false;
        }
        Rectangle r = backs.get(0).getRect();
        return r.x < 120 && r.y < 220;
    }

    @Step("Tap Back")
    public void tapBack() {
        tap(driver.findElement(By.xpath("//*[@content-desc='Back']")));
    }

    @Step("Device Back")
    public void pressDeviceBack() {
        driver.navigate().back();
    }

    @Step("Swipe terms body up")
    public void swipeBodyUp() {
        driver.executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 1400, "width", 600, "height", 700,
                        "direction", "up", "percent", 0.7));
    }

    @Step("Swipe terms body down")
    public void swipeBodyDown() {
        driver.executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 800, "width", 600, "height", 700,
                        "direction", "down", "percent", 0.7));
    }
}
