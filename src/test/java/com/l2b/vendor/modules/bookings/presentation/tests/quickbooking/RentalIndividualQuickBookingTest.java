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
 * Quick Booking edges for rental individual {@code 9000000003} (OTP 1234).
 * Proven 18 Sep: 0 bookings → Home, no Quick Booking. Isolated
 * {@code src/test/resources/quick-booking/rental-individual.xml}.
 */
@Epic("Vendor app")
@Feature("Quick Booking — rental individual 9000000003")
public class RentalIndividualQuickBookingTest extends QuickBookingBaseTest {

    @Test(priority = 1, description = "0003-1: Zero bookings go straight to Home, no Quick Booking")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000003 + 1234. Zero bookings must land on Home. Quick Booking must not appear.")
    public void zeroBookingsLandsOnHome() {
        submitStaticOtp(rentalIndividualPhone());

        HomePage home = new HomePage();
        QuickBookingPage page = new QuickBookingPage();
        Waits.until(DriverManager.get(),
                d -> (home.isDisplayedNow() || page.isDisplayedNow()) ? Boolean.TRUE : null,
                "After OTP, no Home or Quick Booking",
                Duration.ofSeconds(20));

        Allure.parameter("phone", rentalIndividualPhone());
        Allure.parameter("quickBookingVisible", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("homeVisible", String.valueOf(home.isDisplayedNow()));
        home.attachScreenshot("0003-1-zero-bookings-home");

        assertThat(vendorPackage()).isEqualTo("com.l2b.app.qa");
        assertThat(page.isDisplayedNow()).as("Zero-booking account must not show Quick Booking").isFalse();
        assertThat(home.isDisplayedNow()).as("Zero bookings must land on Home").isTrue();
        assertThat(home.isCurrentEarningVisible()).as("Home Current Earning").isTrue();
        assertThat(page.isAcceptVisible()).as("No Accept queue").isFalse();
    }

    @Test(priority = 2, description = "0003-2: Session persists on Home after relaunch")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000003 to Home, terminateApp + activateApp without pm clear. Must return to "
            + "Home, not Language / Quick Booking.")
    public void sessionPersistsOnHome() {
        submitStaticOtp(rentalIndividualPhone());
        HomePage home = new HomePage();
        QuickBookingPage page = new QuickBookingPage();
        Waits.until(DriverManager.get(),
                d -> home.isDisplayedNow() ? Boolean.TRUE : null,
                "Precondition: 0003 did not reach Home",
                Duration.ofSeconds(20));

        AndroidDriver android = (AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.terminateApp(pkg);
        android.activateApp(pkg);

        waitForLoggedInLanding(page, home, android, pkg);
        String landing = namedLanding(page, home);
        Allure.parameter("relaunchLanding", landing);
        home.attachScreenshot("0003-2-relaunch-" + landing);

        assertThat(android.getCurrentPackage()).isEqualTo(pkg);
        assertThat(landing).as("Zero-booking session must return to Home").isEqualTo("home");
        assertThat(page.isDisplayedNow()).as("Relaunch must not invent Quick Booking").isFalse();
        assertThat(home.isDisplayedNow()).as("Home after relaunch").isTrue();
    }
}
