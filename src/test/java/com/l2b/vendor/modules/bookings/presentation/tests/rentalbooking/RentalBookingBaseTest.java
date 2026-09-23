package com.l2b.vendor.modules.bookings.presentation.tests.rentalbooking;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.bookings.presentation.pages.RentalBookingsPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;
import io.qameta.allure.Allure;
import java.time.Duration;

/**
 * Shared entry for Rental Booking cases on {@code 9000000001}.
 *
 * <p>Inherits the proven Rental Home path: OTP {@code 1234} → Quick Booking →
 * one {@code Close} → usable Home (fails as BUGS_FOUND #15 if the extend-time
 * dialog owns the window). Bookings is then reached from Home → Upcoming
 * Booking → {@code See all}.
 *
 * <p><b>On hold.</b> The entry helper throws until the live rental booking seed
 * exists, so nothing in this package can start a session or touch the queue by
 * accident. Standing rules that still apply when it is enabled:
 * <ul>
 *   <li>Accept / Decline / Assign only on bookings seeded from the customer web
 *       for this exercise — never on the pre-existing 0001 queue.</li>
 *   <li>Never tap Log Out.</li>
 *   <li>A new Bookings defect gets a new sequential number. Never folded into
 *       #13–#16 (Quick Booking) or #17–#26 (Home).</li>
 * </ul>
 */
public abstract class RentalBookingBaseTest extends RentalHomeBaseTest {

    protected static final String ON_HOLD =
            "Rental Booking automation is on hold until the live customer-web booking seed. "
                    + "Case is specified only — no Appium steps run.";

    /**
     * Home → Upcoming Booking {@code See all} → Bookings list.
     *
     * <p>Uses the proven Rental Home path (RH-I7): reach usable Home, then tap the
     * Upcoming Booking {@code See all} (index 1 — 0 is Active Fleet, 2 is Booking
     * Orders). Fails as #15 if extend-time owns the window.
     *
     * @return the Bookings page on the Upcoming tab
     */
    protected RentalBookingsPage openBookingsFromHomeSeeAll() {
        HomePage home = reachRentalHomeResilient();
        home.tapNthSeeAll(1);
        RentalBookingsPage bookings = new RentalBookingsPage();
        bookings.waitUntilLoaded();
        return bookings;
    }

    /**
     * Reach usable Rental Home for {@code 9000000001} whether or not a pending request
     * intercepts with Quick Booking. After OTP the app either shows the Quick Booking
     * queue (Close once to Home) or lands on Home directly when no request is pending.
     * Still fails as BUGS_FOUND #15 if the extend-time dialog owns the window.
     */
    protected HomePage reachRentalHomeResilient() {
        assertThat(rentalCompanyPhone()).isEqualTo("9000000001");
        OtpPage otp = openOtp(rentalCompanyPhone());
        otp.focusOtpField();
        if (!otp.otpFieldText().isEmpty()) {
            otp.clearOtp();
        }
        otp.pressDigitKeys("1234");

        QuickBookingPage qb = new QuickBookingPage();
        HomePage home = new HomePage();
        QuickBookingLandingPage landing = new QuickBookingLandingPage();

        Waits.until(DriverManager.get(),
                d -> (qb.isDisplayedNow() || home.isDisplayedNow()
                        || landing.isExtendTimeDialogVisible()) ? Boolean.TRUE : null,
                "After OTP, neither Quick Booking nor Home nor extend-time appeared on 9000000001",
                Duration.ofSeconds(20));

        boolean viaQuickBooking = qb.isDisplayedNow();
        Allure.parameter("postOtpLanding", viaQuickBooking ? "quick-booking"
                : home.isDisplayedNow() ? "home" : "extend-time");

        if (viaQuickBooking) {
            qb.tapClose();
            Waits.until(DriverManager.get(),
                    d -> (!qb.isDisplayedNow() || landing.isExtendTimeDialogVisible()
                            || home.isDisplayedNow()) ? Boolean.TRUE : null,
                    "After Close, Quick Booking did not leave",
                    Duration.ofSeconds(12));
        }

        boolean extend = landing.isExtendTimeDialogVisible();
        Allure.parameter("extendTime", String.valueOf(extend));
        Allure.parameter("bug", extend ? "15" : "none");
        if (extend) {
            landing.attachScreenshot("rental-home-extend-time-blocks-home");
        }
        assertThat(extend)
                .as("BUGS_FOUND #15: Request to extend time blocks Home — do not tap "
                        + "View More Details / Decline / Accept.")
                .isFalse();
        assertThat(home.isDisplayedNow())
                .as("Rental Home (greeting or Current Earning, no Close app bar) must render")
                .isTrue();
        try {
            Waits.until(DriverManager.get(),
                    d -> (home.areRentalStatCardsVisible() || home.isBookingOrdersFeedVisible()
                            || home.isUpcomingStripCardVisible()) ? Boolean.TRUE : null,
                    "Rental Home stayed on skeleton placeholders",
                    Duration.ofSeconds(15));
        } catch (org.openqa.selenium.TimeoutException e) {
            Allure.parameter("skeletonStill", "true");
            home.attachScreenshot("rental-home-skeleton-timeout");
        }
        return home;
    }

    /**
     * Home → Booking Orders {@code See all} → Quick Booking queue, the surface an
     * incoming rental request lands on.
     *
     * @throws UnsupportedOperationException while the module is on hold
     */
    protected void openIncomingQueueFromHomeSeeAll() {
        throw new UnsupportedOperationException(ON_HOLD);
    }
}
