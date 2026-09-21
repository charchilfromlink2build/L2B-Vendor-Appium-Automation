package com.l2b.vendor.modules.home.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.bookings.presentation.tests.quickbooking.QuickBookingBaseTest;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.time.Duration;
import org.openqa.selenium.TimeoutException;
import org.testng.annotations.Test;

/**
 * Rental Home overlay-only landing for {@code 9000000001}. Isolated
 * {@code home/home-rental-landing.xml}. Dump 21 Sep
 * {@code /tmp/l2b-home-rental-0001-20260921/01-after-close}.
 * Do not tap Accept, Decline, or View More Details. BUGS_FOUND #15 — do not
 * open a new bug number for the same dialog.
 */
@Epic("Vendor app")
@Feature("Home landing — rental vendor 9000000001 extend-time overlay")
public class RentalHomeLandingTest extends QuickBookingBaseTest {

    @Test(priority = 1, description = "RH-L1: post-OTP on 9000000001 lands on Quick Booking, not Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 + 1234. Pending rentals must open Quick Booking (title + Close). "
            + "Do not tap Accept/Decline/View More Details or Close.")
    public void postOtpLandsOnQuickBooking() {
        QuickBookingPage qb = loginRental0001ToQueue();
        HomePage home = new HomePage();
        QuickBookingLandingPage landing = new QuickBookingLandingPage();
        OtpPage otp = new OtpPage();

        Allure.parameter("phone", rentalCompanyPhone());
        Allure.parameter("package", String.valueOf(vendorPackage()));
        Allure.parameter("quickBooking", String.valueOf(qb.isDisplayedNow()));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("extendTime", String.valueOf(landing.isExtendTimeDialogVisible()));
        Allure.parameter("otp", String.valueOf(otp.isDisplayedNow()));
        qb.attachScreenshot("rental-home-rhl1-after-otp");

        assertThat(rentalCompanyPhone()).isEqualTo("9000000001");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(otp.isDisplayedNow()).as("Must leave OTP").isFalse();
        assertThat(landing.isExtendTimeDialogVisible())
                .as("Extend-time must not precede Close")
                .isFalse();
        assertThat(qb.isDisplayedNow())
                .as("Dump: Quick Booking title + Close app bar")
                .isTrue();
        assertThat(home.isDisplayedNow())
                .as("Pending queue must intercept before Home")
                .isFalse();
    }

