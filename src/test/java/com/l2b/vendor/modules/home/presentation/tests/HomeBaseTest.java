package com.l2b.vendor.modules.home.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.bookings.presentation.tests.quickbooking.QuickBookingBaseTest;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import java.time.Duration;
import org.openqa.selenium.TimeoutException;

/**
 * Shared OTP login for Home cases on {@code 9000000017}. Do not use 0001/0002/0003.
 */
public abstract class HomeBaseTest extends QuickBookingBaseTest {

    protected HomePage loginToHomeOrQueue() {
        OtpPage otp = openOtp(materialPhone());
        otp.focusOtpField();
        if (!otp.otpFieldText().isEmpty()) {
            otp.clearOtp();
        }
        otp.pressDigitKeys("1234");
        HomePage home = new HomePage();
        QuickBookingPage qb = new QuickBookingPage();
        try {
            Waits.until(DriverManager.get(),
                    d -> (home.isDisplayedNow() || qb.isDisplayedNow()) ? Boolean.TRUE : null,
                    "Home or Quick Booking did not appear after OTP",
                    Duration.ofSeconds(12));
        } catch (TimeoutException first) {
            if (otp.isDisplayedNow() && otp.otpFieldText().isEmpty()) {
                otp.pasteOtp("1234");
            }
            Waits.until(DriverManager.get(),
                    d -> (home.isDisplayedNow() || qb.isDisplayedNow()) ? Boolean.TRUE : null,
                    "Home or Quick Booking did not appear after OTP retry",
                    Duration.ofSeconds(15));
        }
        return home;
    }

    protected String namedAfterTap(HomePage home, QuickBookingPage qb) {
        if (profileDrawerNow()) {
            return "home-drawer";
        }
        if (qb.isDisplayedNow()) {
            return "quick-booking";
        }
        if (home.isDisplayedNow()) {
            return "home";
        }
        String src = DriverManager.get().getPageSource();
        if (src.contains("Verify your OTP")) {
            return "otp";
        }
        if (src.contains("Notifications") && !home.isCurrentEarningVisible()) {
            return "notifications-or-other";
        }
        return "left-home";
    }
}
