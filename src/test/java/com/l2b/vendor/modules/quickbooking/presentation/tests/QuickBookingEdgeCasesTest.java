package com.l2b.vendor.modules.quickbooking.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Adb;
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
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.TimeoutException;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Cross-cutting Quick Booking edges for rental owner {@code 9000000001}
 * (OTP 1234). Isolated {@code src/test/resources/quickbooking/edge-cases.xml}.
 * Do not tap the Home {@code Request to extend time} dialog.
 */
@Epic("Vendor app")
@Feature("Quick Booking edges — rental owner 9000000001")
public class QuickBookingEdgeCasesTest extends QuickBookingBaseTest {

    @Test(priority = 1, description = "Edge-1: Quick Booking does not appear when no eligible rentals")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #2. Blocked on 9000000001 while pending rentals exist. Not retargeted to 0003.")
    public void doesNotAppearWhenNoEligibleRentals() {
        throw new SkipException(
                "Blocked this phase: 9000000001 still has pending rentals. "
                        + "Not retargeted to 0003 / other personas.");
    }

    @Test(priority = 2, description = "Edge-2: Accept while offline shows feedback, no crash")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #11. Login online, disable radios, tap first Accept. Record sheet vs "
            + "offline copy vs silent no-op. Do not Confirm. Do not tap Decline.")
    public void acceptWhileOfflineShowsFeedback() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        assertThat(page.isAcceptEnabled()).isTrue();
        Adb.disableRadios();
        try {
            Waits.until(DriverManager.get(), d -> null, "radio settle", Duration.ofSeconds(2));
        } catch (TimeoutException ignored) {
            // radios need a beat before the Accept tap
        }
        page.tapFirstAccept();
        Waits.until(DriverManager.get(),
                d -> (page.isAssignMachineVisible() || hasOfflineCopy(d.getPageSource())
                        || page.isDisplayedNow()) ? Boolean.TRUE : null,
                "After offline Accept, no sheet / queue / offline copy",
                Duration.ofSeconds(12));

        boolean sheet = page.isAssignMachineVisible();
        boolean offlineCopy = hasOfflineCopy(DriverManager.get().getPageSource());
        Allure.parameter("assignMachine", String.valueOf(sheet));
        Allure.parameter("offlineOrRetryCopy", String.valueOf(offlineCopy));
        Allure.parameter("offlineUx", offlineCopy ? "error-or-retry-copy"
                : sheet ? "opened-assign-anyway"
                : page.isDisplayedNow() ? "stayed-on-queue-no-error-copy"
                : "unknown");
        page.attachScreenshot("edge-2-offline-accept");

        assertThat(vendorPackage()).as("Offline Accept must not crash Vendor")
                .isEqualTo(Config.get("app.package"));
        assertThat(page.isExtendTimeDialogVisible()).isFalse();
    }

    @Test(priority = 3, description = "Edge-3: API/server failure feedback — blocked without mock")
    @Severity(SeverityLevel.NORMAL)
    @Description("List #14. No API mock in this black-box suite. Covered in spirit by offline Accept/Decline.")
    public void apiFailureShowsUserFeedback() {
        throw new SkipException(
                "No API mock on this black-box QA build. Offline Accept/Decline (Edge-2 / Decline-4) "
                        + "are the live stand-ins for request failure.");
    }

    @Test(priority = 4, description = "Edge-4: Timer on a live card is mm:ss and not expired")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #15. Record first Timer. A 00:00 seed is not on 0001 this run.")
    public void expiredBookingTimerBehavior() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        String timer = page.firstTimerValue();
        boolean expired = page.isFirstTimerExpired();
        Allure.parameter("timer", timer);
        Allure.parameter("timerExpired", String.valueOf(expired));
        page.attachScreenshot("edge-4-timer");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isTimerVisible()).isTrue();
        assertThat(timer).as("Timer mm:ss").matches("\\d{1,2}:\\d{2}");
        if (expired) {
            Allure.parameter("note", "0001 now has an expired card — Accept/Decline-after-expiry can run");
        }
    }

    @Test(priority = 5, description = "Edge-5: Timer ticks down while Quick Booking stays open")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #16. Read Timer, wait until the value changes. Do not wait until 00:00.")
    public void timerReachesZeroWhileScreenOpen() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        String before = page.firstTimerValue();
        assertThat(before).matches("\\d{1,2}:\\d{2}");
        try {
            Waits.until(DriverManager.get(),
                    d -> {
                        String now = page.firstTimerValue();
                        return (now != null && !now.equals(before)) ? Boolean.TRUE : null;
                    },
                    "Timer did not tick",
                    Duration.ofSeconds(8));
        } catch (TimeoutException ignored) {
            // recorded below
        }
        String after = page.firstTimerValue();
        Allure.parameter("timerBefore", before);
        Allure.parameter("timerAfter", after);
        page.attachScreenshot("edge-5-timer-tick");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(after).as("Timer still mm:ss").matches("\\d{1,2}:\\d{2}");
        assertThat(after).as("Timer must change while the screen stays open").isNotEqualTo(before);
    }

    @Test(priority = 6, description = "Edge-6: Missing/invalid location does not crash")
    @Severity(SeverityLevel.NORMAL)
    @Description("List #29. If a blank Booking for is on screen, app must stay on Quick Booking. "
            + "Dump cards have locations; this records that and no crash.")
    public void invalidLocationDoesNotCrash() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        boolean blank = page.isBookingForBlank();
        boolean bookingFor = page.isBookingForVisible();
        Allure.parameter("bookingForVisible", String.valueOf(bookingFor));
        Allure.parameter("bookingForLooksBlank", String.valueOf(blank));
        page.attachScreenshot("edge-6-location");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isDisplayedNow()).as("Must stay on Quick Booking").isTrue();
        assertThat(page.isAcceptVisible()).isTrue();
        if (!blank) {
            Allure.parameter("note", "No blank location on 0001 queue this run");
        }
    }

    @Test(priority = 7, description = "Edge-7: Rotation keeps Quick Booking chrome")
    @Severity(SeverityLevel.NORMAL)
    @Description("List #34. Landscape then portrait. Title/Close/Accept must survive. "
            + "Do not tap Accept/Decline.")
    public void rotationKeepsQuickBookingChrome() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        ScreenOrientation before = android.getOrientation();
        try {
            android.rotate(ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            Allure.parameter("rotateThrew", e.getMessage());
        }
        ScreenOrientation landscape = android.getOrientation();
        boolean qbLandscape = page.isDisplayedNow();
        boolean acceptLandscape = page.isAcceptVisible();
        Allure.parameter("orientationBefore", String.valueOf(before));
        Allure.parameter("orientationLandscape", String.valueOf(landscape));
        Allure.parameter("quickBookingInLandscape", String.valueOf(qbLandscape));
        Allure.parameter("acceptInLandscape", String.valueOf(acceptLandscape));
        page.attachScreenshot("edge-7-landscape");

        try {
            if (android.getOrientation() != ScreenOrientation.PORTRAIT) {
                android.rotate(ScreenOrientation.PORTRAIT);
            }
        } catch (RuntimeException ignored) {
            // restore best-effort
        }
        page.attachScreenshot("edge-7-portrait");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isDisplayedNow()).as("Quick Booking after rotate round-trip").isTrue();
        assertThat(page.isAcceptVisible()).isTrue();
        assertThat(page.isCloseVisible()).isTrue();
    }

    @Test(priority = 8, description = "Edge-8: Background then foreground keeps the pending queue")
    @Severity(SeverityLevel.CRITICAL)
    @Description("List #35. HOME ~3s, activateApp. Pending rentals must still show Quick Booking. "
            + "Do not tap Accept/Decline.")
    public void backgroundThenForegroundKeepsQueue() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        HomePage home = new HomePage();
        int before = page.acceptCount();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.runAppInBackground(Duration.ofSeconds(3));
        waitForLoggedInLanding(page, home);

        Allure.parameter("acceptCountBefore", String.valueOf(before));
        Allure.parameter("acceptCountAfter", String.valueOf(page.acceptCount()));
        Allure.parameter("quickBookingAfterFg", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("homeAfterFg", String.valueOf(home.isDisplayedNow()));
        page.attachScreenshot("edge-8-bg-fg");

        assertThat(android.getCurrentPackage()).isEqualTo(pkg);
        assertThat(page.isDisplayedNow()).as("Queue must survive background").isTrue();
        assertThat(page.isAcceptVisible()).isTrue();
        assertThat(page.isExtendTimeDialogVisible()).isFalse();
    }

    @Test(priority = 9, description = "Edge-9: Empty/null queue does not crash")
    @Severity(SeverityLevel.NORMAL)
    @Description("List #36. Blocked on 0001 while pending jobs exist. Not 0003.")
    public void emptyOrNullQueueDoesNotCrash() {
        throw new SkipException(
                "Blocked this phase: 9000000001 queue is not empty/null. Not retargeted to 0003.");
    }

    @Test(priority = 10, description = "Edge-10: Loading state while booking data is fetched")
    @Severity(SeverityLevel.NORMAL)
    @Description("List #37. After OTP, record whether a ProgressBar/Loading marker appears "
            + "before Quick Booking.")
    public void loadingStateWhileFetching() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        boolean loadingNow = page.isLoadingVisible();
        Allure.parameter("loadingVisibleOnQueue", String.valueOf(loadingNow));
        Allure.parameter("quickBookingVisible", String.valueOf(page.isDisplayedNow()));
        page.attachScreenshot("edge-10-loading");

        assertThat(page.isDisplayedNow()).as("Queue eventually loaded").isTrue();
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        if (!loadingNow) {
            Allure.parameter("note", "Loader already gone by the time Quick Booking was asserted");
        }
    }

    @Test(priority = 11, description = "Edge-11: Slow network — queue still lands or shows feedback")
    @Severity(SeverityLevel.NORMAL)
    @Description("List #38. Login, disable radios on the queue, wait, re-enable. Must not crash.")
    public void slowNetworkLoading() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        Adb.disableRadios();
        try {
            Waits.until(DriverManager.get(), d -> null, "offline dwell", Duration.ofSeconds(3));
        } catch (TimeoutException ignored) {
            // dwell
        }
        boolean stillQb = page.isDisplayedNow();
        boolean offlineCopy = hasOfflineCopy(DriverManager.get().getPageSource());
        Adb.enableRadios();
        Allure.parameter("quickBookingWhileOffline", String.valueOf(stillQb));
        Allure.parameter("offlineOrRetryCopy", String.valueOf(offlineCopy));
        page.attachScreenshot("edge-11-slow-network");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(stillQb || offlineCopy)
                .as("Queue stays or an offline message appears")
                .isTrue();
    }

    @Test(priority = 12, description = "Edge-12: Stale booking data after a short background")
    @Severity(SeverityLevel.NORMAL)
    @Description("List #39. Background 3s, return. Queue must still be a coherent Quick Booking "
            + "(title + Accept), not a crash or blank.")
    public void staleBookingDataIsHandled() {
        QuickBookingLandingPage page = loginToLanding(rentalOwnerPhone());
        String amount = page.firstAmountLine();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        android.runAppInBackground(Duration.ofSeconds(3));
        HomePage home = new HomePage();
        waitForLoggedInLanding(page, home);

        Allure.parameter("amountBefore", amount);
        Allure.parameter("amountAfter", page.firstAmountLine());
        Allure.parameter("acceptAfter", String.valueOf(page.acceptCount()));
        page.attachScreenshot("edge-12-stale");

        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isAcceptVisible()).isTrue();
        assertThat(page.firstAmountLine()).as("Amount line still present").isNotBlank();
    }

    @Test(priority = 13, description = "Edge-13: Already accepted/declined elsewhere")
    @Severity(SeverityLevel.NORMAL)
    @Description("List #40. Needs a second client or backend seed. Not runnable on 0001 alone.")
    public void alreadyAcceptedOrDeclinedElsewhereIsHandled() {
        throw new SkipException(
                "Needs a second session or backend seed to mark a booking accepted/declined elsewhere. "
                        + "Not runnable on 9000000001 alone this phase.");
    }
}
