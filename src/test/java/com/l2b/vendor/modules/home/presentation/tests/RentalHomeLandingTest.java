package com.l2b.vendor.modules.home.presentation.tests;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import org.openqa.selenium.TimeoutException;
import org.testng.annotations.Test;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;

import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;

/**
 * Rental Home landing for {@code 9000000001}. Overlay inventory is RH-L1–L3
 * ({@code home/home-rental-landing.xml}). First product Home case is RH-L4
 * ({@code home/home-rental-landing-l4.xml}). Dump 21 Sep
 * {@code /tmp/l2b-rental-flow-0001-20260921/01-after-close}.
 * Do not tap Accept, Decline, View More Details, or Log Out. BUGS_FOUND #15
 * — do not open a new bug number for the same dialog.
 */
@Epic("Vendor app")
@Feature("Home landing — rental vendor 9000000001")
public class RentalHomeLandingTest extends RentalHomeBaseTest {

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

    @Test(priority = 4, description = "RH-L4: Close once reaches usable Rental Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 after one Close. Dump 21 Sep 01-after-close: Good Afternoon! / "
            + "Current Earning, Calendar · Home · Earning · Fleet. Not Quick Booking Close bar. "
            + "Not material Orders / Inventory. Do not tap Accept/Decline on Booking Orders, "
            + "View More Details, or Log Out. If Request to extend time owns the window, fail "
            + "as BUGS_FOUND #15 — do not consume the dialog.")
    public void closeOnceReachesUsableRentalHome() {
        System.out.println("WORKING_DIRECTORY=" + System.getProperty("user.dir"));
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
        Allure.parameter("workingDirectory", System.getProperty("user.dir"));
        Allure.parameter("phone", rentalCompanyPhone());
        Allure.parameter("package", String.valueOf(vendorPackage()));
        Allure.parameter("quickBookingAfterClose", String.valueOf(qb.isDisplayedNow()));
        Allure.parameter("extendTime", String.valueOf(extend));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("greeting", home.greetingNow());
        Allure.parameter("currentEarning", String.valueOf(home.isCurrentEarningVisible()));
        Allure.parameter("rentalTabs", String.valueOf(home.isRentalBottomTabsVisible()));
        Allure.parameter("ordersTab", String.valueOf(home.isOrdersTabVisible()));
        Allure.parameter("inventoryTab", String.valueOf(home.isInventoryTabVisible()));
        Allure.parameter("bug", extend ? "15" : "none");
        home.attachScreenshot("rental-home-rhl4-after-close");

        assertThat(rentalCompanyPhone()).isEqualTo("9000000001");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(qb.isDisplayedNow())
                .as("Close must drop Quick Booking title + Close")
                .isFalse();
        assertThat(extend)
                .as("BUGS_FOUND #15: Request to extend time after Close — Home chrome not in "
                        + "the tree. Do not tap View More Details / Decline / Accept. Usable "
                        + "Rental Home not reached on 9000000001.")
                .isFalse();
        assertThat(home.isDisplayedNow())
                .as("Dump: greeting or Current Earning, no Close app bar")
                .isTrue();
        assertThat(home.isGreetingVisible() || home.isCurrentEarningVisible())
                .as("Dump 01-after-close: Good Morning!/Afternoon!/Evening! or Current Earning")
                .isTrue();
        assertThat(home.isRentalBottomTabsVisible())
                .as("Dump tabs: Calendar, Home, Earning, Fleet")
                .isTrue();
        assertThat(home.isOrdersTabVisible()).as("Must not show material Orders tab").isFalse();
        assertThat(home.isInventoryTabVisible())
                .as("Must not show material Inventory tab")
                .isFalse();
    }

