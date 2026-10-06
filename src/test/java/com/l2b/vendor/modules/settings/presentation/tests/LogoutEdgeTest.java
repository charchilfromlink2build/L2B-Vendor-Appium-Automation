package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Log Out edge — session sticky, relaunch, airplane, double-confirm.
 *
 * <p><b>Critical Edge (LO-E1–E12)</b>
 */
@Epic("Vendor app")
@Feature("Log Out edge — rental 9000000001")
public class LogoutEdgeTest extends LogoutBaseTest {

    @Test(priority = 1, description = "LO-E1: Confirm then force-stop — session must die")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Primary sticky-session probe (manual report).")
    public void confirmForceStopSessionDies() {
        confirmLogoutFromDrawer();
        boolean immediateSticky = looksStillLoggedIn();
        forceStopRelaunch();
        boolean relaunchSticky = looksStillLoggedIn();
        Allure.parameter("immediateSticky", String.valueOf(immediateSticky));
        Allure.parameter("relaunchSticky", String.valueOf(relaunchSticky));
        Allure.parameter("bug", (immediateSticky || relaunchSticky) ? "52" : "none");
        new HomePage().attachScreenshot("lo-e1");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(immediateSticky).as("immediate after Confirm").isFalse();
        softly.assertThat(relaunchSticky).as("after force-stop relaunch").isFalse();
        try {
            softly.assertAll();
        } finally {
            restoreSession0001();
        }
    }

    @Test(priority = 2, description = "LO-E2: Confirm → background → foreground")
    @Severity(SeverityLevel.CRITICAL)
    public void confirmBackgroundForeground() {
        confirmLogoutFromDrawer();
        try {
            ((AndroidDriver) DriverManager.get()).runAppInBackground(java.time.Duration.ofSeconds(3));
        } catch (RuntimeException e) {
            Adb.run("shell", "input", "keyevent", "3");
            sleepQuiet(2000);
            ((AndroidDriver) DriverManager.get()).activateApp(Config.get("app.package"));
        }
        sleepQuiet(1500);
        attachSessionParams("e2");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(looksStillLoggedIn()).as("no sticky after bg/fg").isFalse();
        try {
            softly.assertAll();
        } finally {
            restoreSession0001();
        }
    }

