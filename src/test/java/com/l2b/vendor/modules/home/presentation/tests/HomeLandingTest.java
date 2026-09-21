package com.l2b.vendor.modules.home.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Material vendor {@code 9000000017} Home landing. Dump-0 21 Sep 2026
 * ({@code /tmp/l2b-home-material-0017-20260921/00-landing}). Isolated
 * {@code home/home-landing.xml}. One case per run until review.
 * Do not tap Accept/Decline, extend time, tabs, or profile.
 * Do not log in as 0001 / 0002 / 0003.
 */
@Epic("Vendor app")
@Feature("Home landing — material vendor 9000000017")
public class HomeLandingTest extends HomeBaseTest {

    @Test(priority = 1, description = "H-L1: post-OTP lands on Home when the Quick Booking queue is empty")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017 + 1234 after empty QB queue. Must land on Home (Current Earning / "
            + "greeting), not Quick Booking, OTP, or Sign Up Completed. Dump-0 21 Sep. "
            + "Do not tap Accept/Decline, tabs, profile, or extend time.")
    public void postOtpLandsOnHome() {
        HomePage home = loginToHomeOrQueue();
        QuickBookingPage qb = new QuickBookingPage();
        OtpPage otp = new OtpPage();
        boolean signUpCompleted = !DriverManager.get()
                .findElements(ComposeLocators.textView("Sign Up Completed")).isEmpty();

        Allure.parameter("phone", materialPhone());
        Allure.parameter("package", String.valueOf(vendorPackage()));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("quickBooking", String.valueOf(qb.isDisplayedNow()));
        Allure.parameter("otp", String.valueOf(otp.isDisplayedNow()));
        Allure.parameter("signUpCompleted", String.valueOf(signUpCompleted));
        home.attachScreenshot("home-hl1-after-otp");

        assertThat(materialPhone()).isEqualTo("9000000017");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(otp.isDisplayedNow()).as("Must leave OTP").isFalse();
        assertThat(signUpCompleted).as("Existing user must not see Sign Up Completed").isFalse();
        assertThat(qb.isDisplayedNow())
                .as("Empty queue must not open Quick Booking (title + Close)")
                .isFalse();
        assertThat(home.isDisplayedNow())
                .as("Home: Current Earning or time-of-day greeting, no Close app bar")
                .isTrue();
    }

