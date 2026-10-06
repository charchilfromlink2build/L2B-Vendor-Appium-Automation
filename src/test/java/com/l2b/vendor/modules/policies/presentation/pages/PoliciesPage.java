package com.l2b.vendor.modules.policies.presentation.pages;

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
 * Policies from Profile drawer. Dump {@code /tmp/l2b-policies-0001-20261005}.
 * Labels from language dump 1 Oct. Never Log Out Confirm / Accept / Decline.
 */
public class PoliciesPage extends SplashScreen {

    public static final String SEC_COLLECT = "What we collect";
    public static final String SEC_LOCATION = "Location";
    public static final String SEC_USE = "How we use it";
    public static final String SEC_RIGHTS = "Your rights";
    public static final String NO_SELL = "We do not sell your personal data.";
    public static final String BULLET_MACHINE =
            "Machine and vehicle location while a job is in progress";

    @Override
    @Step("Wait for Policies")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Policies screen did not appear", Duration.ofSeconds(20));
    }

    @Step("Check Policies identity")
    public boolean isDisplayedNow() {
        return isPoliciesTitleVisible()
                || isPresent(ComposeLocators.textView(SEC_COLLECT))
                || isPresent(ComposeLocators.textView(SEC_USE))
                || isPresent(ComposeLocators.textView(SEC_RIGHTS))
                || isPresent(ComposeLocators.textView(SEC_LOCATION))
                || isErrorVisible();
    }

    @Step("Check Policies title")
    public boolean isPoliciesTitleVisible() {
        return isPresent(ComposeLocators.textView("Policies"))
                || isPresent(ComposeLocators.textView("नीतियाँ"));
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

    @Step("Full policy chrome")
    public boolean isFullChromeVisible() {
        return (isPresent(ComposeLocators.textView(SEC_COLLECT))
                || isPresent(ComposeLocators.textViewContains("What we collect"))
                || isPresent(ComposeLocators.textViewContains("हम कौन-सी जानकारी")))
                && (isPresent(ComposeLocators.textView(SEC_USE))
                || isPresent(ComposeLocators.textView(SEC_LOCATION)));
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

    @Step("Swipe policies body up")
    public void swipeBodyUp() {
        driver.executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 1400, "width", 600, "height", 700,
                        "direction", "up", "percent", 0.7));
    }

    @Step("Swipe policies body down")
    public void swipeBodyDown() {
        driver.executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 800, "width", 600, "height", 700,
                        "direction", "down", "percent", 0.7));
    }
}
