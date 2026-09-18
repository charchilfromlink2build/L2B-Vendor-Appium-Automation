package com.l2b.vendor.modules.quickbooking.presentation.pages;

import com.l2b.vendor.core.ui.SplashScreen;
import io.qameta.allure.Step;

/**
 * Quick Booking detail sheet opened from a landing card ({@code View More Details}).
 * Locators must be dump-sourced before any case uses this page.
 */
public class QuickBookingDetailsPage extends SplashScreen {

    @Override
    @Step("Wait for Quick Booking details")
    public void waitUntilLoaded() {
        throw new UnsupportedOperationException(
                "QuickBookingDetailsPage locators are not dump-sourced yet");
    }

    @Step("Check Quick Booking details are visible")
    public boolean isDisplayedNow() {
        throw new UnsupportedOperationException(
                "QuickBookingDetailsPage locators are not dump-sourced yet");
    }
}
