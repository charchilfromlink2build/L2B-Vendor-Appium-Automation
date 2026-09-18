package com.l2b.vendor.modules.home.presentation.pages;

import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.SplashScreen;
import com.l2b.vendor.core.wait.Waits;
import io.qameta.allure.Step;
import java.time.Duration;
import org.openqa.selenium.By;

/**
 * Logged-in Home. Copy from live dump 18 Sep 2026
 * ({@code /tmp/l2b-qb-discovery/rental-individual-0003/window.xml}): greeting
 * {@code Good Afternoon!}, {@code Current Earning}, bottom tabs {@code Home} /
 * {@code Earning} / {@code Fleet} / {@code Calendar}. Greeting is time-of-day
 * specific. Quick Booking is a different screen and must not count as Home.
 */
public class HomePage extends SplashScreen {

    @Override
    @Step("Wait for Home")
    public void waitUntilLoaded() {
        Waits.until(driver, d -> isDisplayedNow() ? Boolean.TRUE : null,
                "Home screen did not appear", Duration.ofSeconds(15));
    }

    @Step("Check Home identity (greeting or Current Earning, not Quick Booking app bar)")
    public boolean isDisplayedNow() {
        if (isQuickBookingAppBar()) {
            return false;
        }
        return isPresent(ComposeLocators.textView("Current Earning"))
                || isPresent(ComposeLocators.textView("Good Morning!"))
                || isPresent(ComposeLocators.textView("Good Afternoon!"))
                || isPresent(ComposeLocators.textView("Good Evening!"));
    }

    @Step("Check Current Earning is visible")
    public boolean isCurrentEarningVisible() {
        return isPresent(ComposeLocators.textView("Current Earning"));
    }

    @Step("Check Home bottom tab is visible")
    public boolean isHomeTabVisible() {
        return isPresent(ComposeLocators.textView("Home"));
    }

    @Step("Check Profile affordance is visible")
    public boolean isProfileVisible() {
        return isPresent(By.xpath("//*[@content-desc='Profile']"));
    }

    /**
     * Quick Booking app bar only. Home booking-order cards can reuse the words
     * {@code Quick Booking}, so that string is not a screen identity.
     */
    private boolean isQuickBookingAppBar() {
        return isPresent(By.xpath("//*[@content-desc='Close']"));
    }
}