    @Test(priority = 3, description = "LO-E3: device Back on dialog does not Confirm")
    @Severity(SeverityLevel.CRITICAL)
    public void backOnDialogDoesNotConfirm() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(700);
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        assertThat(looksStillLoggedIn() || page.isDisplayedNow() || profileDrawerNow()).isTrue();
        assertThat(looksLoggedOutUi()).isFalse();
    }

    @Test(priority = 4, description = "LO-E4: rapid Cancel/Confirm race — final Confirm wins or Cancel")
    @Severity(SeverityLevel.NORMAL)
    public void rapidCancelConfirm() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(500);
        try {
            page.tapLogoutCancel();
        } catch (RuntimeException ignored) {
        }
        sleepQuiet(300);
        if (!page.isLogoutDialogVisible()) {
            page.tapLogOut();
            sleepQuiet(600);
        }
        page.tapLogoutConfirm();
        sleepQuiet(2000);
        attachSessionParams("e4");
        if (looksLoggedOutUi()) {
            restoreSession0001();
        }
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 5, description = "LO-E5: Confirm twice in a row after re-login")
    @Severity(SeverityLevel.CRITICAL)
    public void confirmTwiceAcrossSessions() {
        confirmLogoutFromDrawer();
        restoreSession0001();
        confirmLogoutFromDrawer();
        attachSessionParams("e5");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(looksStillLoggedIn()).as("second Confirm clears").isFalse();
        try {
            softly.assertAll();
        } finally {
            restoreSession0001();
        }
    }

    @Test(priority = 6, description = "LO-E6: no Accept/Decline on logout path")
    @Severity(SeverityLevel.CRITICAL)
    public void noAcceptDecline() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(700);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("Decline")).as("no Decline").isFalse();
        softly.assertThat(DriverManager.get().findElements(
                org.openqa.selenium.By.xpath("//*[@text='Accept']")).isEmpty()).as("no Accept").isTrue();
        softly.assertAll();
        page.tapLogoutCancel();
    }

    @Test(priority = 7, description = "LO-E7: Confirm then am start activity")
    @Severity(SeverityLevel.CRITICAL)
    public void confirmThenAmStart() {
        confirmLogoutFromDrawer();
        Adb.run("shell", "am", "start", "-n",
                Config.get("app.package") + "/" + Config.get("app.activity"));
        sleepQuiet(2500);
        dismissGotItIfPresent();
        attachSessionParams("e7");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(looksStillLoggedIn()).as("am start must not restore Home").isFalse();
        try {
            softly.assertAll();
        } finally {
            restoreSession0001();
        }
    }

    @Test(priority = 8, description = "LO-E8: drawer rows gone after successful logout UX")
    @Severity(SeverityLevel.NORMAL)
    public void drawerGoneAfterLogout() {
        confirmLogoutFromDrawer();
        SoftAssertions softly = new SoftAssertions();
        if (looksLoggedOutUi()) {
            softly.assertThat(profileDrawerNow()).as("no drawer").isFalse();
            softly.assertThat(hasText("Manage team")).as("no Manage team").isFalse();
        } else {
            Allure.parameter("bugStickyDrawer", String.valueOf(profileDrawerNow() || hasText("Account")));
            softly.assertThat(looksStillLoggedIn()).isFalse();
        }
        try {
            softly.assertAll();
        } finally {
            if (looksLoggedOutUi() || !looksStillLoggedIn()) {
                restoreSession0001();
            }
        }
    }

    @Test(priority = 9, description = "LO-E9: Confirm offline (airplane) still clears or errors clearly")
    @Severity(SeverityLevel.CRITICAL)
    public void confirmOffline() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(600);
        try {
            Adb.run("shell", "cmd", "connectivity", "airplane-mode", "enable");
            Adb.run("shell", "settings", "put", "global", "airplane_mode_on", "1");
            Adb.run("shell", "am", "broadcast", "-a", "android.intent.action.AIRPLANE_MODE",
                    "--ez", "state", "true");
            sleepQuiet(1000);
            page.tapLogoutConfirm();
            sleepQuiet(2500);
            attachSessionParams("e9offline");
            Allure.parameter("stillLoggedInOffline", String.valueOf(looksStillLoggedIn()));
            // Prefer local clear even offline; sticky offline is also a bug.
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(looksStillLoggedIn())
                    .as("offline Confirm must not keep Home as if logged in")
                    .isFalse();
            softly.assertAll();
        } finally {
            try {
                Adb.run("shell", "cmd", "connectivity", "airplane-mode", "disable");
                Adb.run("shell", "settings", "put", "global", "airplane_mode_on", "0");
                Adb.run("shell", "am", "broadcast", "-a", "android.intent.action.AIRPLANE_MODE",
                        "--ez", "state", "false");
            } catch (RuntimeException ignored) {
            }
            Adb.ensureNetworkReady();
            sleepQuiet(1500);
            if (looksLoggedOutUi() || !looksStillLoggedIn()) {
                try {
                    restoreSession0001();
                } catch (RuntimeException e) {
                    Allure.parameter("restoreAfterOffline", e.getClass().getSimpleName());
                }
            }
        }
    }

    @Test(priority = 10, description = "LO-E10: kill app WITHOUT Confirm keeps session (control)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Control: force-stop while logged in should keep session — contrast with Confirm.")
    public void killWithoutConfirmKeepsSession() {
        ensureRentalHomeWarm();
        assertThat(looksStillLoggedIn()).isTrue();
        forceStopRelaunch();
        attachSessionParams("e10control");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(looksStillLoggedIn() || hasText("Current Earning") || hasText("Kasim Pathan"))
                .as("kill without logout should keep session")
                .isTrue();
        softly.assertAll();
    }

    @Test(priority = 11, description = "LO-E10b: if Confirm sticky, Account still opens (bug probe)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Only fails when Confirm leaves Home and Account still opens.")
    public void stickySessionCanOpenAccount() {
        confirmLogoutFromDrawer();
        boolean sticky = looksStillLoggedIn();
        Allure.parameter("sticky", String.valueOf(sticky));
        if (!sticky) {
            Allure.parameter("sessionCleared", "true");
            Allure.parameter("bug", "none");
            restoreSession0001();
            return;
        }
        try {
            if (!profileDrawerNow()) {
                new HomePage().tapDesc("Profile");
                sleepQuiet(900);
            }
            if (profileDrawerNow() || hasText("Account")) {
                new ProfileDrawerPage().tapRow("Account");
                sleepQuiet(1200);
            }
            boolean accountOpen = hasText("Profile Info") || hasText("Full Name");
            Allure.parameter("accountStillOpens", String.valueOf(accountOpen));
            Allure.parameter("bug", "52");
            new ProfileDrawerPage().attachScreenshot("lo-e10-sticky-account");
            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(accountOpen)
                    .as("sticky session opens Account — logout broken")
                    .isFalse();
            softly.assertAll();
        } finally {
            try {
                DriverManager.get().navigate().back();
            } catch (RuntimeException ignored) {
            }
        }
    }

    @Test(priority = 12, description = "LO-E12: Confirm does not drop to launcher")
    @Severity(SeverityLevel.NORMAL)
    public void confirmNotLauncher() {
        confirmLogoutFromDrawer();
        String named = classifyRentalNow();
        Allure.parameter("after", named);
        assertThat(named).isNotEqualTo("launcher");
        assertThat(vendorPackage()).contains("l2b");
        if (looksLoggedOutUi()) {
            restoreSession0001();
        }
    }

    @Test(priority = 13, description = "LO-E13: restore OTP after Confirm reaches rental Home")
    @Severity(SeverityLevel.CRITICAL)
    public void restoreAfterConfirm() {
        confirmLogoutFromDrawer();
        HomePage home = restoreSession0001();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(home.isDisplayedNow() || hasText("Current Earning")).as("Home").isTrue();
        softly.assertThat(looksLoggedOutUi()).as("not auth").isFalse();
        softly.assertAll();
    }

    @Test(priority = 14, description = "LO-E14: Confirm → Home key → cold activate — still logged out")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Sticky-session probe: leave app via Home then reopen without Confirm path.")
    public void confirmHomeKeyRelaunch() {
        confirmLogoutFromDrawer();
        Adb.run("shell", "input", "keyevent", "3");
        sleepQuiet(1500);
        try {
            ((AndroidDriver) DriverManager.get()).activateApp(Config.get("app.package"));
        } catch (RuntimeException e) {
            Adb.run("shell", "am", "start", "-n",
                    Config.get("app.package") + "/" + Config.get("app.activity"));
        }
        sleepQuiet(2500);
        dismissGotItIfPresent();
        attachSessionParams("e14");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(looksStillLoggedIn()).as("no sticky after Home+activate").isFalse();
        softly.assertThat(looksLoggedOutUi()).as("auth UI after Home+activate").isTrue();
        try {
            softly.assertAll();
        } finally {
            restoreSession0001();
        }
    }

    @Test(priority = 15, description = "LO-E15: Confirm → wait 20s — session must stay cleared")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Token refresh / delayed restore race after Confirm.")
    public void confirmWaitStillLoggedOut() {
        confirmLogoutFromDrawer();
        sleepQuiet(20_000);
        attachSessionParams("e15wait");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(looksStillLoggedIn()).as("no delayed sticky after 20s").isFalse();
        softly.assertThat(looksLoggedOutUi()).as("still auth UI after 20s").isTrue();
        try {
            softly.assertAll();
        } finally {
            restoreSession0001();
        }
    }

    @Test(priority = 16, description = "LO-E16: Cancel then force-stop — session MUST remain (control)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Control: Cancel is not logout. Explains false sticky reports if user Cancelled.")
    public void cancelThenKillKeepsSession() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(700);
        page.tapLogoutCancel();
        sleepQuiet(700);
        assertThat(looksStillLoggedIn() || page.isDisplayedNow()).isTrue();
        forceStopRelaunch();
        attachSessionParams("e16cancelKill");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(looksStillLoggedIn() || hasText("Current Earning") || hasText("Kasim Pathan"))
                .as("Cancel + kill must keep session")
                .isTrue();
        softly.assertAll();
    }

    @Test(priority = 17, description = "LO-E17: Confirm → process kill (am kill) — still logged out")
    @Severity(SeverityLevel.CRITICAL)
    public void confirmAmKillStillLoggedOut() {
        confirmLogoutFromDrawer();
        String pkg = Config.get("app.package");
        try {
            Adb.run("shell", "am", "kill", pkg);
        } catch (RuntimeException e) {
            Adb.forceStop(pkg);
        }
        sleepQuiet(1000);
        try {
            ((AndroidDriver) DriverManager.get()).activateApp(pkg);
        } catch (RuntimeException e) {
            Adb.run("shell", "am", "start", "-n", pkg + "/" + Config.get("app.activity"));
        }
        sleepQuiet(2500);
        dismissGotItIfPresent();
        attachSessionParams("e17amKill");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(looksStillLoggedIn()).as("no sticky after am kill").isFalse();
        try {
            softly.assertAll();
        } finally {
            restoreSession0001();
        }
    }

    @Test(priority = 18, description = "LO-E18: Confirm clears Kasim/drawer AND server-side must require OTP")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Deep sticky: after Confirm, Profile/Account chrome must be gone; re-entry needs OTP.")
    public void confirmClearsVendorChromeRequiresOtp() {
        confirmLogoutFromDrawer();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("Kasim Pathan")).as("no vendor name").isFalse();
        softly.assertThat(hasText("Manage team")).as("no Manage team").isFalse();
        softly.assertThat(hasText("Current Earning")).as("no Current Earning").isFalse();
        softly.assertThat(looksLoggedOutUi()).as("auth UI").isTrue();
        forceStopRelaunch();
        softly.assertThat(looksStillLoggedIn()).as("relaunch still out").isFalse();
        softly.assertThat(looksLoggedOutUi() || hasText("Welcome to L2B") || hasText("Get OTP"))
                .as("auth/onboarding after relaunch")
                .isTrue();
        Allure.parameter("bug", looksStillLoggedIn() ? "52" : "none");
        try {
            softly.assertAll();
        } finally {
            restoreSession0001();
        }
    }

    @Test(priority = 19, description = "LO-E19: Confirm then device Back must NOT restore active Home")
    @Severity(SeverityLevel.BLOCKER)
    @Description("BUGS_FOUND #53 — user report: after Log out Confirm, Back returns to prior logged-in session.")
    public void confirmThenBackMustNotRestoreSession() {
        confirmLogoutFromDrawer();
        boolean afterConfirmIn = looksStillLoggedIn();
        boolean afterConfirmOut = looksLoggedOutUi();
        Allure.parameter("afterConfirmLoggedIn", String.valueOf(afterConfirmIn));
        Allure.parameter("afterConfirmLoggedOutUi", String.valueOf(afterConfirmOut));

        DriverManager.get().navigate().back();
        sleepQuiet(1500);
        boolean sticky = looksStillLoggedIn();
        boolean outUi = looksLoggedOutUi();
        String named = classifyRentalNow();
        Allure.parameter("afterBackNamed", named);
        Allure.parameter("afterBackLoggedIn", String.valueOf(sticky));
        Allure.parameter("afterBackLoggedOutUi", String.valueOf(outUi));
        Allure.parameter("bug", sticky ? "53" : "none");
        new ProfileDrawerPage().attachScreenshot("lo-e19-confirm-back");

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(sticky)
                .as("BUGS_FOUND #53: Back after Confirm must NOT restore Kasim/Home/drawer session")
                .isFalse();
        softly.assertThat(outUi || named.equals("launcher") || hasText("Welcome to L2B")
                        || hasText("Get OTP") || hasText("Sign up"))
                .as("must stay on auth/onboarding or leave Vendor — not prior Home")
                .isTrue();
        try {
            softly.assertAll();
        } finally {
            if (looksLoggedOutUi() || !looksStillLoggedIn()) {
                try {
                    restoreSession0001();
                } catch (RuntimeException e) {
                    Allure.parameter("restoreFail", e.getClass().getSimpleName());
                }
            }
        }
    }

    @Test(priority = 20, description = "LO-E20: Confirm → Back ×2 — still no sticky Home")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Double Back after Confirm must not land on Current Earning / Kasim.")
    public void confirmThenDoubleBackNoSticky() {
        confirmLogoutFromDrawer();
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        DriverManager.get().navigate().back();
        sleepQuiet(1200);
        attachSessionParams("e20doubleBack");
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(looksStillLoggedIn()).as("no sticky after double Back").isFalse();
        softly.assertThat(hasText("Kasim Pathan")).as("no Kasim").isFalse();
        softly.assertThat(hasText("Current Earning")).as("no Current Earning").isFalse();
        Allure.parameter("bug", looksStillLoggedIn() ? "53" : "none");
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
}
