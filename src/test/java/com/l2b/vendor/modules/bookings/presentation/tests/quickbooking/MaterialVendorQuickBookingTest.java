package com.l2b.vendor.modules.bookings.presentation.tests.quickbooking;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.time.Duration;
import org.testng.annotations.Test;

/**
 * Quick Booking edges for material vendor {@code 9000000017} (OTP 1234).
 * Proven 18 Sep: 2 {@code pending_vendor_acceptance} orders open Quick Booking.
 * Never tap Accept/Decline. Isolated {@code src/test/resources/quick-booking/material.xml}.
 */
@Epic("Vendor app")
@Feature("Quick Booking — material vendor 9000000017")
public class MaterialVendorQuickBookingTest extends QuickBookingBaseTest {

    @Test(priority = 1, description = "0017-1: pending_vendor_acceptance lands on Quick Booking")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017 + 1234. Material pending orders must open Quick Booking with "
            + "Order Placed / Pickup scheduled / Accept / Decline. Do not tap Accept/Decline.")
    public void pendingLandsOnQuickBooking() {
        QuickBookingPage page = loginToQuickBooking(materialPhone());
        HomePage home = new HomePage();

        Allure.parameter("phone", materialPhone());
        Allure.parameter("acceptCount", String.valueOf(page.acceptCount()));
        Allure.parameter("hasPickup11Sep", String.valueOf(page.hasText("11 Sep 2026")));
        Allure.parameter("hasPickup10Sep", String.valueOf(page.hasText("10 Sep 2026")));
        page.attachScreenshot("0017-1-pending-quick-booking");

        assertThat(vendorPackage()).isEqualTo("com.l2b.app.qa");
        assertThat(page.isDisplayedNow()).as("Quick Booking title + Close").isTrue();
        assertThat(home.isDisplayedNow()).as("Must not be Home").isFalse();
        assertThat(page.isOrderPlacedVisible()).as("Order Placed").isTrue();
        assertThat(page.isPickupScheduledVisible()).as("Pickup scheduled").isTrue();
        assertThat(page.isFastDeliveryVisible()).as("Fast Delivery").isTrue();
        assertThat(page.isBookingForVisible()).as("Booking for").isTrue();
        assertThat(page.isAcceptVisible()).as("Accept").isTrue();
        assertThat(page.isDeclineVisible()).as("Decline").isTrue();
        assertThat(page.isViewMoreDetailsVisible()).as("View More Details").isTrue();
        assertThat(page.acceptCount()).as("Two pending material orders").isGreaterThanOrEqualTo(2);
        assertThat(page.hasText("11 Sep 2026")).as("MAT-20260909-2556 pickup").isTrue();
        assertThat(page.hasText("10 Sep 2026")).as("MAT-20260908-2000 pickup").isTrue();
        assertThat(page.isRentalAmountChromeVisible()).as("Must not show rental Amount · Online Mode").isFalse();
        assertThat(page.isTimerVisible()).as("Material cards have no rental Timer").isFalse();
    }

    @Test(priority = 2, description = "0017-2: Close goes to Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017 on Quick Booking. Tap Close. Must reach Home. Do not tap Accept/Decline.")
    public void closeGoesToHome() {
        QuickBookingPage page = loginToQuickBooking(materialPhone());
        assertThat(page.isDisplayedNow()).as("Precondition: Quick Booking").isTrue();
        page.tapClose();

        HomePage home = new HomePage();
        home.waitUntilLoaded();
        Allure.parameter("quickBookingAfterClose", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("homeAfterClose", String.valueOf(home.isDisplayedNow()));
        page.attachScreenshot("0017-2-close-home");

        assertThat(vendorPackage()).isEqualTo("com.l2b.app.qa");
        assertThat(page.isDisplayedNow()).as("Quick Booking must be gone after Close").isFalse();
        assertThat(home.isDisplayedNow()).as("Close must land on Home").isTrue();
    }

    /**
     * Same Quick Booking page as rental; observed 18 Sep: launcher. BUGS_FOUND #13.
     */
    @Test(priority = 3, description = "0017-3: Back — named landing, no crash")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017 on Quick Booking. Device Back. Observed: launcher (BUGS_FOUND #13).")
    public void backNamedLanding() {
        QuickBookingPage page = loginToQuickBooking(materialPhone());
        assertThat(page.isDisplayedNow()).as("Precondition: Quick Booking").isTrue();
        page.pressBack();

        HomePage home = new HomePage();
        Waits.until(DriverManager.get(),
                d -> (home.isDisplayedNow() || page.isDisplayedNow() || launcherNow()) ? Boolean.TRUE : null,
                "After Back, no Home / Quick Booking / launcher",
                Duration.ofSeconds(12));

        String landing = namedLanding(page, home);
        String pkg = vendorPackage();
        Allure.parameter("backLanding", landing);
        Allure.parameter("backPackage", String.valueOf(pkg));
        page.attachScreenshot("0017-3-back-" + landing);

        assertThat(pkg).as("Back from Quick Booking must not crash (package blank)").isNotBlank();
        assertThat(landing)
                .as("Observed 18 Sep: launcher. Logged as BUGS_FOUND #13")
                .isEqualTo("launcher");
    }

    @Test(priority = 4, description = "0017-4: Session persists on Quick Booking after relaunch")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017 login to Quick Booking, terminateApp + activateApp without pm clear. "
            + "Must return to Quick Booking, not Language.")
    public void sessionPersistsOnQuickBooking() {
        QuickBookingPage page = loginToQuickBooking(materialPhone());
        assertThat(page.isDisplayedNow()).as("Precondition: Quick Booking").isTrue();

        AndroidDriver android = (AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.terminateApp(pkg);
        android.activateApp(pkg);

        HomePage home = new HomePage();
        waitForLoggedInLanding(page, home, android, pkg);
        String landing = namedLanding(page, home);
        Allure.parameter("relaunchLanding", landing);
        page.attachScreenshot("0017-4-relaunch-" + landing);

        assertThat(android.getCurrentPackage()).isEqualTo(pkg);
        assertThat(landing).as("Logged-in pending material must reopen Quick Booking").isEqualTo("quick-booking");
        assertThat(page.isDisplayedNow()).as("Quick Booking after relaunch").isTrue();
        assertThat(page.isOrderPlacedVisible()).as("Material chrome after relaunch").isTrue();
        assertThat(home.isDisplayedNow()).as("Must not skip queue to Home").isFalse();
    }
}