    @Test(priority = 2, description = "RH-L2: Close once leaves Quick Booking without consuming the queue")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001. One Close tap (not rapid). Quick Booking app bar must go. "
            + "Do not tap Accept/Decline/View More Details on the extend-time dialog.")
    public void closeOnceLeavesQuickBooking() {
        QuickBookingPage qb = loginRental0001ToQueue();
        assertThat(qb.isDisplayedNow()).as("Precondition: Quick Booking").isTrue();
        qb.tapClose();

        HomePage home = new HomePage();
        QuickBookingLandingPage landing = new QuickBookingLandingPage();
        Waits.until(DriverManager.get(),
                d -> (!qb.isDisplayedNow() || landing.isExtendTimeDialogVisible()
                        || home.isDisplayedNow()) ? Boolean.TRUE : null,
                "After Close, Quick Booking did not leave",
                Duration.ofSeconds(12));

        Allure.parameter("phone", rentalCompanyPhone());
        Allure.parameter("package", String.valueOf(vendorPackage()));
        Allure.parameter("quickBookingAfterClose", String.valueOf(qb.isDisplayedNow()));
        Allure.parameter("extendTime", String.valueOf(landing.isExtendTimeDialogVisible()));
        Allure.parameter("homeA11y", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("bug", "15");
        qb.attachScreenshot("rental-home-rhl2-after-close");

        assertThat(rentalCompanyPhone()).isEqualTo("9000000001");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(qb.isDisplayedNow())
                .as("Close must drop Quick Booking title + Close")
                .isFalse();
        assertThat(landing.isExtendTimeDialogVisible())
                .as("Observed: Request to extend time after Close (BUGS_FOUND #15)")
                .isTrue();
    }

    @Test(priority = 3, description = "RH-L3: extend-time fully blocks Home a11y — dump inventory, no consume")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 after one Close. Dump 21 Sep 01-after-close: dismissable=false, "
            + "three clickables (View More Details, Decline, Accept), no Close/X/Not now. "
            + "Home chrome is not in the tree. BUGS_FOUND #15. Do not tap those three.")
    public void extendTimeFullyBlocksHome() {
        QuickBookingPage qb = loginRental0001ToQueue();
        qb.tapClose();
        QuickBookingLandingPage landing = new QuickBookingLandingPage();
        HomePage home = new HomePage();
        Waits.until(DriverManager.get(),
                d -> landing.isExtendTimeDialogVisible() ? Boolean.TRUE : null,
                "Extend-time dialog did not appear after Close",
                Duration.ofSeconds(12));

        int clickable = landing.clickableCount();
        boolean safeDismiss = landing.hasSafeDismissCopy();
        boolean checkable = landing.hasCheckableControl();
        boolean notDismissable = landing.isExtendTimeMarkedNotDismissable();
        Allure.parameter("phone", rentalCompanyPhone());
        Allure.parameter("package", String.valueOf(vendorPackage()));
        Allure.parameter("title", String.valueOf(landing.isExtendTimeDialogVisible()));
        Allure.parameter("viewMoreDetails", String.valueOf(landing.isExtendTimeViewMoreDetailsVisible()));
        Allure.parameter("declineOuter", String.valueOf(landing.isExtendTimeDeclineOuterVisible()));
        Allure.parameter("acceptOuter", String.valueOf(landing.isExtendTimeAcceptOuterVisible()));
        Allure.parameter("clickableCount", String.valueOf(clickable));
        Allure.parameter("safeDismiss", String.valueOf(safeDismiss));
        Allure.parameter("checkable", String.valueOf(checkable));
        Allure.parameter("dismissableFalse", String.valueOf(notDismissable));
        Allure.parameter("homeA11y", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("currentEarning", String.valueOf(home.isCurrentEarningVisible()));
        Allure.parameter("bug", "15");
        landing.attachScreenshot("rental-home-rhl3-extend-time");

        assertThat(rentalCompanyPhone()).isEqualTo("9000000001");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(qb.isDisplayedNow()).as("Quick Booking app bar must be gone").isFalse();
        assertThat(landing.isExtendTimeDialogVisible()).as("Dump title Request to extend time").isTrue();
        assertThat(landing.hasText("Initial End Date:")).isTrue();
        assertThat(landing.hasText("Extend till")).isTrue();
        assertThat(landing.hasText("Extended Amount:")).isTrue();
        assertThat(landing.isExtendTimeViewMoreDetailsVisible())
                .as("Dump clickable TextView View More Details — do not tap")
                .isTrue();
        assertThat(landing.isExtendTimeDeclineOuterVisible())
                .as("Dump clickable outer Decline — do not tap")
                .isTrue();
        assertThat(landing.isExtendTimeAcceptOuterVisible())
                .as("Dump clickable outer Accept — do not tap")
                .isTrue();
        assertThat(clickable)
                .as("Dump: only View More Details, Decline, Accept")
                .isEqualTo(3);
        assertThat(safeDismiss)
                .as("No Close/X, Not now, Remind me later, Close sheet")
                .isFalse();
        assertThat(checkable).as("Dump: no checkbox").isFalse();
        assertThat(notDismissable).as("Dump root dismissable=false").isTrue();
        assertThat(home.isDisplayedNow())
                .as("Home greeting/earning must not be in the a11y tree while dialog owns the window")
                .isFalse();
        assertThat(home.isCurrentEarningVisible()).isFalse();
    }

    private QuickBookingPage loginRental0001ToQueue() {
        assertThat(rentalCompanyPhone()).isEqualTo("9000000001");
        OtpPage otp = openOtp(rentalCompanyPhone());
        otp.focusOtpField();
        if (!otp.otpFieldText().isEmpty()) {
            otp.clearOtp();
        }
        otp.pressDigitKeys("1234");
        QuickBookingPage qb = new QuickBookingPage();
        try {
            Waits.until(DriverManager.get(),
                    d -> qb.isDisplayedNow() ? Boolean.TRUE : null,
                    "Quick Booking did not appear after OTP on 9000000001",
                    Duration.ofSeconds(15));
        } catch (TimeoutException first) {
            if (otp.isDisplayedNow() && otp.otpFieldText().isEmpty()) {
                otp.pasteOtp("1234");
            }
            Waits.until(DriverManager.get(),
                    d -> qb.isDisplayedNow() ? Boolean.TRUE : null,
                    "Quick Booking did not appear after OTP retry on 9000000001",
                    Duration.ofSeconds(15));
        }
        return qb;
    }
}
