package com.l2b.vendor.modules.team.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import com.l2b.vendor.modules.settings.presentation.tests.ProfileDrawerBaseTest;
import com.l2b.vendor.modules.team.domain.TeamEnvironment;
import com.l2b.vendor.modules.team.presentation.pages.TeamPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import java.lang.reflect.Method;
import org.testng.annotations.BeforeSuite;

/**
 * Shared 9000000001 path onto Manage Team via Profile drawer.
 * Prefers warm logged-in session (noReset=true) — never waits for Language when
 * already past onboarding. Never Accept / Decline / Log Out Confirm.
 */
public abstract class TeamBaseTest extends ProfileDrawerBaseTest {

    @BeforeSuite(alwaysRun = true)
    public void prepareTeamEnvironment() {
        TeamEnvironment.prepareSuite();
    }

    @Override
    protected boolean noReset() {
        return TeamEnvironment.noReset();
    }

    @Override
    protected boolean newSessionPerMethod() {
        return TeamEnvironment.newSessionPerMethod();
    }

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        // Keep warm session — do not force-stop (parent QB base always force-stops).
    }

    @Override
    @Step("Reach Profile drawer via warm Home (or fresh OTP fallback)")
    protected ProfileDrawerPage reachProfileDrawer() {
        HomePage home = ensureRentalHomeWarm();
        home.tapDesc("Profile");
        ProfileDrawerPage page = new ProfileDrawerPage();
        page.waitUntilLoaded();
        Allure.parameter("entry", "profile-warm");
        Allure.parameter("after", classifyRentalNow());
        return page;
    }

    @Step("Ensure rental Home on warm session (Close QB only; no Accept/Decline)")
    protected HomePage ensureRentalHomeWarm() {
        try {
            ((AndroidDriver) DriverManager.get()).activateApp(Config.get("app.package"));
        } catch (RuntimeException e) {
            try {
                Adb.run("shell", "am", "start", "-n",
                        Config.get("app.package") + "/" + Config.get("app.activity"));
            } catch (RuntimeException ignored) {
            }
        }
        sleepQuiet(800);
        for (int attempt = 0; attempt < 2; attempt++) {
            for (int i = 0; i < 28; i++) {
                dismissGotItIfPresent();
                QuickBookingPage qb = new QuickBookingPage();
                if (qb.isDisplayedNow() || hasText("Quick Booking")) {
                    if (qb.isCloseVisible()) {
                        qb.tapClose();
                        sleepQuiet(1200);
                    }
                    dismissGotItIfPresent();
                    Allure.parameter("warmPath", "closed-quick-booking");
                }
                // Change Assignment sheet — Close sheet, never assign.
                if (hasText("Select machine for operator/driver")
                        || !DriverManager.get().findElements(
                                org.openqa.selenium.By.xpath("//*[@content-desc='Close sheet']"))
                                .isEmpty()) {
                    try {
                        new TeamPage().tapCloseSheet();
                    } catch (RuntimeException e) {
                        DriverManager.get().navigate().back();
                    }
                    sleepQuiet(700);
                    continue;
                }
                // Inner Profile routes — Back toward Home / drawer.
                if (hasText("Manage Team") || hasText("Profile Info")
                        || hasText("Help & Support")
                        || (hasText("Choose the language") && (hasText("Save") || hasText("सेव करें")))
                        || hasText("Your machines") || hasText("Complete KYC")
                        || hasText("KYC Verification")) {
                    DriverManager.get().navigate().back();
                    sleepQuiet(700);
                    continue;
                }
                if (profileDrawerNow() || (hasText("Account") && hasText("Log Out"))) {
                    DriverManager.get().navigate().back();
                    sleepQuiet(700);
                    continue;
                }
                if (new HomePage().isDisplayedNow() || hasText("Current Earning")
                        || hasText("Kasim Pathan") || hasText("Upcoming Booking")
                        || hasText("Active Fleet")) {
                    Allure.parameter("warmPath", "home");
                    return new HomePage();
                }
                if (hasText("Welcome to L2B") || hasText("Verify OTP")
                        || hasText("Enter your mobile number")
                        || (hasText("Get Started") && hasText("Choose the language"))) {
                    break;
                }
                if (i == 8 || i == 16 || i == 24) {
                    try {
                        ((AndroidDriver) DriverManager.get())
                                .activateApp(Config.get("app.package"));
                    } catch (RuntimeException ignored) {
                    }
                }
                sleepQuiet(500);
            }
            // noReset session: never wait for Language. Force-stop + relaunch once.
            if (noReset() && attempt == 0) {
                Allure.parameter("warmRecover", "force-stop-relaunch");
                try {
                    Adb.forceStop(Config.get("app.package"));
                    sleepQuiet(800);
                    ((AndroidDriver) DriverManager.get()).activateApp(Config.get("app.package"));
                    sleepQuiet(1500);
                } catch (RuntimeException ignored) {
                }
                continue;
            }
            break;
        }
        if (noReset()) {
            Allure.parameter("warmPath", "noReset-home-last-try");
            // Last chance: if somehow still logged-in chrome, return Home shell.
            if (hasText("Current Earning") || hasText("Kasim Pathan")
                    || new HomePage().isDisplayedNow()) {
                return new HomePage();
            }
            throw new IllegalStateException(
                    "Warm Home not found with noReset=true — refusing Language/OTP fallback. "
                            + "named=" + classifyRentalNow());
        }
        Allure.parameter("warmPath", "fresh-otp-fallback");
        return reachUsableRentalHomeOrFailExtendTime();
    }

    @Step("Dismiss Got it overlays")
    protected void dismissGotItIfPresent() {
        for (String label : new String[] {"Got it", "समझ गए", "అర్థమైంది", "ಅರ್ಥವಾಯಿತು", "OK"}) {
            if (hasText(label)) {
                var els = DriverManager.get().findElements(
                        org.openqa.selenium.By.xpath(
                                "//android.widget.TextView[@text='" + label + "']"));
                if (!els.isEmpty()) {
                    var r = els.get(0).getRect();
                    DriverManager.get().executeScript("mobile: clickGesture",
                            java.util.Map.of(
                                    "x", r.x + Math.max(1, r.width / 2),
                                    "y", r.y + Math.max(1, r.height / 2)));
                    sleepQuiet(600);
                }
            }
        }
    }

    @Step("Reach Manage Team via Profile → Manage team")
    protected TeamPage reachTeam() {
        ProfileDrawerPage drawer = reachProfileDrawer();
        drawer.tapRow("Manage team");
        sleepQuiet(1200);
        TeamPage page = new TeamPage();
        try {
            page.waitUntilLoaded();
        } catch (RuntimeException first) {
            // Drawer row miss / overlay — retry once from Home.
            Allure.parameter("teamReachRetry", first.getClass().getSimpleName());
            dismissGotItIfPresent();
            ensureRentalHomeWarm();
            new HomePage().tapDesc("Profile");
            sleepQuiet(1000);
            new ProfileDrawerPage().waitUntilLoaded();
            new ProfileDrawerPage().tapRow("Manage team");
            sleepQuiet(1200);
            page.waitUntilLoaded();
        }
        Allure.parameter("entry", "drawer-Manage team");
        Allure.parameter("after", classifyRentalNow());
        return page;
    }

    @Step("Dismiss toward Manage Team (Back from Add/Change only)")
    protected TeamPage dismissToTeam(TeamPage page) {
        for (int i = 0; i < 8; i++) {
            if (page.isDisplayedNow()) {
                return page;
            }
            String named = classifyRentalNow();
            if ("launcher".equals(named)) {
                return page;
            }
            if (profileDrawerNow() || (hasText("Account") && hasText("Log Out"))) {
                new ProfileDrawerPage().tapRow("Manage team");
                sleepQuiet(1000);
                if (page.isDisplayedNow()) {
                    return page;
                }
            }
            if ("home".equals(named) || new HomePage().isDisplayedNow()) {
                try {
                    new HomePage().tapDesc("Profile");
                    sleepQuiet(900);
                    new ProfileDrawerPage().tapRow("Manage team");
                    sleepQuiet(1000);
                    if (page.isDisplayedNow()) {
                        return page;
                    }
                } catch (RuntimeException ignored) {
                }
            }
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        if (!page.isDisplayedNow()) {
            return reachTeam();
        }
        return page;
    }
}