    @Test(priority = 2, description = "H-L2: Home chrome is material (Orders/Inventory), not rental Fleet/Calendar")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017 Dump-0. Bottom tabs must be Orders / Home / Earning / Inventory. "
            + "No Fleet or Calendar. Body uses Upcoming Orders, not rental booking chrome. "
            + "Do not tap tabs, profile, or extend time.")
    public void materialChromeNotRental() {
        HomePage home = loginToHomeOrQueue();
        QuickBookingPage qb = new QuickBookingPage();

        Allure.parameter("phone", materialPhone());
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("materialTabs", String.valueOf(home.isMaterialBottomTabsVisible()));
        Allure.parameter("fleetTab", String.valueOf(home.isFleetTabVisible()));
        Allure.parameter("calendarTab", String.valueOf(home.isCalendarTabVisible()));
        Allure.parameter("upcomingOrders", String.valueOf(home.isUpcomingOrdersVisible()));
        home.attachScreenshot("home-hl2-material-chrome");

        assertThat(materialPhone()).isEqualTo("9000000017");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(qb.isDisplayedNow()).as("Still Home, not Quick Booking").isFalse();
        assertThat(home.isDisplayedNow()).as("Home identity").isTrue();
        assertThat(home.isMaterialBottomTabsVisible())
                .as("Dump-0 tabs: Orders, Home, Earning, Inventory")
                .isTrue();
        assertThat(home.isUpcomingOrdersVisible())
                .as("Material copy Upcoming Orders")
                .isTrue();
        assertThat(home.isFleetTabVisible()).as("Must not show rental Fleet tab").isFalse();
        assertThat(home.isCalendarTabVisible()).as("Must not show rental Calendar tab").isFalse();
    }

    @Test(priority = 3, description = "H-L3: time-of-day greeting is visible on Home")
    @Severity(SeverityLevel.NORMAL)
    @Description("9000000017 Dump-0 header. Must show Good Morning! / Good Afternoon! / "
            + "Good Evening! (do not pin one). Record the live string. Do not tap.")
    public void greetingVisible() {
        HomePage home = loginToHomeOrQueue();
        String greeting = home.greetingNow();
        Allure.parameter("phone", materialPhone());
        Allure.parameter("greeting", greeting);
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        home.attachScreenshot("home-hl3-greeting");

        assertThat(materialPhone()).isEqualTo("9000000017");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow()).as("Home identity").isTrue();
        assertThat(home.isGreetingVisible())
                .as("Dump greeting Good Morning! / Afternoon! / Evening!")
                .isTrue();
        assertThat(greeting).isIn("Good Morning!", "Good Afternoon!", "Good Evening!");
    }

    @Test(priority = 4, description = "H-L4: Current Earning tile is visible on Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017 Dump-0. Current Earning label, rupee amount, and trend line must "
            + "show. Do not tap the tile, sparkline, or Earning tab.")
    public void currentEarningTileVisible() {
        HomePage home = loginToHomeOrQueue();
        String amount = home.currentEarningAmountNow();
        Allure.parameter("phone", materialPhone());
        Allure.parameter("currentEarning", String.valueOf(home.isCurrentEarningVisible()));
        Allure.parameter("amount", amount);
        Allure.parameter("trend", String.valueOf(home.isCurrentEarningTrendVisible()));
        home.attachScreenshot("home-hl4-earning-tile");

        assertThat(materialPhone()).isEqualTo("9000000017");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow()).as("Home identity").isTrue();
        assertThat(home.isCurrentEarningVisible()).as("Dump-0 label Current Earning").isTrue();
        assertThat(amount).as("Rupee amount on the tile").startsWith("₹");
        assertThat(home.isCurrentEarningTrendVisible())
                .as("Trend line with % and from")
                .isTrue();
    }

    @Test(priority = 5, description = "H-L5: bottom tabs present and Home content is selected")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-0 tabs Orders/Home/Earning/Inventory. Dump has no selected=true; Home is "
            + "selected when Current Earning stays on screen. Do not tap tabs.")
    public void bottomTabsPresentAndHomeSelected() {
        HomePage home = loginToHomeOrQueue();
        Allure.parameter("materialTabs", String.valueOf(home.isMaterialBottomTabsVisible()));
        Allure.parameter("homeTab", String.valueOf(home.isHomeTabVisible()));
        Allure.parameter("currentEarning", String.valueOf(home.isCurrentEarningVisible()));
        Allure.parameter("selectedAttrInDump", "false — XML has selected=true count 0");
        home.attachScreenshot("home-hl5-tabs");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isMaterialBottomTabsVisible()).isTrue();
        assertThat(home.isHomeTabVisible()).isTrue();
        assertThat(home.isCurrentEarningVisible())
                .as("Home tab selected means earning tile still on Home")
                .isTrue();
    }

    @Test(priority = 6, description = "H-L6: Profile affordance visible and account drawer is closed")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-0 Profile content-desc. Drawer must not auto-open (Account+Logout). Do not tap.")
    public void profileVisibleDrawerClosed() {
        HomePage home = loginToHomeOrQueue();
        boolean drawer = profileDrawerNow();
        Allure.parameter("profile", String.valueOf(home.isProfileVisible()));
        Allure.parameter("drawer", String.valueOf(drawer));
        home.attachScreenshot("home-hl6-profile");

        assertThat(home.isDisplayedNow()).isTrue();
        assertThat(home.isProfileVisible()).as("Dump-0 Profile").isTrue();
        assertThat(drawer).as("Account drawer must start closed").isFalse();
    }

    @Test(priority = 7, description = "H-L7: no Quick Booking overlay or extend-time dialog")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Home must not show Close app bar, Quick Booking title+Close, or Request to "
            + "extend time. Do not tap.")
    public void noQuickBookingOverlay() {
        HomePage home = loginToHomeOrQueue();
        QuickBookingPage qb = new QuickBookingPage();
        boolean extend = new com.l2b.vendor.modules.quickbooking.presentation.pages
                .QuickBookingLandingPage().isExtendTimeDialogVisible();
        Allure.parameter("qb", String.valueOf(qb.isDisplayedNow()));
        Allure.parameter("closeOverlay", String.valueOf(home.isQuickBookingOverlayVisible()));
        Allure.parameter("extendTime", String.valueOf(extend));
        home.attachScreenshot("home-hl7-no-qb");

        assertThat(home.isDisplayedNow()).isTrue();
        assertThat(qb.isDisplayedNow()).isFalse();
        assertThat(home.isQuickBookingOverlayVisible()).isFalse();
        assertThat(extend).as("Material Home must not show rental extend-time").isFalse();
    }

    @Test(priority = 8, description = "H-L8: swipe Home body — no crash, chrome stays")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump-0 vertical room is small; swipe up anyway. Home tabs and earning must "
            + "survive. Horizontal Upcoming strip is H-I8. Do not tap.")
    public void homeScrollKeepsChrome() {
        HomePage home = loginToHomeOrQueue();
        home.swipeHomeUp();
        Allure.parameter("homeAfterSwipe", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("tabsAfterSwipe", String.valueOf(home.isMaterialBottomTabsVisible()));
        Allure.parameter("package", String.valueOf(vendorPackage()));
        home.attachScreenshot("home-hl8-after-swipe");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow() || home.isMaterialBottomTabsVisible())
                .as("Swipe must not crash or leave Vendor Home chrome")
                .isTrue();
        assertThat(home.isMaterialBottomTabsVisible()).isTrue();
    }

    @Test(priority = 9, description = "H-L9: Dump-0 stat cards and empty Orders copy are visible")
    @Severity(SeverityLevel.NORMAL)
    @Description("Upcoming / Earning / Completed / Disputes Orders plus No orders right now. "
            + "Cards are clickable=false in Dump-0. Do not tap.")
    public void statCardsAndEmptyOrdersVisible() {
        HomePage home = loginToHomeOrQueue();
        Allure.parameter("upcoming", String.valueOf(home.isStatVisible("Upcoming Orders")));
        Allure.parameter("earningOrders", String.valueOf(home.isStatVisible("Earning Orders")));
        Allure.parameter("completed", String.valueOf(home.isStatVisible("Completed Orders")));
        Allure.parameter("disputes", String.valueOf(home.isStatVisible("Disputes Orders")));
        Allure.parameter("emptyOrders", String.valueOf(home.isEmptyOrdersCopyVisible()));
        Allure.parameter("confirmedCard", String.valueOf(home.isUpcomingConfirmedCardVisible()));
        Allure.parameter("seeAll", String.valueOf(home.isSeeAllVisible()));
        home.attachScreenshot("home-hl9-stats");

        assertThat(home.isStatVisible("Upcoming Orders")).isTrue();
        assertThat(home.isStatVisible("Earning Orders")).isTrue();
        assertThat(home.isStatVisible("Completed Orders")).isTrue();
        assertThat(home.isStatVisible("Disputes Orders")).isTrue();
        assertThat(home.isEmptyOrdersCopyVisible()).isTrue();
        assertThat(home.isUpcomingConfirmedCardVisible()).isTrue();
        assertThat(home.isSeeAllVisible()).isTrue();
    }
}
