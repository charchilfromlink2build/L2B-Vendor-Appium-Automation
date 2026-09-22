package com.l2b.vendor.modules.home.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
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
 * Rental Home edges for {@code 9000000001}. New bugs get new numbers — do not
 * fold into #13/#14/#15/#17/#18/#19. Never Accept, Decline, or Log Out.
 */
@Epic("Vendor app")
@Feature("Home edges — rental vendor 9000000001")
public class RentalHomeEdgeTest extends RentalHomeBaseTest {

    @Test(priority = 1, description = "RH-E1: device Back on Rental Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("If launcher: new Rental Home bug. Do not fold into #13 or #17.")
    public void backOnHome() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        ((AndroidDriver) DriverManager.get()).pressKey(new KeyEvent(AndroidKey.BACK));
        String named = classifyRentalNow();
        boolean launcher = launcherNow();
        Allure.parameter("after", named);
        Allure.parameter("package", String.valueOf(vendorPackage()));
        Allure.parameter("launcher", String.valueOf(launcher));
        home.attachScreenshot("rental-home-rhe1-back");
        if (launcher) {
            Allure.parameter("bug", "25");
        }
        assertThat(launcher)
                .as("BUGS_FOUND #25: device Back on Rental Home exits to the Android launcher. "
                        + "Do not fold into #13 (Quick Booking Back) or #17 (material Home Back).")
                .isFalse();
        assertThat(home.isDisplayedNow() || home.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 2, description = "RH-E2: rotate landscape then portrait")
    @Severity(SeverityLevel.NORMAL)
    @Description("If drawer opens: new number. Do not fold into #18.")
    public void rotationKeepsHome() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        try {
            android.rotate(ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            Allure.parameter("rotateThrew", e.getMessage());
        }
        boolean drawerLand = profileDrawerNow();
        Allure.parameter("homeLandscape", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("drawerLandscape", String.valueOf(drawerLand));
        home.attachScreenshot("rental-home-rhe2-landscape");
        try {
            if (android.getOrientation() != ScreenOrientation.PORTRAIT) {
                android.rotate(ScreenOrientation.PORTRAIT);
            }
        } catch (RuntimeException ignored) {
            // restore
        }
        boolean drawerPort = profileDrawerNow();
        Allure.parameter("homePortrait", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("drawerPortrait", String.valueOf(drawerPort));
        home.attachScreenshot("rental-home-rhe2-portrait");
        if (drawerLand || drawerPort) {
            Allure.parameter("bug", "26");
        }
        assertThat(drawerLand || drawerPort)
                .as("BUGS_FOUND #26: rotating Rental Home opens the Account drawer and hides "
                        + "Current Earning. Do not fold into #18 (material Home rotation).")
                .isFalse();
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow() || home.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 3, description = "RH-E3: background then foreground")
    @Severity(SeverityLevel.CRITICAL)
    public void backgroundThenForeground() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        android.runAppInBackground(Duration.ofSeconds(3));
        waitForLoggedInLanding(new QuickBookingPage(), home, android, Config.get("app.package"));
        Allure.parameter("after", classifyRentalNow());
        home.attachScreenshot("rental-home-rhe3-bg-fg");
        assertThat(android.getCurrentPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow() || new QuickBookingPage().isDisplayedNow()
                || home.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 4, description = "RH-E4: offline dwell does not crash")
    @Severity(SeverityLevel.NORMAL)
    public void offlineDoesNotCrash() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        Adb.disableRadios();
        try {
            try {
                Waits.until(DriverManager.get(), d -> null, "offline dwell", Duration.ofSeconds(3));
            } catch (TimeoutException ignored) {
                // dwell
            }
            String src = DriverManager.get().getPageSource().toLowerCase();
            boolean offlineCopy = src.contains("no internet") || src.contains("offline")
                    || src.contains("retry") || src.contains("check your connection");
            Allure.parameter("offlineCopy", String.valueOf(offlineCopy));
            Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
            home.attachScreenshot("rental-home-rhe4-offline");
            assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
            assertThat(home.isDisplayedNow() || offlineCopy || home.isRentalBottomTabsVisible()).isTrue();
        } finally {
            Adb.ensureNetworkReady();
        }
    }

    @Test(priority = 5, description = "RH-E5: force-stop + relaunch without pm clear")
    @Severity(SeverityLevel.CRITICAL)
    public void relaunchWithoutClear() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.terminateApp(pkg);
        android.activateApp(pkg);
        waitForLoggedInLanding(new QuickBookingPage(), home, android, pkg);
        Allure.parameter("after", classifyRentalNow());
        home.attachScreenshot("rental-home-rhe5-relaunch");
        assertThat(android.getCurrentPackage()).isEqualTo(pkg);
        assertThat(home.isDisplayedNow() || new QuickBookingPage().isDisplayedNow())
                .as("Relaunch must be Home or QB intercept, not Sign Up")
                .isTrue();
    }

    @Test(priority = 6, description = "RH-E6: outside tap with drawer closed stays Home")
    @Severity(SeverityLevel.NORMAL)
    public void outsideTapStaysHome() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        org.openqa.selenium.Dimension size = DriverManager.get().manage().window().getSize();
        DriverManager.get().executeScript("mobile: clickGesture", java.util.Map.of(
                "x", size.width / 2,
                "y", (int) (size.height * 0.12)));
        Allure.parameter("after", classifyRentalNow());
        Allure.parameter("drawer", String.valueOf(profileDrawerNow()));
        home.attachScreenshot("rental-home-rhe6-outside");
        assertThat(home.isDisplayedNow()).isTrue();
        assertThat(profileDrawerNow()).isFalse();
    }

