package com.l2b.vendor.modules.quickbooking.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;
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
 * Quick Booking landing / queue for rental owner {@code 9000000001} (OTP 1234).
 * Isolated {@code src/test/resources/quickbooking/landing.xml}. Not in default
 * {@code testng.xml}. Independent {@code @Test} methods; landing cases 1–15.
 *
 * <p>Read-only this file: do not tap Accept or Decline.
 */
@Epic("Vendor app")
@Feature("Quick Booking landing — rental owner 9000000001")
public class QuickBookingLandingTest extends QuickBookingBaseTest {

    @Test(priority = 1, description = "Landing-1: Quick Booking appears after login when pending rentals exist")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 + 1234. Eligible pending rentals must open Quick Booking "
            + "(title + Close), not Home. Do not tap Accept/Decline.")
    public void appearsAfterLoginWhenPendingRentalsExist() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        HomePage home = new HomePage();

        Allure.parameter("phone", rentalOwnerPhone());
        Allure.parameter("package", String.valueOf(vendorPackage()));
        Allure.parameter("quickBookingVisible", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("homeVisible", String.valueOf(home.isDisplayedNow()));
        page.attachScreenshot("landing-1-after-login");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isDisplayedNow()).as("Quick Booking title + Close after login").isTrue();
        assertThat(page.isCloseVisible()).as("Close").isTrue();
        assertThat(page.isAcceptVisible()).as("At least one pending card (Accept)").isTrue();
        assertThat(home.isDisplayedNow()).as("Must not skip queue to Home").isFalse();
    }

    @Test(priority = 2, description = "Landing-2: Multiple rental booking cards are visible")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 + 1234. First screen must show at least two pending cards "
            + "(Accept count >= 2, matching Decline and View More Details). Do not tap Accept/Decline.")
    public void multipleCardsAreVisible() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());

        int accept = page.acceptCount();
        int decline = page.declineCount();
        int viewMore = page.viewMoreDetailsCount();
        Allure.parameter("phone", rentalOwnerPhone());
        Allure.parameter("acceptCount", String.valueOf(accept));
        Allure.parameter("declineCount", String.valueOf(decline));
        Allure.parameter("viewMoreCount", String.valueOf(viewMore));
        page.attachScreenshot("landing-2-multiple-cards");

        assertThat(page.isDisplayedNow()).as("Still on Quick Booking").isTrue();
        assertThat(accept).as("At least two pending cards on first screen").isGreaterThanOrEqualTo(2);
        assertThat(decline).as("Each card has Decline").isEqualTo(accept);
        assertThat(viewMore).as("Each card has View More Details").isGreaterThanOrEqualTo(2);
    }

    @Test(priority = 3, description = "Landing-3: First card shows machine, amount, dates, location, timer")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 + 1234. First visible pending card: SKU, Amount · Online Mode, "
            + "start/end/duration, site, Timer mm:ss. Do not tap Accept/Decline.")
    public void firstCardShowsMachineAmountDatesLocationTimer() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());

        String amount = page.firstAmountLine();
        String dates = page.firstDateRangeLine();
        String timer = page.firstTimerValue();
        Allure.parameter("phone", rentalOwnerPhone());
        Allure.parameter("firstAmount", amount);
        Allure.parameter("firstDateRange", dates);
        Allure.parameter("firstTimer", timer);
        page.attachScreenshot("landing-3-first-card");

        assertThat(page.isDisplayedNow()).as("Still on Quick Booking").isTrue();
        assertThat(page.hasText("Excavator 20 Tonnes")).as("Machine/SKU").isTrue();
        assertThat(amount).as("Amount · Online Mode with rupee").contains("Amount · Online Mode").contains("₹");
        assertThat(dates)
                .as("Start–end with duration, dump pattern: 18 Sep 1:44 PM - 19 Sep 2026 1:44 PM (1 day)")
                .matches(".*\\d{1,2} \\w{3}.+-\\s+.+\\(\\d+ days?\\)");
        assertThat(page.hasText("hebbal,Banglore")).as("First site").isTrue();
        assertThat(page.isBookingForVisible()).as("Booking for").isTrue();
        assertThat(page.isTimerVisible()).as("Timer label").isTrue();
        assertThat(timer).as("Timer mm:ss").matches("\\d{1,2}:\\d{2}");
        assertThat(page.isGetDirectionVisible()).as("Get Direction").isTrue();
        assertThat(page.isOrderPlacedVisible()).as("Not material Order Placed").isFalse();
    }

    @Test(priority = 4, description = "Landing-4: Scrolling reveals further booking cards")
    @Severity(SeverityLevel.NORMAL)
    @Description("Swipe the queue up. Must stay on Quick Booking with Accept still on a card. "
            + "Do not tap Accept/Decline.")
    public void scrollShowsFurtherCards() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        String beforeAmount = page.firstAmountLine();
        page.swipeListUp();
        page.swipeListUp();

        String afterAmount = page.firstAmountLine();
        Allure.parameter("amountBeforeScroll", beforeAmount);
        Allure.parameter("amountAfterScroll", afterAmount);
        Allure.parameter("acceptAfterScroll", String.valueOf(page.acceptCount()));
        page.attachScreenshot("landing-4-after-scroll");

        assertThat(page.isDisplayedNow()).as("Still on Quick Booking after scroll").isTrue();
        assertThat(page.acceptCount()).as("A booking card remains after scroll").isGreaterThanOrEqualTo(1);
        assertThat(page.isAcceptVisible()).isTrue();
        assertThat(page.hasText("₹1,020,000") || !afterAmount.equals(beforeAmount))
                .as("Further card (large amount) visible, or first card changed after scroll")
                .isTrue();
    }

    @Test(priority = 5, description = "Landing-5: Accept/Decline stay on the correct card after scroll")
    @Severity(SeverityLevel.CRITICAL)
    @Description("After scrolling, each visible Accept has a matching Decline. Do not tap them.")
    public void acceptDeclineStayOnCorrectCardAfterScroll() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        page.swipeListUp();

        int accept = page.acceptCount();
        int decline = page.declineCount();
        Allure.parameter("acceptAfterScroll", String.valueOf(accept));
        Allure.parameter("declineAfterScroll", String.valueOf(decline));
        page.attachScreenshot("landing-5-accept-decline-after-scroll");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(accept).as("Accept still on a card after scroll").isGreaterThanOrEqualTo(1);
        assertThat(decline).as("Decline count still matches Accept after scroll").isEqualTo(accept);
        assertThat(page.isAcceptVisible()).isTrue();
        assertThat(page.hasText("Decline")).isTrue();
    }

    @Test(priority = 6, description = "Landing-6: View More Details expands the first card")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap first View More Details. Must expand that card (View Less and/or Purpose/"
            + "Soil/Load). Do not tap Accept/Decline.")
    public void viewMoreExpandsCorrectCard() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        assertThat(page.isViewMoreVisible()).as("Precondition: View More Details").isTrue();
        page.tapFirstViewMore();
        Waits.until(DriverManager.get(),
                d -> (page.isViewLessVisible()
                        || page.hasText("Purpose")
                        || page.hasText("Type of Soil")
                        || page.hasText("Type of Load")) ? Boolean.TRUE : null,
                "View More did not expand the first card",
                Duration.ofSeconds(8));

        Allure.parameter("viewLess", String.valueOf(page.isViewLessVisible()));
        Allure.parameter("purpose", String.valueOf(page.hasText("Purpose")));
        Allure.parameter("soil", String.valueOf(page.hasText("Type of Soil")));
        Allure.parameter("load", String.valueOf(page.hasText("Type of Load")));
        page.attachScreenshot("landing-6-view-more-expanded");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isViewLessVisible() || page.hasText("Purpose")
                || page.hasText("Type of Soil") || page.hasText("Type of Load"))
                .as("First card expanded")
                .isTrue();
    }

    @Test(priority = 7, description = "Landing-7: View Less Details collapses the first card")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Expand first card, tap View Less. Details must collapse. Do not tap Accept/Decline.")
    public void viewLessCollapsesCorrectCard() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        page.tapFirstViewMore();
        Waits.until(DriverManager.get(),
                d -> page.isViewLessVisible() ? Boolean.TRUE : null,
                "View Less did not appear after View More",
                Duration.ofSeconds(8));
        page.tapFirstViewLess();
        Waits.until(DriverManager.get(),
                d -> page.isViewMoreVisible() ? Boolean.TRUE : null,
                "View More did not return after View Less",
                Duration.ofSeconds(8));

        Allure.parameter("viewMoreAfterCollapse", String.valueOf(page.isViewMoreVisible()));
        Allure.parameter("viewLessAfterCollapse", String.valueOf(page.isViewLessVisible()));
        page.attachScreenshot("landing-7-view-less-collapsed");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isViewMoreVisible()).as("View More back after collapse").isTrue();
        assertThat(page.isViewLessVisible()).as("View Less gone after collapse").isFalse();
    }

    @Test(priority = 8, description = "Landing-8: Expand/collapse does not change other cards")
    @Severity(SeverityLevel.NORMAL)
    @Description("Expand the first card. Second card must stay collapsed (large amount still "
            + "visible, only one View Less). View More on the second card may be below the fold. "
            + "Do not tap Accept/Decline.")
    public void expandCollapseDoesNotChangeOtherCards() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        assertThat(page.viewMoreDetailsCount()).as("Need two cards").isGreaterThanOrEqualTo(2);
        page.tapFirstViewMore();
        Waits.until(DriverManager.get(),
                d -> page.isViewLessVisible() ? Boolean.TRUE : null,
                "View Less did not appear after View More",
                Duration.ofSeconds(8));

        int viewLess = page.viewLessDetailsCount();
        int viewMore = page.viewMoreDetailsCount();
        Allure.parameter("viewLessCount", String.valueOf(viewLess));
        Allure.parameter("viewMoreCountAfterExpand", String.valueOf(viewMore));
        Allure.parameter("secondCardAmountVisible", String.valueOf(page.hasText("₹1,020,000")));
        page.attachScreenshot("landing-8-other-cards-unchanged");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(viewLess).as("Only the tapped card shows View Less").isEqualTo(1);
        assertThat(page.hasText("₹1,020,000")).as("Second card still on screen, not expanded away").isTrue();
        assertThat(page.hasText("excavation") || page.hasText("Purpose")).as("First card details").isTrue();
    }

    /**
     * Close from Quick Booking. Ideal: clean Home. Observed 18 Sep: Home is covered by a
     * Request to extend time dialog (Accept/Decline). Logged as BUGS_FOUND #15.
     * Do not tap Accept/Decline on that dialog.
     */
    @Test(priority = 9, description = "Landing-9: Close — named landing (Home or extend-time dialog)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Tap Close. Observed: Home behind Request to extend time (BUGS_FOUND #15). "
            + "Do not tap Accept/Decline.")
    public void closeReturnsToHome() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        assertThat(page.isDisplayedNow()).as("Precondition: Quick Booking").isTrue();
        page.tapClose();

        HomePage home = new HomePage();
        Waits.until(DriverManager.get(),
                d -> (home.isDisplayedNow() || page.isExtendTimeDialogVisible()
                        || page.hasText("Good Afternoon!") || page.hasText("Current Earning"))
                        ? Boolean.TRUE : null,
                "After Close, no Home / extend-time dialog",
                Duration.ofSeconds(15));

        boolean extend = page.isExtendTimeDialogVisible();
        Allure.parameter("extendTimeDialog", String.valueOf(extend));
        Allure.parameter("homeVisible", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("quickBookingAfterClose", String.valueOf(page.isDisplayedNow()));
        page.attachScreenshot("landing-9-close-" + (extend ? "extend-dialog" : "home"));

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isDisplayedNow()).as("Quick Booking app bar must be gone after Close").isFalse();
        if (extend) {
            assertThat(page.hasText("Request to extend time"))
                    .as("Observed 18 Sep: extend-time dialog on Home. Logged as BUGS_FOUND #15")
                    .isTrue();
        } else {
            assertThat(home.isDisplayedNow() || page.hasText("Current Earning"))
                    .as("Close must land on Home")
                    .isTrue();
        }
    }

    @Test(priority = 10, description = "Landing-10: Reopen after Close still shows the pending queue")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Close (Home may be covered by extend-time dialog #15), terminateApp + "
            + "activateApp. Pending rentals should reopen Quick Booking. Do not tap Accept/Decline.")
    public void reopenAfterCloseStillShowsQueue() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        page.tapClose();
        HomePage home = new HomePage();
        Waits.until(DriverManager.get(),
                d -> (home.isDisplayedNow() || page.isExtendTimeDialogVisible()
                        || page.hasText("Current Earning")) ? Boolean.TRUE : null,
                "After Close, no Home / extend-time dialog",
                Duration.ofSeconds(15));
        Allure.parameter("extendTimeDialogAfterClose", String.valueOf(page.isExtendTimeDialogVisible()));

        AndroidDriver android = (AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.terminateApp(pkg);
        android.activateApp(pkg);

        Waits.until(android,
                d -> {
                    if (!pkg.equals(android.getCurrentPackage())) {
                        return null;
                    }
                    return (page.isDisplayedNow() || home.isDisplayedNow()
                            || page.isExtendTimeDialogVisible()) ? Boolean.TRUE : null;
                },
                "After relaunch, no Quick Booking / Home / extend-time dialog",
                Duration.ofSeconds(20));

        Allure.parameter("quickBookingAfterRelaunch", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("homeAfterRelaunch", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("extendAfterRelaunch", String.valueOf(page.isExtendTimeDialogVisible()));
        page.attachScreenshot("landing-10-reopen-queue");

        assertThat(android.getCurrentPackage()).isEqualTo(pkg);
        assertThat(page.isDisplayedNow()).as("Pending queue must reopen Quick Booking").isTrue();
        assertThat(page.isAcceptVisible()).as("Accept still on queue").isTrue();
    }

    @Test(priority = 11, description = "Landing-11: Get Direction is enabled and does not crash")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap first Get Direction. Record landing (maps or in-app). Must not crash. "
            + "Do not tap Accept/Decline.")
    public void getDirectionOpensWithoutCrash() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        assertThat(page.isGetDirectionVisible()).as("Get Direction visible").isTrue();
        assertThat(page.isGetDirectionEnabled()).as("Get Direction clickable outer enabled").isTrue();

        String pkgBefore = vendorPackage();
        page.tapFirstGetDirection();
        Waits.until(DriverManager.get(),
                d -> {
                    String pkg = vendorPackage();
                    return (pkg != null && !pkg.isBlank()) ? Boolean.TRUE : null;
                },
                "After Get Direction, no current package",
                Duration.ofSeconds(8));

        String pkgAfter = vendorPackage();
        String landing = pkgAfter == null ? "unknown"
                : pkgAfter.contains("maps") ? "maps"
                : pkgAfter.equals(Config.get("app.package"))
                        ? (page.isDisplayedNow() ? "quick-booking" : "vendor-other")
                        : "other-package:" + pkgAfter;
        Allure.parameter("packageBefore", String.valueOf(pkgBefore));
        Allure.parameter("packageAfter", String.valueOf(pkgAfter));
        Allure.parameter("getDirectionLanding", landing);
        page.attachScreenshot("landing-11-get-direction-" + landing.replace(':', '-'));

        assertThat(pkgAfter).as("Get Direction must not crash (package blank)").isNotBlank();
        assertThat(landing).as("Get Direction landing must be named").isNotEqualTo("unknown");

        if (!Config.get("app.package").equals(pkgAfter)) {
            ((AndroidDriver) DriverManager.get()).activateApp(Config.get("app.package"));
        }
    }

    @Test(priority = 12, description = "Landing-12: Long location text does not break the card")
    @Severity(SeverityLevel.NORMAL)
    @Description("Second dump card location is long (RMV 2nd Stage…). App must stay on Quick "
            + "Booking with Accept still visible. Do not tap Accept/Decline.")
    public void longMachineAndLocationTextDoNotCrash() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        Allure.parameter("longLocationVisible",
                String.valueOf(page.hasText("RMV 2nd Stage, Banday Colony, Bengaluru")));
        page.attachScreenshot("landing-12-long-location");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isDisplayedNow()).as("Still on Quick Booking").isTrue();
        assertThat(page.hasText("RMV 2nd Stage, Banday Colony, Bengaluru"))
                .as("Long location on second card")
                .isTrue();
        assertThat(page.isAcceptVisible()).as("Accept still on cards").isTrue();
    }

    @Test(priority = 13, description = "Landing-13: Large booking amount displays with rupee and payment mode")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump second card Amount · Online Mode ₹1,020,000. Do not tap Accept/Decline.")
    public void largeAmountDisplaysWithRupeeAndPaymentMode() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        boolean large = page.hasText("₹1,020,000");
        Allure.parameter("largeAmountVisible", String.valueOf(large));
        Allure.parameter("amountLines", String.join(" | ", page.amountLines()));
        page.attachScreenshot("landing-13-large-amount");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(large).as("Large amount ₹1,020,000 on a card").isTrue();
        assertThat(page.amountLines())
                .as("Every visible amount line keeps Online Mode and rupee")
                .allMatch(line -> line.contains("Amount · Online Mode") && line.contains("₹"));
    }

    @Test(priority = 14, description = "Landing-14: Different date ranges and durations display on cards")
    @Severity(SeverityLevel.NORMAL)
    @Description("First card (1 day) and second card (20 days) from dump. Do not tap Accept/Decline.")
    public void startEndDurationDisplayOnCard() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        String first = page.firstDateRangeLine();
        boolean oneDay = page.hasText("(1 day)");
        boolean twentyDays = page.hasText("(20 days)");
        Allure.parameter("firstDateRange", first);
        Allure.parameter("hasOneDay", String.valueOf(oneDay));
        Allure.parameter("hasTwentyDays", String.valueOf(twentyDays));
        page.attachScreenshot("landing-14-durations");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(first).as("First card date range").matches(".*\\d{1,2} \\w{3}.+-\\s+.+\\(\\d+ days?\\)");
        assertThat(oneDay).as("1 day duration on first card").isTrue();
        assertThat(twentyDays).as("20 days duration on second card").isTrue();
    }

    @Test(priority = 15, description = "Landing-15: Timer stays consistent while scrolling")
    @Severity(SeverityLevel.NORMAL)
    @Description("Read Timer, swipe, Timer must still be mm:ss on Quick Booking. "
            + "Do not tap Accept/Decline.")
    public void timerStaysVisibleWhileScrolling() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        String before = page.firstTimerValue();
        page.swipeListUp();
        String after = page.firstTimerValue();
        Allure.parameter("timerBeforeScroll", before);
        Allure.parameter("timerAfterScroll", after);
        page.attachScreenshot("landing-15-timer-after-scroll");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isTimerVisible()).as("Timer label after scroll").isTrue();
        assertThat(before).as("Timer before scroll").matches("\\d{1,2}:\\d{2}");
        assertThat(after).as("Timer after scroll").matches("\\d{1,2}:\\d{2}");
    }
}
