package com.l2b.vendor.modules.home.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.bookings.presentation.tests.quickbooking.QuickBookingBaseTest;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;
import io.qameta.allure.Allure;
import java.time.Duration;
import org.openqa.selenium.TimeoutException;

/**
 * Shared 9000000001 path to usable Rental Home. Close only. Never Accept, Decline,
 * or Log Out. If extend-time owns the window, fail as BUGS_FOUND #15.
 */
public abstract class RentalHomeBaseTest extends QuickBookingBaseTest {

    protected HomePage reachUsableRentalHomeOrFailExtendTime() {
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

        boolean extend = landing.isExtendTimeDialogVisible();
        Allure.parameter("extendTime", String.valueOf(extend));
        Allure.parameter("bug", extend ? "15" : "none");
        if (extend) {
            landing.attachScreenshot("rental-home-extend-time-blocks-home");
        }
        assertThat(qb.isDisplayedNow())
                .as("Close must drop Quick Booking title + Close")
                .isFalse();
        assertThat(extend)
                .as("BUGS_FOUND #15: Request to extend time after Close — do not tap "
                        + "View More Details / Decline / Accept.")
                .isFalse();
        assertThat(home.isDisplayedNow())
                .as("Dump: greeting or Current Earning, no Close app bar")
                .isTrue();
        try {
            Waits.until(DriverManager.get(),
                    d -> (home.areRentalStatCardsVisible() || home.isBookingOrdersFeedVisible()
                            || home.isUpcomingStripCardVisible()) ? Boolean.TRUE : null,
                    "Rental Home stayed on skeleton placeholders",
                    Duration.ofSeconds(15));
        } catch (TimeoutException e) {
            Allure.parameter("skeletonStill", "true");
            home.attachScreenshot("rental-home-skeleton-timeout");
        }
        return home;
    }

    protected QuickBookingPage loginRental0001ToQueue() {
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

    protected String classifyRentalNow() {
        if (launcherNow()) {
            return "launcher";
        }
        if (new QuickBookingLandingPage().isExtendTimeDialogVisible()) {
            return "extend-time";
        }
        if (new QuickBookingPage().isDisplayedNow()) {
            return "quick-booking";
        }
        if (profileDrawerNow()) {
            return "home-drawer";
        }
        if (hasText("Schedule") && new HomePage().isCalendarTabVisible()
                && !new HomePage().isCurrentEarningVisible()) {
            return "calendar";
        }
        if (hasText("Earning & Incentive") || hasText("Wallet Balance")) {
            return "earning";
        }
        if (hasText("Your Fleet") || hasText("Add Machine")) {
            return "fleet";
        }
        if (hasText("Notification") && (hasText("Unread") || hasText("All"))) {
            return "notifications";
        }
        // Category list from Notification "See all" (e.g. Rentals + Mark all read)
        if (hasText("Mark all read") && (hasText("Rentals") || hasText("View Details"))) {
            return "notifications-category";
        }
        if (hasText("Profile Info") && (hasText("Full Name") || hasText("Mobile Number"))) {
            return "account";
        }
        if (hasText("Select machine for operator/driver")) {
            return "team-change-assignment";
        }
        if (hasText("Manage Team")
                && (hasText("Your team member") || hasText("Your team members")
                || hasText("Add Member") || hasText("Change Assignment"))) {
            return "team";
        }
        if ((hasText("Refer & Earn") || hasText("Refer now"))
                && (hasText("Your referral code") || hasText("Share your code")
                || hasText("Referrals are not available right now.")
                || hasText("Retry"))) {
            return "refer";
        }
        if ((hasText("FAQs") || hasText("FAQ"))
                && (hasText("How do I start a booked job?")
                || hasText("What happens if a customer cancels?")
                || hasText("How do I reach a human?")
                || hasText("How do I talk to a human?")
                || hasText("Why is my account still under review?")
                || hasText("Something went wrong. Please try again.")
                || (hasText("Retry") && hasText("FAQs")))) {
            return "faq";
        }
        if (hasText("Accepting these terms")
                || (hasText("Your obligations") && hasText("Payments and deductions"))
                || ((hasText("Terms & Services") || hasText("Terms and Services")
                || hasText("नियम एवं सेवाएँ"))
                && (hasText("Something went wrong. Please try again.")
                || (hasText("Retry") && hasText("Terms & Services"))))) {
            return "terms";
        }
        if ((hasText("Policies") || hasText("नीतियाँ"))
                && (hasText("What we collect")
                || hasText("How we use it")
                || hasText("Your rights")
                || hasText("Location")
                || hasText("हम कौन-सी जानकारी")
                || hasText("Something went wrong. Please try again.")
                || (hasText("Retry") && hasText("Policies")))) {
            return "policies";
        }
        if ((hasText("Settings") || hasText("सेटिंग्स"))
                && (hasText("Permissions")
                || hasText("An unexpected error occurred")
                || (hasText("Retry") && hasText("Settings")))
                && (hasText("Notification") || hasText("GPS Location")
                || hasText("Camera Access")
                || hasText("An unexpected error occurred")
                || hasText("Retry"))) {
            return "app-settings";
        }
        if (hasText("Help & Support")
                || (hasText("Live chat") && hasText("Raise Ticket") && hasText("Ticket history"))
                || (hasText("Submit a query") && (hasText("Chat now") || hasText("Submit")))
                || (hasText("Ticket history") && hasText("Recent tickets"))) {
            return "help";
        }
        if ((hasText("Choose the language") || hasText("भाषा चुनें") || hasText("All language")
                || hasText("सभी भाषाएं"))
                && (hasText("English") || hasText("हिंदी"))
                && (hasText("Save") || hasText("सेव करें"))) {
            return "language-settings";
        }
        if (new HomePage().isDisplayedNow()) {
            return "home";
        }
        return namedLanding(new QuickBookingPage(), new HomePage());
    }

    protected boolean hasText(String text) {
        return !DriverManager.get().findElements(ComposeLocators.textView(text)).isEmpty();
    }
}
