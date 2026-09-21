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
import org.testng.annotations.Test;

/**
 * Home edges for {@code 9000000017}. Isolated {@code home/home-edge.xml}.
 * New Home bugs get a new sequential number — do not fold into #13/#14/#15.
 */
@Epic("Vendor app")
@Feature("Home edges — material vendor 9000000017")
public class HomeEdgeTest extends HomeBaseTest {

    @Test(priority = 1, description = "H-E1: device Back on Home — named landing")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Back from Home. Stay vs launcher. If launcher, new Home bug — not #13.")
    public void backOnHome() {
        HomePage home = loginToHomeOrQueue();
        ((AndroidDriver) DriverManager.get()).pressKey(new KeyEvent(AndroidKey.BACK));
        String pkg = vendorPackage();
        boolean homeStill = home.isDisplayedNow();
        boolean launcher = pkg != null && pkg.contains("launcher");
        Allure.parameter("package", String.valueOf(pkg));
        Allure.parameter("home", String.valueOf(homeStill));
        Allure.parameter("launcher", String.valueOf(launcher));
        System.out.println("H-E1 after Back pkg=" + pkg + " home=" + homeStill + " launcher=" + launcher);
        home.attachScreenshot("home-he1-back");
        assertThat(pkg).as("Back must not crash (blank package)").isNotBlank();
        if (launcher) {
            Allure.parameter("bugCandidate", "NEW Home Back-to-launcher — do not fold into #13");
        }
        assertThat(homeStill || launcher || "com.l2b.app.qa".equals(pkg))
                .as("Stay on Vendor or named launcher")
                .isTrue();
    }

    @Test(priority = 2, description = "H-E2: rotation landscape then portrait")
    @Severity(SeverityLevel.NORMAL)
    @Description("If drawer opens, new Home-screen bug number — do not fold into #14.")
    public void rotationKeepsHome() {
        HomePage home = loginToHomeOrQueue();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        try {
            android.rotate(ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            Allure.parameter("rotateThrew", e.getMessage());
        }
        boolean drawerLand = profileDrawerNow();
        Allure.parameter("homeLandscape", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("drawerLandscape", String.valueOf(drawerLand));
        System.out.println("H-E2 landscape home=" + home.isDisplayedNow() + " drawer=" + drawerLand);
        home.attachScreenshot("home-he2-landscape");
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
        System.out.println("H-E2 portrait home=" + home.isDisplayedNow() + " drawer=" + drawerPort);
        home.attachScreenshot("home-he2-portrait");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        if (drawerLand || drawerPort) {
            Allure.parameter("bugCandidate", "NEW Home rotation-opens-drawer — do not fold into #14");
        }
        assertThat(home.isDisplayedNow() || drawerPort || home.isMaterialBottomTabsVisible()).isTrue();
    }

    @Test(priority = 3, description = "H-E3: background then foreground keeps Home")
    @Severity(SeverityLevel.CRITICAL)
    public void backgroundThenForeground() {
        HomePage home = loginToHomeOrQueue();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        android.runAppInBackground(Duration.ofSeconds(3));
        waitForLoggedInLanding(new QuickBookingPage(), home, android, Config.get("app.package"));
        Allure.parameter("homeAfter", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("package", android.getCurrentPackage());
        System.out.println("H-E3 bg/fg pkg=" + android.getCurrentPackage() + " home=" + home.isDisplayedNow());
        home.attachScreenshot("home-he3-bg-fg");
        assertThat(android.getCurrentPackage()).isEqualTo(Config.get("app.package"));
        assertThat(home.isDisplayedNow() || home.isMaterialBottomTabsVisible()).isTrue();
    }

    @Test(priority = 4, description = "H-E4: offline dwell on Home does not crash")
    @Severity(SeverityLevel.NORMAL)
    public void offlineDoesNotCrash() {
        HomePage home = loginToHomeOrQueue();
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
            System.out.println("H-E4 offlineCopy=" + offlineCopy + " home=" + home.isDisplayedNow()
                    + " pkg=" + vendorPackage());
            home.attachScreenshot("home-he4-offline");
            assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
            assertThat(home.isDisplayedNow() || offlineCopy || home.isMaterialBottomTabsVisible()).isTrue();
        } finally {
            Adb.ensureNetworkReady();
        }
    }

    @Test(priority = 5, description = "H-E5: force-stop + relaunch without pm clear stays Home")
    @Severity(SeverityLevel.CRITICAL)
    public void relaunchWithoutClearStaysHome() {
        HomePage home = loginToHomeOrQueue();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.terminateApp(pkg);
        android.activateApp(pkg);
        waitForLoggedInLanding(new QuickBookingPage(), home, android, pkg);
        Allure.parameter("homeAfter", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("qbAfter", String.valueOf(new QuickBookingPage().isDisplayedNow()));
        System.out.println("H-E5 relaunch home=" + home.isDisplayedNow()
                + " qb=" + new QuickBookingPage().isDisplayedNow()
                + " pkg=" + android.getCurrentPackage());
        home.attachScreenshot("home-he5-relaunch");
        assertThat(android.getCurrentPackage()).isEqualTo(pkg);
        assertThat(home.isDisplayedNow())
                .as("Empty QB queue must reopen Home, not first-launch")
                .isTrue();
    }

    @Test(priority = 6, description = "H-E6: re-tap Home tab stays on Home, drawer closed")
    @Severity(SeverityLevel.NORMAL)
    public void retapHomeTab() {
        HomePage home = loginToHomeOrQueue();
        home.tapDesc("Home");
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("drawer", String.valueOf(profileDrawerNow()));
        Allure.parameter("earning", String.valueOf(home.isCurrentEarningVisible()));
        System.out.println("H-E6 retap home=" + home.isDisplayedNow()
                + " drawer=" + profileDrawerNow()
                + " earning=" + home.isCurrentEarningVisible());
        home.attachScreenshot("home-he6-retap-home");
        assertThat(home.isDisplayedNow()).isTrue();
        assertThat(home.isCurrentEarningVisible()).isTrue();
        assertThat(profileDrawerNow()).as("Re-tap Home must not open drawer").isFalse();
    }
}
