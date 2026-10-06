package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Log Out interact — Confirm + session probes. Restore OTP after Confirm.
 *
 * <p><b>Happy Path (LO-I1–I10)</b>
 */
@Epic("Vendor app")
@Feature("Log Out interact — rental 9000000001")
public class LogoutInteractTest extends LogoutBaseTest {

    @Test(priority = 1, description = "LO-I1: Confirm Log out")
    @Severity(SeverityLevel.BLOCKER)
    public void confirmLogOut() {
        confirmLogoutFromDrawer();
        attachSessionParams("i1");
        new ProfileDrawerPage().attachScreenshot("lo-i1");
        assertThat(vendorPackage()).contains("l2b");
        if (looksLoggedOutUi()) {
            restoreSession0001();
        }
    }

    @Test(priority = 2, description = "LO-I2: after Confirm — auth UI OR sticky session bug")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Documents sticky session if Home/drawer remains after Confirm.")
    public void afterConfirmSessionState() {
        confirmLogoutFromDrawer();
        boolean stillIn = looksStillLoggedIn();
        boolean outUi = looksLoggedOutUi();
        Allure.parameter("stillLoggedIn", String.valueOf(stillIn));
        Allure.parameter("loggedOutUi", String.valueOf(outUi));
        Allure.parameter("bug", stillIn ? "52-session-sticky" : "none");
        new ProfileDrawerPage().attachScreenshot("lo-i2");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(stillIn).as("session must clear").isFalse();
        softly.assertThat(outUi).as("auth UI required").isTrue();
        try {
            softly.assertAll();
        } finally {
            if (outUi) {
                restoreSession0001();
            }
        }
    }

    @Test(priority = 3, description = "LO-I3: force-stop relaunch after Confirm")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Cold relaunch must not restore prior vendor Home without login.")
    public void forceStopRelaunchAfterConfirm() {
        confirmLogoutFromDrawer();
        forceStopRelaunch();
        attachSessionParams("i3relaunch");
        new HomePage().attachScreenshot("lo-i3");
        boolean stillIn = looksStillLoggedIn();
        Allure.parameter("bugSessionSurvivesRelaunch", String.valueOf(stillIn));
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(stillIn)
                .as("force-stop relaunch must not keep Kasim/Home without login")
                .isFalse();
        softly.assertThat(looksLoggedOutUi() || hasText("Welcome to L2B")
                || hasText("Enter your mobile number") || hasText("Verify OTP"))
                .as("auth/onboarding after relaunch")
                .isTrue();
        try {
            softly.assertAll();
        } finally {
            restoreSession0001();
        }
    }

    @Test(priority = 4, description = "LO-I4: activateApp after Confirm")
    @Severity(SeverityLevel.CRITICAL)
    public void activateAfterConfirm() {
        confirmLogoutFromDrawer();
        try {
            ((io.appium.java_client.android.AndroidDriver) DriverManager.get())
                    .activateApp(com.l2b.vendor.environment.Config.get("app.package"));
        } catch (RuntimeException ignored) {
        }
        sleepQuiet(2000);
        attachSessionParams("i4");
        Allure.parameter("bugStickyActivate", String.valueOf(looksStillLoggedIn()));
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(looksStillLoggedIn()).as("no sticky after activate").isFalse();
        try {
            softly.assertAll();
        } finally {
            if (looksLoggedOutUi() || !looksStillLoggedIn()) {
                restoreSession0001();
            }
        }
    }

    @Test(priority = 5, description = "LO-I5: Cancel never clears session")
    @Severity(SeverityLevel.CRITICAL)
    public void cancelNeverClears() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(700);
        page.tapLogoutCancel();
        sleepQuiet(700);
        assertThat(looksStillLoggedIn() || page.isDisplayedNow()).isTrue();
        assertThat(looksLoggedOutUi()).isFalse();
    }

    @Test(priority = 6, description = "LO-I6: double Confirm tap safe")
    @Severity(SeverityLevel.NORMAL)
    public void doubleConfirmTap() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(700);
        page.tapLogoutConfirm();
        sleepQuiet(500);
        if (page.isLogoutDialogVisible() || hasText("Log out")) {
            try {
                page.tapLogoutConfirm();
            } catch (RuntimeException ignored) {
            }
        }
        sleepQuiet(1500);
        attachSessionParams("i6");
        assertThat(vendorPackage()).contains("l2b");
        if (looksLoggedOutUi()) {
            restoreSession0001();
        }
    }

    @Test(priority = 7, description = "LO-I7: Confirm then Profile must not open drawer without login")
    @Severity(SeverityLevel.CRITICAL)
    public void confirmThenProfileTap() {
        confirmLogoutFromDrawer();
        if (looksStillLoggedIn() || new HomePage().isDisplayedNow()) {
            try {
                new HomePage().tapDesc("Profile");
                sleepQuiet(900);
            } catch (RuntimeException ignored) {
            }
            Allure.parameter("drawerAfterSticky", String.valueOf(profileDrawerNow()));
            Allure.parameter("bug", "session-sticky-profile-still-works");
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(looksStillLoggedIn()).as("must be logged out").isFalse();
        try {
            softly.assertAll();
        } finally {
            if (looksLoggedOutUi() || !looksStillLoggedIn()) {
                restoreSession0001();
            }
        }
    }

    @Test(priority = 8, description = "LO-I8: re-login after Confirm reaches Home")
    @Severity(SeverityLevel.CRITICAL)
    public void reloginAfterConfirm() {
        confirmLogoutFromDrawer();
        HomePage home = restoreSession0001();
        assertThat(home.isDisplayedNow() || hasText("Current Earning")).isTrue();
        assertThat(looksLoggedOutUi()).isFalse();
    }

    @Test(priority = 9, description = "LO-I9: Kasim Pathan gone after Confirm (when logged out UX works)")
    @Severity(SeverityLevel.NORMAL)
    public void vendorNameGoneAfterConfirm() {
        confirmLogoutFromDrawer();
        attachSessionParams("i9");
        SoftAssertions softly = new SoftAssertions();
        if (looksLoggedOutUi()) {
            softly.assertThat(hasText("Kasim Pathan")).as("no vendor name").isFalse();
            softly.assertThat(hasText("Current Earning")).as("no earning").isFalse();
        } else {
            Allure.parameter("bugStickyShowsKasim", String.valueOf(hasText("Kasim Pathan")));
            softly.assertThat(looksStillLoggedIn()).as("sticky session").isFalse();
        }
        try {
            softly.assertAll();
        } finally {
            if (looksLoggedOutUi() || !looksStillLoggedIn()) {
                try {
                    restoreSession0001();
                } catch (RuntimeException ignored) {
                }
            }
        }
    }

    @Test(priority = 10, description = "LO-I10: stay Vendor package after Confirm")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendorPackage() {
        confirmLogoutFromDrawer();
        assertThat(vendorPackage()).contains("l2b");
        if (looksLoggedOutUi()) {
            restoreSession0001();
        }
    }
}
