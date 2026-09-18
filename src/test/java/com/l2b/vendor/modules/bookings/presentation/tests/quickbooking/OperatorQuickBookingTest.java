package com.l2b.vendor.modules.bookings.presentation.tests.quickbooking;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.time.Duration;
import org.testng.annotations.Test;

/**
 * Post-OTP landing for operator {@code 9000000002} (OTP 1234). Operator existing-user
 * landing is not specified yet — case 1 records the real screen. Isolated
 * {@code src/test/resources/quick-booking/operator.xml}. Do not run until rental
 * company / individual / material files are done.
 */
@Epic("Vendor app")
@Feature("Quick Booking — operator 9000000002")
public class OperatorQuickBookingTest extends QuickBookingBaseTest {

    @Test(priority = 1, description = "0002-1: After OTP, record named landing (do not assume Quick Booking)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000002 + 1234. Record whether operator lands on Quick Booking, Home, or "
            + "another vendor screen. Does not tap Accept/Decline.")
    public void afterOtpNamedLanding() {
        submitStaticOtp(operatorPhone());

        QuickBookingPage page = new QuickBookingPage();
        HomePage home = new HomePage();
        OtpPage otp = new OtpPage();
        Waits.until(DriverManager.get(),
                d -> (!otp.isDisplayedNow()) ? Boolean.TRUE : null,
                "Operator OTP 1234 stayed on OTP",
                Duration.ofSeconds(20));

        String landing = namedLanding(page, home);
        Allure.parameter("phone", operatorPhone());
        Allure.parameter("landing", landing);
        Allure.parameter("package", vendorPackage());
        Allure.parameter("quickBooking", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        page.attachScreenshot("0002-1-landing-" + landing);

        assertThat(vendorPackage()).as("Must stay in Vendor after operator OTP").isEqualTo("com.l2b.app.qa");
        assertThat(otp.isDisplayedNow()).as("OTP must be gone after 1234").isFalse();
        assertThat(landing)
                .as("Named landing after operator OTP (quick-booking / home / vendor-other)")
                .isIn("quick-booking", "home", "vendor-other");
    }
}
