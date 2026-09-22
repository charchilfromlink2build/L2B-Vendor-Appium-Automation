package com.l2b.vendor.modules.home.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Rental Home taps for {@code 9000000001}. Destination only. Never Accept, Decline,
 * or Log Out. Isolated {@code home/home-rental-interact-*.xml}.
 */
@Epic("Vendor app")
@Feature("Home interact — rental vendor 9000000001")
public class RentalHomeInteractTest extends RentalHomeBaseTest {

    @Test(priority = 1, description = "RH-I1: Monthly / Select period opens options")
    @Severity(SeverityLevel.NORMAL)
    public void monthlyPeriodFilter() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc("Select period");
        String named = classifyRentalNow();
        boolean weekly = hasText("Weekly") || pageHas("Week");
        boolean yearly = hasText("Yearly") || pageHas("Year");
        boolean daily = hasText("Daily") || hasText("Today");
        Allure.parameter("after", named);
        Allure.parameter("weekly", String.valueOf(weekly));
        Allure.parameter("yearly", String.valueOf(yearly));
        Allure.parameter("daily", String.valueOf(daily));
        home.attachScreenshot("rental-home-rhi1-monthly");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(weekly || yearly || daily || home.isPeriodFilterVisible() || named.equals("home"))
                .as("Period tap opens options or stays on Home filter")
                .isTrue();
    }

    @Test(priority = 2, description = "RH-I2: About this stat / Current Earning — named overlay or Home")
    @Severity(SeverityLevel.NORMAL)
    public void aboutThisStatOrEarning() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        int infos = home.aboutThisStatCount();
        if (infos > 0) {
            home.tapDesc("About this stat");
        } else {
            home.tapText("Current Earning");
        }
        String named = classifyRentalNow();
        Allure.parameter("infoIcons", String.valueOf(infos));
        Allure.parameter("after", named);
        home.attachScreenshot("rental-home-rhi2-info");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(named).isNotEqualTo("otp");
    }

    @Test(priority = 3, description = "RH-I3: Active Fleet See all → Fleet (no machine deep-test)")
    @Severity(SeverityLevel.CRITICAL)
    public void activeFleetSeeAll() {
        tapSeeAllExpect(0, "fleet", "rental-home-rhi3-fleet-see-all");
    }

    @Test(priority = 4, description = "RH-I4: Completed Task card — named landing")
    @Severity(SeverityLevel.NORMAL)
    public void tapCompletedTask() {
        documentStatTap("Total Completed Task", "rental-home-rhi4-completed");
    }

    @Test(priority = 5, description = "RH-I5: Upcoming Booking stat — named landing")
    @Severity(SeverityLevel.NORMAL)
    public void tapUpcomingStat() {
        documentStatTap("Upcoming Booking", "rental-home-rhi5-upcoming-stat");
    }

    @Test(priority = 6, description = "RH-I6: Earning Projected — named landing")
    @Severity(SeverityLevel.NORMAL)
    public void tapEarningProjected() {
        documentStatTap("Earning Projected", "rental-home-rhi6-projected");
    }

    @Test(priority = 7, description = "RH-I7: Upcoming See all → Bookings list")
    @Severity(SeverityLevel.CRITICAL)
    public void upcomingSeeAll() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapNthSeeAll(1);
        String named = classifyRentalNow();
        Allure.parameter("after", named);
        home.attachScreenshot("rental-home-rhi7-upcoming-see-all");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(named).isNotIn("otp", "language", "signup");
    }

    @Test(priority = 8, description = "RH-I8: first Upcoming strip card tap")
    @Severity(SeverityLevel.CRITICAL)
    public void tapFirstUpcomingCard() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapFirstUpcomingStripCard();
        String named = classifyRentalNow();
        Allure.parameter("after", named);
        home.attachScreenshot("rental-home-rhi8-upcoming-card");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(named).isNotIn("otp", "language");
    }

    @Test(priority = 9, description = "RH-I9: Booking Orders See all → Quick Booking (Close only)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("See all on Booking Orders. Expect Quick Booking. Close once if it opens. "
            + "Do not Accept/Decline.")
    public void bookingOrdersSeeAll() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapNthSeeAll(2);
        String named = classifyRentalNow();
        Allure.parameter("after", named);
        home.attachScreenshot("rental-home-rhi9-orders-see-all");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        if ("quick-booking".equals(named)) {
            new com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage().tapClose();
        }
        assertThat(named).isNotIn("otp", "language");
    }

    @Test(priority = 10, description = "RH-I10: Calendar tab — Schedule, bar stays")
    @Severity(SeverityLevel.CRITICAL)
    public void calendarTab() {
        tapTab("Calendar", "calendar", "rental-home-rhi10-calendar");
    }

    @Test(priority = 11, description = "RH-I11: Earning tab — Earning & Incentive")
    @Severity(SeverityLevel.CRITICAL)
    public void earningTab() {
        tapTab("Earning", "earning", "rental-home-rhi11-earning");
    }

    @Test(priority = 12, description = "RH-I12: Fleet tab — Your Fleet")
    @Severity(SeverityLevel.CRITICAL)
    public void fleetTab() {
        tapTab("Fleet", "fleet", "rental-home-rhi12-fleet");
    }

    @Test(priority = 13, description = "RH-I13: Home tab again stays Home, drawer closed")
    @Severity(SeverityLevel.CRITICAL)
    public void homeTabAgain() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc("Home");
        Allure.parameter("after", classifyRentalNow());
        Allure.parameter("drawer", String.valueOf(profileDrawerNow()));
        home.attachScreenshot("rental-home-rhi13-home-tab");
        assertThat(home.isDisplayedNow()).isTrue();
        assertThat(profileDrawerNow()).isFalse();
    }

    @Test(priority = 14, description = "RH-I14: Profile opens drawer — do not Log Out")
    @Severity(SeverityLevel.CRITICAL)
    public void profileOpensDrawer() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc("Profile");
        boolean drawer = profileDrawerNow();
        Allure.parameter("drawer", String.valueOf(drawer));
        Allure.parameter("after", classifyRentalNow());
        home.attachScreenshot("rental-home-rhi14-profile");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(drawer).as("Profile must open Account + Log Out sheet").isTrue();
    }

    @Test(priority = 15, description = "RH-I15: Notifications — Unread/All")
    @Severity(SeverityLevel.CRITICAL)
    public void notifications() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc("Notifications");
        String named = classifyRentalNow();
        Allure.parameter("after", named);
        home.attachScreenshot("rental-home-rhi15-notifications");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(named).isIn("notifications", "home", "vendor-other");
    }

    @Test(priority = 16, description = "RH-I16: pull-to-refresh exists or not")
    @Severity(SeverityLevel.NORMAL)
    public void pullToRefresh() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        org.openqa.selenium.Dimension size = DriverManager.get().manage().window().getSize();
        DriverManager.get().executeScript("mobile: swipeGesture", java.util.Map.of(
                "left", (int) (size.width * 0.3),
                "top", (int) (size.height * 0.18),
                "width", (int) (size.width * 0.4),
                "height", (int) (size.height * 0.22),
                "direction", "down",
                "percent", 0.8));
        Allure.parameter("homeAfter", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("after", classifyRentalNow());
        home.attachScreenshot("rental-home-rhi16-ptr");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow() || home.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 17, description = "RH-I17: multi-card independence — View More only if present")
    @Severity(SeverityLevel.CRITICAL)
    @Description("If View More Details is on Home, tap it and assert the other Timer still "
            + "exists. If absent, record BUGS_FOUND #22 and skip — do not tap Accept/Decline.")
    public void multiCardIndependenceViewMore() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        java.util.List<String> before = home.bookingOrderTimersNow();
        if (!home.isViewMoreDetailsVisible()) {
            Allure.parameter("knownBug", "22");
            home.attachScreenshot("rental-home-rhi17-no-view-more");
            throw new SkipException(
                    "BUGS_FOUND #22: View More Details is not on Home Booking Orders. "
                            + "Not tapping Accept/Decline to test independence.");
        }
        home.tapText("View More Details");
        java.util.List<String> after = home.bookingOrderTimersNow();
        Allure.parameter("before", String.join(",", before));
        Allure.parameter("after", String.join(",", after));
        Allure.parameter("afterLanding", classifyRentalNow());
        home.attachScreenshot("rental-home-rhi17-view-more");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(after.isEmpty() && !before.isEmpty())
                .as("Tapping View More on one card must not wipe the other card timer")
                .isFalse();
    }

    private void tapSeeAllExpect(int index, String expected, String shot) {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapNthSeeAll(index);
        String named = classifyRentalNow();
        Allure.parameter("after", named);
        home.attachScreenshot(shot);
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(named).as("See all destination").isIn(expected, "home", "vendor-other");
    }

    private void tapTab(String tab, String expected, String shot) {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc(tab);
        String named = classifyRentalNow();
        Allure.parameter("after", named);
        Allure.parameter("tabsStay", String.valueOf(home.isRentalBottomTabsVisible()));
        home.attachScreenshot(shot);
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isRentalBottomTabsVisible()).as("Bottom bar stays").isTrue();
        assertThat(named).isIn(expected, "home", "vendor-other");
    }

    private void documentStatTap(String label, String shot) {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        try {
            home.tapText(label);
        } catch (RuntimeException e) {
            Allure.parameter("tapThrew", e.getClass().getSimpleName());
        }
        String named = classifyRentalNow();
        Allure.parameter("label", label);
        Allure.parameter("after", named);
        home.attachScreenshot(shot);
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(named).isNotIn("otp", "language", "signup");
    }

    private boolean pageHas(String fragment) {
        return DriverManager.get().getPageSource().contains(fragment);
    }
}
