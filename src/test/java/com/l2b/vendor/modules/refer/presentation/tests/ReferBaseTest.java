package com.l2b.vendor.modules.refer.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.refer.domain.ReferEnvironment;
import com.l2b.vendor.modules.refer.presentation.pages.ReferPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import com.l2b.vendor.modules.settings.presentation.tests.ProfileDrawerBaseTest;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import java.lang.reflect.Method;
import org.testng.annotations.BeforeSuite;

/**
 * Shared 9000000001 path onto Refer & Earn via Profile drawer.
 * Warm logged-in session (noReset=true). Never Accept / Decline / Log Out Confirm.
 */
public abstract class ReferBaseTest extends ProfileDrawerBaseTest {

    @BeforeSuite(alwaysRun = true)
    public void prepareReferEnvironment() {
        ReferEnvironment.prepareSuite();
    }

    @Override
    protected boolean noReset() {
        return ReferEnvironment.noReset();
    }

    @Override
    protected boolean newSessionPerMethod() {
        return ReferEnvironment.newSessionPerMethod();
    }

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
    }

    @Override
    @Step("Reach Profile drawer via warm Home")
    protected ProfileDrawerPage reachProfileDrawer() {
        HomePage home = ensureRentalHomeWarm();
        home.tapDesc("Profile");
        ProfileDrawerPage page = new ProfileDrawerPage();
        page.waitUntilLoaded();
        Allure.parameter("entry", "profile-warm");
        Allure.parameter("after", classifyRentalNow());
        return page;
    }

    @Step("Ensure rental Home on warm session")
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
                }
                if (hasText("Select machine for operator/driver")) {
                    DriverManager.get().navigate().back();
                    sleepQuiet(700);
                    continue;
                }
                if ((hasText("Refer now") && (hasText("Your referral code")
                        || hasText("Referrals are not available right now.")))
                        || (hasText("How do I start a booked job?")
                        && (hasText("FAQ") || hasText("How do I talk to a human?")))
                        || hasText("Accepting these terms")
                        || hasText("Manage Team") || hasText("Profile Info")
                        || hasText("Help & Support")
                        || (hasText("Choose the language")
                        && (hasText("Save") || hasText("सेव करें")))) {
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

    @Step("Reach Refer & Earn via Profile → Refer & Earn")
    protected ReferPage reachRefer() {
        ProfileDrawerPage drawer = reachProfileDrawer();
        drawer.tapRow("Refer & Earn");
        sleepQuiet(1200);
        ReferPage page = new ReferPage();
        try {
            page.waitUntilLoaded();
        } catch (RuntimeException first) {
            Allure.parameter("referReachRetry", first.getClass().getSimpleName());
            dismissGotItIfPresent();
            ensureRentalHomeWarm();
            new HomePage().tapDesc("Profile");
            sleepQuiet(1000);
            new ProfileDrawerPage().waitUntilLoaded();
            new ProfileDrawerPage().tapRow("Refer & Earn");
            sleepQuiet(1200);
            page.waitUntilLoaded();
        }
        Allure.parameter("entry", "drawer-Refer & Earn");
        Allure.parameter("after", classifyRentalNow());
        return page;
    }

    @Step("Dismiss toward Refer (Back from share only)")
    protected ReferPage dismissToRefer(ReferPage page) {
        for (int i = 0; i < 8; i++) {
            if (page.isDisplayedNow()) {
                return page;
            }
            String named = classifyRentalNow();
            if ("launcher".equals(named)) {
                return page;
            }
            if (profileDrawerNow() || (hasText("Account") && hasText("Log Out"))) {
                new ProfileDrawerPage().tapRow("Refer & Earn");
                sleepQuiet(1000);
                if (page.isDisplayedNow()) {
                    return page;
                }
            }
            if ("home".equals(named) || new HomePage().isDisplayedNow()) {
                try {
                    new HomePage().tapDesc("Profile");
                    sleepQuiet(900);
                    new ProfileDrawerPage().tapRow("Refer & Earn");
                    sleepQuiet(1000);
                    if (page.isDisplayedNow()) {
                        return page;
                    }
                } catch (RuntimeException ignored) {
                }
            }
            // Android share sheet / chooser — device Back.
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        if (!page.isDisplayedNow()) {
            return reachRefer();
        }
        return page;
    }
}