    @Test(priority = 7, description = "RH-E7: observe Timer toward 00:00 — no Accept")
    @Severity(SeverityLevel.CRITICAL)
    @Description("If timer is already <= 00:05, wait for 00:00 or disappearance. If timer is "
            + "minutes away, record live value and skip long wait. Never tap Accept at 00:00.")
    public void timerExpiryObserved() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        java.util.List<String> timers = home.bookingOrderTimersNow();
        Allure.parameter("timers", String.join(",", timers));
        if (timers.isEmpty()) {
            home.attachScreenshot("rental-home-rhe7-no-timer");
            throw new SkipException("No Booking Orders timer on Home this run.");
        }
        int seconds = toSeconds(timers.get(0));
        Allure.parameter("firstTimerSeconds", String.valueOf(seconds));
        if (seconds > 20) {
            home.attachScreenshot("rental-home-rhe7-timer-long");
            throw new SkipException(
                    "Timer is " + timers.get(0) + " — not waiting out expiry this slice. "
                            + "Retest when a card is under 20s. Did not tap Accept/Decline.");
        }
        try {
            Waits.until(DriverManager.get(),
                    d -> {
                        java.util.List<String> now = home.bookingOrderTimersNow();
                        if (now.isEmpty() || now.contains("00:00")) {
                            return Boolean.TRUE;
                        }
                        return null;
                    },
                    "Timer did not reach 00:00 or drop",
                    Duration.ofSeconds(25));
        } catch (TimeoutException e) {
            Allure.parameter("expiry", "still-ticking");
        }
        java.util.List<String> after = home.bookingOrderTimersNow();
        Allure.parameter("after", String.join(",", after));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        home.attachScreenshot("rental-home-rhe7-expiry");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 8, description = "RH-E8: spam Monthly before reload finishes")
    @Severity(SeverityLevel.NORMAL)
    public void spamMonthlyFilter() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        for (int i = 0; i < 4; i++) {
            try {
                home.tapDesc("Select period");
            } catch (RuntimeException e) {
                Allure.parameter("tap" + i, e.getClass().getSimpleName());
            }
        }
        Allure.parameter("after", classifyRentalNow());
        Allure.parameter("package", String.valueOf(vendorPackage()));
        home.attachScreenshot("rental-home-rhe8-spam-monthly");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow() || home.isPeriodFilterVisible()
                || home.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 9, description = "RH-E9: rapid Home / Earning / Home tab switches")
    @Severity(SeverityLevel.NORMAL)
    public void rapidTabSwitch() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc("Earning");
        home.tapDesc("Home");
        home.tapDesc("Earning");
        home.tapDesc("Home");
        Allure.parameter("after", classifyRentalNow());
        home.attachScreenshot("rental-home-rhe9-rapid-tabs");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow() || home.isRentalBottomTabsVisible()).isTrue();
    }

    @Test(priority = 10, description = "RH-E10: See all while Upcoming strip is swiping")
    @Severity(SeverityLevel.NORMAL)
    public void seeAllDuringUpcomingSwipe() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.swipeRentalUpcomingStrip("left");
        int seeAll = home.seeAllCount();
        Allure.parameter("seeAllAfterSwipe", String.valueOf(seeAll));
        if (seeAll >= 2) {
            home.tapNthSeeAll(1);
        } else if (seeAll == 1) {
            home.tapNthSeeAll(0);
            Allure.parameter("seeAllNote", "only one See all after swipe — tapped index 0");
        }
        String named = classifyRentalNow();
        Allure.parameter("after", named);
        Allure.parameter("launcher", String.valueOf(launcherNow()));
        home.attachScreenshot("rental-home-rhe10-see-all-during-swipe");
        assertThat(launcherNow())
                .as("BUGS_FOUND #25: See all during Upcoming swipe must not exit to launcher")
                .isFalse();
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
    }

    @Test(priority = 11, description = "RH-E11: airplane mode while timer is ticking")
    @Severity(SeverityLevel.NORMAL)
    public void offlineWhileTimerTicks() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        java.util.List<String> before = home.bookingOrderTimersNow();
        Adb.disableRadios();
        try {
            try {
                Waits.until(DriverManager.get(),
                        d -> !home.bookingOrderTimersNow().equals(before) ? Boolean.TRUE : null,
                        "timer dwell offline",
                        Duration.ofSeconds(8));
            } catch (TimeoutException ignored) {
                // timer may freeze offline — document
            }
            Allure.parameter("before", String.join(",", before));
            Allure.parameter("after", String.join(",", home.bookingOrderTimersNow()));
            Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
            home.attachScreenshot("rental-home-rhe11-timer-offline");
            assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        } finally {
            Adb.ensureNetworkReady();
        }
    }

    @Test(priority = 12, description = "RH-E12: Accept/Decline multi-tap is blocked this phase")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Standing rule: do not consume 0001 queue. Script exists as a skip, not a tap.")
    public void acceptDeclineMultiTapBlocked() {
        reachUsableRentalHomeOrFailExtendTime();
        throw new SkipException(
                "Not executed: rapid Accept/Decline on 9000000001 would consume the queue. "
                        + "Needs a separate go-ahead. Home still shows Accept/Decline — do not tap.");
    }

    private static int toSeconds(String mmss) {
        String[] p = mmss.split(":");
        return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
    }
}
