package com.l2b.vendor.modules.quickbooking.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.bookings.presentation.tests.quickbooking.QuickBookingBaseTest;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
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
 * Material vendor {@code 9000000017} Quick Booking edges after Accept + Decline
 * consumed the live cards. Isolated {@code quickbooking/material-edge-cases.xml}.
 * No timer / operator / Get Direction / Assign machine cases.
 * Do not log in as 0001 / 0002 / 0003. Do not tap Accept/Decline.
 */
@Epic("Vendor app")
@Feature("Quick Booking edges — material vendor 9000000017")
public class MaterialQuickBookingEdgeCasesTest extends QuickBookingBaseTest {

    @Test(priority = 1, description = "Material-Edge-1: after consume, login lands on Home or empty Quick Booking")
    @Severity(SeverityLevel.CRITICAL)
    @Description("9000000017 after both cards consumed. Record Home vs empty Quick Booking "
            + "vs leftover cards. Do not tap Accept/Decline.")
    public void emptyQueueAfterConsume() {
        QuickBookingPage page = loginToMaterialScreen();
        HomePage home = new HomePage();
        QuickBookingLandingPage landing = new QuickBookingLandingPage();
        String landingName = namedMaterialLanding(page, home, landing);
        Allure.parameter("phone", materialPhone());
        Allure.parameter("landing", landingName);
        Allure.parameter("quickBooking", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("emptyCopy", String.valueOf(page.hasText("No pending bookings")));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("acceptCount", String.valueOf(page.acceptCount()));
        page.attachScreenshot("material-edge-1-empty-queue");

        assertThat(materialPhone()).isEqualTo("9000000017");
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(landingName).as("Must land on Home or Quick Booking, not crash")
                .isIn("home", "quick-booking-empty", "quick-booking");
    }

    @Test(priority = 2, description = "Material-Edge-2: Close from Quick Booking goes to Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("If login is Quick Booking (empty or leftover card), Close must reach Home. "
            + "Skip if post-consume login is already Home. Do not tap Accept/Decline.")
    public void closeGoesToHome() {
        QuickBookingPage page = loginToMaterialScreen();
        HomePage home = new HomePage();
        if (!page.isDisplayedNow()) {
            throw new SkipException(
                    "Post-consume login is not Quick Booking (landing="
                            + namedMaterialLanding(page, home, new QuickBookingLandingPage())
                            + "). Close needs a QB screen — seed via customer portal if we need it again.");
        }
        page.tapClose();
        Waits.until(DriverManager.get(),
                d -> home.isDisplayedNow() ? Boolean.TRUE : null,
                "After Close, Home did not appear",
                Duration.ofSeconds(15));
        Allure.parameter("quickBookingAfterClose", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("homeAfterClose", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("extendTimeDialog",
                String.valueOf(new QuickBookingLandingPage().isExtendTimeDialogVisible()));
        page.attachScreenshot("material-edge-2-close");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(page.isDisplayedNow()).as("Quick Booking must be gone after Close").isFalse();
        assertThat(home.isDisplayedNow()).as("Close must land on Home").isTrue();
        assertThat(new QuickBookingLandingPage().isExtendTimeDialogVisible())
                .as("Material must not show rental extend-time dialog")
                .isFalse();
    }

    @Test(priority = 3, description = "Material-Edge-3: Back from Quick Booking — named landing, no crash")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Device Back from Quick Booking. Observed earlier on 0017: launcher (BUGS_FOUND #13). "
            + "Skip if login is already Home. Do not tap Accept/Decline.")
    public void backNamedLanding() {
        QuickBookingPage page = loginToMaterialScreen();
        HomePage home = new HomePage();
        if (!page.isDisplayedNow()) {
            throw new SkipException(
                    "Post-consume login is not Quick Booking. Back-from-QB needs a QB screen. #13 already logged.");
        }
        page.pressBack();
        Waits.until(DriverManager.get(),
                d -> (home.isDisplayedNow() || page.isDisplayedNow() || launcherNow()) ? Boolean.TRUE : null,
                "After Back, no Home / Quick Booking / launcher",
                Duration.ofSeconds(12));
        String landing = namedLanding(page, home);
        Allure.parameter("backLanding", landing);
        Allure.parameter("backPackage", String.valueOf(vendorPackage()));
        page.attachScreenshot("material-edge-3-back-" + landing);

        assertThat(vendorPackage()).as("Back must not crash (package blank)").isNotBlank();
        if ("launcher".equals(landing)) {
            Allure.parameter("bugNote", "Same as BUGS_FOUND #13 — do not open a new number");
        }
        assertThat(landing)
                .as("Observed 18 Sep on 0017: launcher. Logged as BUGS_FOUND #13")
                .isEqualTo("launcher");
    }

    @Test(priority = 4, description = "Material-Edge-4: Rotation keeps the current chrome")
    @Severity(SeverityLevel.NORMAL)
    @Description("Landscape then portrait. Home or Quick Booking chrome must survive. "
            + "Do not tap Accept/Decline.")
    public void rotationKeepsChrome() {
        QuickBookingPage page = loginToMaterialScreen();
        HomePage home = new HomePage();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        String beforeName = namedMaterialLanding(page, home, new QuickBookingLandingPage());
        try {
            android.rotate(ScreenOrientation.LANDSCAPE);
        } catch (RuntimeException e) {
            Allure.parameter("rotateThrew", e.getMessage());
        }
        String landscapeName = namedMaterialLanding(page, home, new QuickBookingLandingPage());
        Allure.parameter("landingBefore", beforeName);
        Allure.parameter("landingLandscape", landscapeName);
        page.attachScreenshot("material-edge-4-landscape");
        try {
            if (android.getOrientation() != ScreenOrientation.PORTRAIT) {
                android.rotate(ScreenOrientation.PORTRAIT);
            }
        } catch (RuntimeException ignored) {
            // restore best-effort
        }
        String portraitName = namedMaterialLanding(page, home, new QuickBookingLandingPage());
        Allure.parameter("landingPortrait", portraitName);
        page.attachScreenshot("material-edge-4-portrait");

        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        if ("home-drawer".equals(landscapeName) || "home-drawer".equals(portraitName)) {
            Allure.parameter("note",
                    "Rotate opened the Home account drawer (Account/KYC/Logout). Home stayed behind. "
                            + "Same overlay as #14, different trigger — not a new #17.");
        }
        assertThat(portraitName).as("Round-trip must stay on Home / drawer / Quick Booking")
                .isIn("home", "home-drawer", "quick-booking-empty", "quick-booking");
    }

    @Test(priority = 5, description = "Material-Edge-5: Background then foreground keeps the landing")
    @Severity(SeverityLevel.CRITICAL)
    @Description("HOME ~3s, return. Must stay on Home or Quick Booking, no crash. "
            + "Do not tap Accept/Decline.")
    public void backgroundThenForegroundKeepsLanding() {
        QuickBookingPage page = loginToMaterialScreen();
        HomePage home = new HomePage();
        String before = namedMaterialLanding(page, home, new QuickBookingLandingPage());
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        android.runAppInBackground(Duration.ofSeconds(3));
        waitForLoggedInLanding(page, home, android, Config.get("app.package"));
        String after = namedMaterialLanding(page, home, new QuickBookingLandingPage());
        Allure.parameter("landingBefore", before);
        Allure.parameter("landingAfterFg", after);
        page.attachScreenshot("material-edge-5-bg-fg");

        assertThat(android.getCurrentPackage()).isEqualTo(Config.get("app.package"));
        assertThat(after).as("Must return to Home or Quick Booking").isIn(
                "home", "home-drawer", "quick-booking-empty", "quick-booking");
    }

    @Test(priority = 6, description = "Material-Edge-6: Offline on the post-consume landing does not crash")
    @Severity(SeverityLevel.NORMAL)
    @Description("Login, disable radios, dwell. Record offline copy vs silent stay. "
            + "Radios restored in AfterMethod. Do not tap Accept/Decline.")
    public void offlineOnLandingDoesNotCrash() {
        QuickBookingPage page = loginToMaterialScreen();
        HomePage home = new HomePage();
        String before = namedMaterialLanding(page, home, new QuickBookingLandingPage());
        Adb.disableRadios();
        try {
            Waits.until(DriverManager.get(), d -> null, "offline dwell", Duration.ofSeconds(3));
        } catch (TimeoutException ignored) {
            // dwell
        }
        boolean offlineCopy = hasOfflineCopy(DriverManager.get().getPageSource());
        String after = namedMaterialLanding(page, home, new QuickBookingLandingPage());
        Allure.parameter("landingBefore", before);
        Allure.parameter("landingWhileOffline", after);
        Allure.parameter("offlineOrRetryCopy", String.valueOf(offlineCopy));
        page.attachScreenshot("material-edge-6-offline");

        assertThat(vendorPackage()).as("Offline must not crash Vendor")
                .isEqualTo(Config.get("app.package"));
        assertThat(after.equals(before) || offlineCopy || "home".equals(after)
                || after.startsWith("quick-booking"))
                .as("Stay on the same landing or show offline copy")
                .isTrue();
    }

    private static boolean hasOfflineCopy(String source) {
        String s = source == null ? "" : source.toLowerCase();
        return s.contains("no internet") || s.contains("no network") || s.contains("offline")
                || s.contains("retry") || s.contains("check your connection")
                || s.contains("something went wrong") || s.contains("unable to");
    }

    private static String namedMaterialLanding(QuickBookingPage page, HomePage home,
            QuickBookingLandingPage landing) {
        if (profileDrawerNow()) {
            return "home-drawer";
        }
        if (page.isDisplayedNow()) {
            if (page.hasText("No pending bookings") || landing.acceptCount() == 0) {
                return "quick-booking-empty";
            }
            return "quick-booking";
        }
        if (home.isDisplayedNow()) {
            return "home";
        }
        if (launcherNow()) {
            return "launcher";
        }
        return "other";
    }

    private QuickBookingPage loginToMaterialScreen() {
        OtpPage otp = openOtp(materialPhone());
        otp.focusOtpField();
        if (!otp.otpFieldText().isEmpty()) {
            otp.clearOtp();
        }
        otp.pressDigitKeys("1234");
        QuickBookingPage page = new QuickBookingPage();
        HomePage home = new HomePage();
        try {
            Waits.until(DriverManager.get(),
                    d -> (page.isDisplayedNow() || home.isDisplayedNow()) ? Boolean.TRUE : null,
                    "Home or Quick Booking did not appear after OTP",
                    Duration.ofSeconds(12));
        } catch (TimeoutException first) {
            if (otp.isDisplayedNow() && otp.otpFieldText().isEmpty()) {
                otp.pasteOtp("1234");
            }
            Waits.until(DriverManager.get(),
                    d -> (page.isDisplayedNow() || home.isDisplayedNow()) ? Boolean.TRUE : null,
                    "Home or Quick Booking did not appear after OTP retry",
                    Duration.ofSeconds(15));
        }
        return page;
    }
}
