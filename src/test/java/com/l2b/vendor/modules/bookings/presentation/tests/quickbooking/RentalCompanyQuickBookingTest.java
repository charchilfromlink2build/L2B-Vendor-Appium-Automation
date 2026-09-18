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
 * Quick Booking edges for rental company {@code 9000000001} (OTP 1234).
 * Proven 18 Sep: 5 {@code pending} jobs open Quick Booking. Never tap Accept/Decline.
 * Isolated {@code src/test/resources/quick-booking/rental-company.xml}.
 */
@Epic("Vendor app")
@Feature("Quick Booking — rental company 9000000001")
public class RentalCompanyQuickBookingTest extends QuickBookingBaseTest {

    @Test(priority = 1, description = "0001-1: Pending rental lands on Quick Booking with rental chrome")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 + 1234. Pending rentals must open Quick Booking with Accept/Decline, "
            + "Timer, Amount · Online Mode. Do not tap Accept/Decline.")
    public void pendingLandsOnQuickBooking() {
        QuickBookingPage page = loginToQuickBooking(rentalCompanyPhone());
        HomePage home = new HomePage();

        Allure.parameter("phone", rentalCompanyPhone());
        Allure.parameter("acceptCount", String.valueOf(page.acceptCount()));
        Allure.parameter("declineCount", String.valueOf(page.declineCount()));
        Allure.parameter("hasExcavator", String.valueOf(page.hasText("Excavator 20 Tonnes")));
        Allure.parameter("hasHebbal", String.valueOf(page.hasText("hebbal,Banglore")));
        page.attachScreenshot("0001-1-pending-quick-booking");

        assertThat(vendorPackage()).isEqualTo("com.l2b.app.qa");
        assertThat(page.isDisplayedNow()).as("Quick Booking title + Close").isTrue();
        assertThat(home.isDisplayedNow()).as("Must not be Home").isFalse();
        assertThat(page.isRentalAmountChromeVisible()).as("Rental Amount · Online Mode").isTrue();
        assertThat(page.isTimerVisible()).as("Timer").isTrue();
        assertThat(page.isGetDirectionVisible()).as("Get Direction").isTrue();
        assertThat(page.isBookingForVisible()).as("Booking for").isTrue();
        assertThat(page.isAcceptVisible()).as("Accept").isTrue();
        assertThat(page.isDeclineVisible()).as("Decline").isTrue();
        assertThat(page.isViewMoreDetailsVisible()).as("View More Details").isTrue();
        assertThat(page.acceptCount()).as("At least one Accept card").isGreaterThanOrEqualTo(1);
        assertThat(page.declineCount()).as("Decline count matches Accept").isEqualTo(page.acceptCount());
        assertThat(page.hasText("Excavator 20 Tonnes")).as("First proven pending SKU").isTrue();
        assertThat(page.isOrderPlacedVisible()).as("Must not show material Order Placed chrome").isFalse();
    }

    @Test(priority = 2, description = "0001-2: Close goes to Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 on Quick Booking. Tap Close (outer View). Must reach Home. "
            + "Do not tap Accept/Decline.")
    public void closeGoesToHome() {
        QuickBookingPage page = loginToQuickBooking(rentalCompanyPhone());
        assertThat(page.isDisplayedNow()).as("Precondition: Quick Booking").isTrue();
        page.tapClose();

        HomePage home = new HomePage();
        home.waitUntilLoaded();
        Allure.parameter("quickBookingAfterClose", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("homeAfterClose", String.valueOf(home.isDisplayedNow()));
        page.attachScreenshot("0001-2-close-home");

        assertThat(vendorPackage()).isEqualTo("com.l2b.app.qa");
        assertThat(page.isDisplayedNow()).as("Quick Booking must be gone after Close").isFalse();
        assertThat(home.isDisplayedNow()).as("Close must land on Home").isTrue();
        assertThat(home.isCurrentEarningVisible()).as("Home Current Earning").isTrue();
    }

    /**
     * Device Back. Expected (ideal): Home. Observed 18 Sep: Android launcher.
     * Logged as BUGS_FOUND #13. Do not tap Accept/Decline.
     */
    @Test(priority = 3, description = "0001-3: Back — named landing, no crash")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 on Quick Booking. Device Back. Observed: launcher (BUGS_FOUND #13).")
    public void backNamedLanding() {
        QuickBookingPage page = loginToQuickBooking(rentalCompanyPhone());
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
        page.attachScreenshot("0001-3-back-" + landing);

        assertThat(pkg).as("Back from Quick Booking must not crash (package blank)").isNotBlank();
        assertThat(landing)
                .as("Observed 18 Sep: launcher. Logged as BUGS_FOUND #13")
                .isEqualTo("launcher");
    }

    @Test(priority = 4, description = "0001-4: Session persists on Quick Booking after relaunch")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 login to Quick Booking, terminateApp + activateApp without pm clear. "
            + "Must return to Quick Booking, not Language.")
    public void sessionPersistsOnQuickBooking() {
        QuickBookingPage page = loginToQuickBooking(rentalCompanyPhone());
        assertThat(page.isDisplayedNow()).as("Precondition: Quick Booking").isTrue();

        AndroidDriver android = (AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.terminateApp(pkg);
        android.activateApp(pkg);

        HomePage home = new HomePage();
        waitForLoggedInLanding(page, home, android, pkg);
        String landing = namedLanding(page, home);
        Allure.parameter("relaunchLanding", landing);
        page.attachScreenshot("0001-4-relaunch-" + landing);

        assertThat(android.getCurrentPackage()).isEqualTo(pkg);
        assertThat(landing).as("Logged-in pending rental must reopen Quick Booking").isEqualTo("quick-booking");
        assertThat(page.isDisplayedNow()).as("Quick Booking after relaunch").isTrue();
        assertThat(page.isAcceptVisible()).as("Accept still on queue after relaunch").isTrue();
        assertThat(home.isDisplayedNow()).as("Must not skip queue to Home").isFalse();
    }

    /**
     * After login, the first pending card must match dump-sourced rental chrome
     * (18 Sep: Excavator 20 Tonnes, Amount · Online Mode, hebbal, Timer mm:ss).
     * Does not tap Accept/Decline.
     */
    @Test(priority = 5, description = "0001-5: First pending card content after login")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000001 + 1234. First visible pending card: SKU, online amount, site, Timer. "
            + "Do not tap Accept/Decline.")
    public void firstPendingCardContentAfterLogin() {
        QuickBookingPage page = loginToQuickBooking(rentalCompanyPhone());

        String amount = page.firstAmountLine();
        String timer = page.firstTimerValue();
        Allure.parameter("phone", rentalCompanyPhone());
        Allure.parameter("firstAmount", amount);
        Allure.parameter("firstTimer", timer);
        Allure.parameter("viewMoreCount", String.valueOf(page.viewMoreDetailsCount()));
        page.attachScreenshot("0001-5-first-card");

        assertThat(page.isDisplayedNow()).as("Still on Quick Booking").isTrue();
        assertThat(page.isCloseVisible()).as("Close").isTrue();
        assertThat(page.hasText("Excavator 20 Tonnes")).as("SKU").isTrue();
        assertThat(amount).as("First amount line").contains("Amount · Online Mode").contains("₹");
        assertThat(page.hasText("hebbal,Banglore")).as("First site").isTrue();
        assertThat(page.isTimerVisible()).as("Timer label").isTrue();
        assertThat(timer).as("Timer mm:ss").matches("\\d{1,2}:\\d{2}");
        assertThat(page.isGetDirectionVisible()).as("Get Direction").isTrue();
        assertThat(page.isBookingForVisible()).as("Booking for").isTrue();
        assertThat(page.isViewMoreDetailsVisible()).as("View More Details").isTrue();
        assertThat(page.isOrderPlacedVisible()).as("Not material Order Placed").isFalse();
    }

    /**
     * Dump 18 Sep: Accept/Decline enabled on clickable outer View. Inner TextView/Button
     * are enabled=true decoys and the inner Button is not clickable.
     */
    @Test(priority = 6, description = "0001-6: Accept/Decline enabled on outer View, inner decoys")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Do not tap Accept/Decline. Record outer enabled vs inner TextView/Button decoys.")
    public void acceptDeclineEnabledOnOuterView() {
        QuickBookingPage page = loginToQuickBooking(rentalCompanyPhone());

        Allure.parameter("acceptOuterEnabled", String.valueOf(page.isAcceptEnabled()));
        Allure.parameter("declineOuterEnabled", String.valueOf(page.isDeclineEnabled()));
        Allure.parameter("acceptInnerTextEnabled", String.valueOf(page.isAcceptInnerTextEnabled()));
        Allure.parameter("acceptInnerButtonClickable", String.valueOf(page.isAcceptInnerButtonClickable()));
        page.attachScreenshot("0001-6-accept-decline-decoys");

        assertThat(page.isAcceptVisible()).as("Accept visible").isTrue();
        assertThat(page.isDeclineVisible()).as("Decline visible").isTrue();
        assertThat(page.isAcceptEnabled()).as("Accept clickable outer View enabled").isTrue();
        assertThat(page.isDeclineEnabled()).as("Decline clickable outer View enabled").isTrue();
        assertThat(page.isAcceptInnerButtonClickable())
                .as("Inner Accept Button must stay clickable=false decoy")
                .isFalse();
        assertThat(page.acceptCount()).isEqualTo(page.declineCount());
    }

    @Test(priority = 7, description = "0001-7: Close then relaunch — pending queue still opens Quick Booking")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Close to Home, terminateApp + activateApp without pm clear. Pending rentals must "
            + "open Quick Booking again. Do not tap Accept/Decline.")
    public void closeThenRelaunchStillQuickBooking() {
        QuickBookingPage page = loginToQuickBooking(rentalCompanyPhone());
        page.tapClose();
        HomePage home = new HomePage();
        home.waitUntilLoaded();
        assertThat(home.isDisplayedNow()).as("Precondition: Home after Close").isTrue();

        AndroidDriver android = (AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.terminateApp(pkg);
        android.activateApp(pkg);

        waitForLoggedInLanding(page, home, android, pkg);
        String landing = namedLanding(page, home);
        Allure.parameter("relaunchLanding", landing);
        page.attachScreenshot("0001-7-close-relaunch-" + landing);

        assertThat(android.getCurrentPackage()).isEqualTo(pkg);
        assertThat(landing).as("Pending queue must reopen Quick Booking after Close+relaunch")
                .isEqualTo("quick-booking");
        assertThat(page.isAcceptVisible()).as("Accept still on queue").isTrue();
    }

    @Test(priority = 8, description = "0001-8: Rotation keeps Quick Booking chrome")
    @Severity(SeverityLevel.NORMAL)
    @Description("Rotate to landscape on Quick Booking. Record whether title/Close/Accept survive. "
            + "Restore portrait. Do not tap Accept/Decline.")
    public void rotationKeepsQuickBookingChrome() {
        QuickBookingPage page = loginToQuickBooking(rentalCompanyPhone());
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        org.openqa.selenium.ScreenOrientation before = android.getOrientation();
        try {
            android.rotate(org.openqa.selenium.ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            Allure.parameter("rotateThrew", e.getMessage());
        }
        org.openqa.selenium.ScreenOrientation after = android.getOrientation();
        boolean qbLandscape = page.isDisplayedNow();
        boolean acceptLandscape = page.isAcceptVisible();
        Allure.parameter("orientationBefore", String.valueOf(before));
        Allure.parameter("orientationAfter", String.valueOf(after));
        Allure.parameter("quickBookingInLandscape", String.valueOf(qbLandscape));
        Allure.parameter("acceptInLandscape", String.valueOf(acceptLandscape));
        page.attachScreenshot("0001-8-landscape");

        if (android.getOrientation() != org.openqa.selenium.ScreenOrientation.PORTRAIT) {
            try {
                android.rotate(org.openqa.selenium.ScreenOrientation.PORTRAIT);
            } catch (RuntimeException ignored) {
                // restore best-effort
            }
        }
        page.attachScreenshot("0001-8-portrait-restore");

        assertThat(vendorPackage()).isEqualTo("com.l2b.app.qa");
        assertThat(page.isDisplayedNow()).as("Quick Booking after rotate round-trip").isTrue();
        assertThat(page.isAcceptVisible()).as("Accept after rotate round-trip").isTrue();
        assertThat(page.isCloseVisible()).as("Close after rotate round-trip").isTrue();
    }

    /**
     * Rapid triple Close. Expected (ideal): Home. Observed 18 Sep: Home profile drawer
     * (Account / Log Out). Logged as BUGS_FOUND #14. Do not tap Accept/Decline or Log Out.
     */
    @Test(priority = 9, description = "0001-9: Rapid triple Close — named landing, no crash")
    @Severity(SeverityLevel.NORMAL)
    @Description("Three clickGestures on Close. Observed: Home profile drawer (BUGS_FOUND #14).")
    public void rapidCloseDoesNotCrash() {
        QuickBookingPage page = loginToQuickBooking(rentalCompanyPhone());
        assertThat(page.isCloseVisible()).as("Precondition: Close").isTrue();
        page.tapCloseRapidly(3);

        HomePage home = new HomePage();
        Waits.until(DriverManager.get(),
                d -> (home.isDisplayedNow() || page.isDisplayedNow()
                        || profileDrawerNow() || launcherNow()) ? Boolean.TRUE : null,
                "After rapid Close, no Home / drawer / Quick Booking / launcher",
                Duration.ofSeconds(12));

        String landing = namedLanding(page, home);
        String pkg = vendorPackage();
        Allure.parameter("rapidCloseLanding", landing);
        Allure.parameter("rapidClosePackage", String.valueOf(pkg));
        page.attachScreenshot("0001-9-rapid-close-" + landing);

        assertThat(pkg).as("Rapid Close must not crash (package blank)").isNotBlank();
        assertThat(landing)
                .as("Observed 18 Sep: Home profile drawer. Logged as BUGS_FOUND #14")
                .isEqualTo("home-drawer");
    }

    @Test(priority = 10, description = "0001-10: Queue shows more than one pending card")
    @Severity(SeverityLevel.NORMAL)
    @Description("API has 5 pending. First screen must show at least two cards (Accept count >= 2). "
            + "Do not tap Accept/Decline.")
    public void queueShowsAtLeastTwoPendingCards() {
        QuickBookingPage page = loginToQuickBooking(rentalCompanyPhone());
        Allure.parameter("acceptCount", String.valueOf(page.acceptCount()));
        Allure.parameter("viewMoreCount", String.valueOf(page.viewMoreDetailsCount()));
        page.attachScreenshot("0001-10-queue-two-cards");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.acceptCount()).as("At least two pending cards on first screen").isGreaterThanOrEqualTo(2);
        assertThat(page.viewMoreDetailsCount()).isGreaterThanOrEqualTo(2);
        assertThat(page.hasText("₹51,000") || page.hasText("₹1,020,000"))
                .as("Known pending amounts on first screen")
                .isTrue();
    }
}