    @Test(priority = 5, description = "RH-L8: four Rental Home stat cards are visible")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 after one Close. Dump 21 Sep 01-after-close 2×2: Active Fleet, "
            + "Total Completed Task, Upcoming Booking, Earning Projected. Assert labels only "
            + "— do not pin live counts or rupees. Do not tap See all, About this stat, "
            + "Accept, Decline, View More Details, or Log Out. If extend-time owns the "
            + "window, fail as BUGS_FOUND #15.")
    public void rentalStatCardsVisible() {
        System.out.println("WORKING_DIRECTORY=" + System.getProperty("user.dir"));
        HomePage home = reachUsableRentalHomeOrFailExtendTime();

        Allure.parameter("workingDirectory", System.getProperty("user.dir"));
        Allure.parameter("phone", rentalCompanyPhone());
        Allure.parameter("package", String.valueOf(vendorPackage()));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("activeFleet", String.valueOf(home.isStatVisible("Active Fleet")));
        Allure.parameter("completedTask", String.valueOf(home.isStatVisible("Total Completed Task")));
        Allure.parameter("upcomingBooking", String.valueOf(home.isStatVisible("Upcoming Booking")));
        Allure.parameter("earningProjected", String.valueOf(home.isStatVisible("Earning Projected")));
        Allure.parameter("statCards", String.valueOf(home.areRentalStatCardsVisible()));
        home.attachScreenshot("rental-home-rhl8-stat-cards");

        assertThat(rentalCompanyPhone()).isEqualTo("9000000001");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow())
                .as("Must stay on usable Rental Home")
                .isTrue();
        assertThat(home.isStatVisible("Active Fleet"))
                .as("Dump 2×2: Active Fleet")
                .isTrue();
        assertThat(home.isStatVisible("Total Completed Task"))
                .as("Dump 2×2: Total Completed Task")
                .isTrue();
        assertThat(home.isStatVisible("Upcoming Booking"))
                .as("Dump 2×2: Upcoming Booking")
                .isTrue();
        assertThat(home.isStatVisible("Earning Projected"))
                .as("Dump 2×2: Earning Projected")
                .isTrue();
        assertThat(home.areRentalStatCardsVisible()).isTrue();
    }

    @Test(priority = 6, description = "RH-L12: Upcoming Booking strip card is visible")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 after one Close. Dump 21 Sep 01-after-close: Upcoming Booking "
            + "section plus a clickable strip card (machine, plate, Booking for). Second card "
            + "is clipped — do not swipe (RH-L16) and do not tap the card (RH-I8). Do not tap "
            + "Accept/Decline/View More Details/Log Out. Empty Booking-for address is "
            + "BUGS_FOUND #20 — record, do not open a new number. If extend-time, fail #15.")
    public void upcomingStripCardVisible() {
        System.out.println("WORKING_DIRECTORY=" + System.getProperty("user.dir"));
        HomePage home = reachUsableRentalHomeOrFailExtendTime();

        int cards = home.upcomingStripCardCount();
        String machine = home.firstUpcomingMachineNow();
        String plate = home.firstUpcomingPlateNow();
        String address = home.firstUpcomingBookingForAddressNow();
        boolean addressEmpty = address.isBlank();

        Allure.parameter("workingDirectory", System.getProperty("user.dir"));
        Allure.parameter("phone", rentalCompanyPhone());
        Allure.parameter("package", String.valueOf(vendorPackage()));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("upcomingStrip", String.valueOf(home.isUpcomingStripCardVisible()));
        Allure.parameter("stripCards", String.valueOf(cards));
        Allure.parameter("machine", machine);
        Allure.parameter("plate", plate);
        Allure.parameter("bookingForAddress", address);
        Allure.parameter("bookingForEmpty", String.valueOf(addressEmpty));
        Allure.parameter("knownBug", addressEmpty ? "20" : "none");
        home.attachScreenshot("rental-home-rhl12-upcoming-strip");

        assertThat(rentalCompanyPhone()).isEqualTo("9000000001");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow()).as("Must stay on usable Rental Home").isTrue();
        assertThat(home.isUpcomingStripCardVisible())
                .as("Dump: Upcoming Booking strip + Booking for card")
                .isTrue();
        assertThat(cards)
                .as("At least the first Upcoming card; a clipped second card may also count")
                .isGreaterThanOrEqualTo(1);
        assertThat(machine.isBlank() && plate.isBlank())
                .as("Dump card has a machine name or plate (KA13Z2117 or live equivalent)")
                .isFalse();
    }

    @Test(priority = 7, description = "RH-L13: Booking Orders feed and Timer are visible")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 21 Sep: Booking Orders, Timer mm:ss, Decline, Accept. Do not tap "
            + "Decline/Accept/View More. Missing View More Details is BUGS_FOUND #22.")
    public void bookingOrdersFeedVisible() {
        System.out.println("WORKING_DIRECTORY=" + System.getProperty("user.dir"));
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        if (!home.isBookingOrdersFeedVisible()) {
            home.swipeHomeUp();
        }
        try {
            Waits.until(DriverManager.get(),
                    d -> home.isBookingOrdersFeedVisible() ? Boolean.TRUE : null,
                    "Booking Orders feed did not leave the Home skeleton",
                    Duration.ofSeconds(12));
        } catch (TimeoutException e) {
            Allure.parameter("feedAfterWait", "false");
        }
        java.util.List<String> timers = home.bookingOrderTimersNow();
        if (timers.isEmpty()) {
            try {
                Waits.until(DriverManager.get(),
                        d -> !home.bookingOrderTimersNow().isEmpty() ? Boolean.TRUE : null,
                        "No mm:ss Timer after feed labels",
                        Duration.ofSeconds(8));
            } catch (TimeoutException ignored) {
                // assert below
            }
            timers = home.bookingOrderTimersNow();
        }
        Allure.parameter("workingDirectory", System.getProperty("user.dir"));
        Allure.parameter("phone", rentalCompanyPhone());
        Allure.parameter("bookingOrders", String.valueOf(home.isBookingOrdersFeedVisible()));
        Allure.parameter("timerCount", String.valueOf(timers.size()));
        Allure.parameter("timers", String.join(",", timers));
        Allure.parameter("viewMore", String.valueOf(home.isViewMoreDetailsVisible()));
        Allure.parameter("knownBug", home.isViewMoreDetailsVisible() ? "none" : "22");
        home.attachScreenshot("rental-home-rhl13-booking-orders");
        assertThat(home.isDisplayedNow()).isTrue();
        assertThat(home.isBookingOrdersFeedVisible())
                .as("Dump: Booking Orders + Timer + Decline + Accept (do not tap). "
                        + "First paint can be skeleton (₹ 0 placeholders).")
                .isTrue();
        assertThat(timers).as("At least one mm:ss Timer after skeleton").isNotEmpty();
    }

    @Test(priority = 8, description = "RH-L14: header Profile, Monthly, Notifications — drawer closed")
    @Severity(SeverityLevel.CRITICAL)
    public void headerChromeDrawerClosed() {
        System.out.println("WORKING_DIRECTORY=" + System.getProperty("user.dir"));
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        boolean drawer = profileDrawerNow();
        Allure.parameter("profile", String.valueOf(home.isProfileVisible()));
        Allure.parameter("monthly", String.valueOf(home.isPeriodFilterVisible()));
        Allure.parameter("notifications", String.valueOf(home.isNotificationsVisible()));
        Allure.parameter("drawer", String.valueOf(drawer));
        home.attachScreenshot("rental-home-rhl14-header");
        assertThat(home.isDisplayedNow()).isTrue();
        assertThat(home.isProfileVisible()).isTrue();
        assertThat(home.isPeriodFilterVisible()).isTrue();
        assertThat(home.isNotificationsVisible()).isTrue();
        assertThat(drawer).as("Account drawer must start closed").isFalse();
    }

    @Test(priority = 9, description = "RH-L15: vertical swipe keeps rental Home chrome")
    @Severity(SeverityLevel.NORMAL)
    public void verticalSwipeKeepsChrome() {
        System.out.println("WORKING_DIRECTORY=" + System.getProperty("user.dir"));
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.swipeHomeUp();
        Allure.parameter("homeAfter", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("tabsAfter", String.valueOf(home.isRentalBottomTabsVisible()));
        home.attachScreenshot("rental-home-rhl15-v-swipe");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow() || home.isRentalBottomTabsVisible()).isTrue();
        assertThat(home.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 10, description = "RH-L16: Upcoming strip swipes horizontally")
    @Severity(SeverityLevel.NORMAL)
    @Description("Second card is clipped. Swipe left then right. Stay Home. Do not tap cards.")
    public void upcomingStripHorizontalSwipe() {
        System.out.println("WORKING_DIRECTORY=" + System.getProperty("user.dir"));
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        String beforePlate = home.firstUpcomingPlateNow();
        String beforeMachine = home.firstUpcomingMachineNow();
        home.swipeRentalUpcomingStrip("left");
        String afterLeft = home.firstUpcomingPlateNow() + "|" + home.firstUpcomingMachineNow();
        home.swipeRentalUpcomingStrip("right");
        String named = classifyRentalNow();
        boolean launcher = launcherNow();
        Allure.parameter("beforePlate", beforePlate);
        Allure.parameter("beforeMachine", beforeMachine);
        Allure.parameter("afterLeft", afterLeft);
        Allure.parameter("afterRightPlate", home.firstUpcomingPlateNow());
        Allure.parameter("after", named);
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("launcher", String.valueOf(launcher));
        home.attachScreenshot("rental-home-rhl16-h-swipe");
        if (launcher) {
            Allure.parameter("bug", "25");
        }
        assertThat(launcher)
                .as("BUGS_FOUND #25: Upcoming strip swipe must not fire Android Back to the "
                        + "launcher. Do not fold into #13/#17. Gesture is inset from the left edge.")
                .isFalse();
        assertThat(home.isDisplayedNow() || home.isRentalBottomTabsVisible())
                .as("Must stay on Rental Home after Upcoming strip swipe")
                .isTrue();
    }

    @Test(priority = 11, description = "RH-L17: Booking Orders timers tick independently")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Read each mm:ss. Wait until a value changes (no hardcoded sleep). Two-plus "
            + "timers must not be assumed in sync. Do not tap Accept/Decline.")
    public void bookingOrderTimersTickIndependently() {
        System.out.println("WORKING_DIRECTORY=" + System.getProperty("user.dir"));
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        java.util.List<String> before = home.bookingOrderTimersNow();
        Waits.until(DriverManager.get(),
                d -> {
                    java.util.List<String> now = home.bookingOrderTimersNow();
                    return !now.equals(before) ? Boolean.TRUE : null;
                },
                "No Booking Orders timer changed within 15s",
                Duration.ofSeconds(15));
        java.util.List<String> after = home.bookingOrderTimersNow();
        Allure.parameter("before", String.join(",", before));
        Allure.parameter("after", String.join(",", after));
        Allure.parameter("timerCount", String.valueOf(after.size()));
        home.attachScreenshot("rental-home-rhl17-timers");
        assertThat(before).isNotEmpty();
        assertThat(after).isNotEqualTo(before);
        if (before.size() >= 2 && after.size() >= 2 && before.get(0).equals(before.get(1))) {
            Allure.parameter("startedInSync", "true");
        }
        if (before.size() >= 2 && after.size() >= 2) {
            boolean sameDelta = before.get(0).equals(after.get(0)) == before.get(1).equals(after.get(1));
            Allure.parameter("bothMovedOrBothStuckTogether", String.valueOf(sameDelta));
        }
    }
}
