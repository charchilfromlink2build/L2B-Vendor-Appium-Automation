package com.l2b.vendor.modules.settings.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.domain.SettingsEnvironment;
import com.l2b.vendor.modules.settings.presentation.pages.AppSettingsPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import java.lang.reflect.Method;
import org.testng.annotations.BeforeSuite;

/** Shared 9000000001 path onto Profile → Settings (permissions). */
public abstract class AppSettingsBaseTest extends ProfileDrawerBaseTest {

    @BeforeSuite(alwaysRun = true)
    public void prepareAppSettingsEnvironment() {
        SettingsEnvironment.prepareSuite();
    }

    @Override
    protected boolean noReset() {
        return SettingsEnvironment.noReset();
    }

    @Override
    protected boolean newSessionPerMethod() {
        return SettingsEnvironment.newSessionPerMethod();
    }

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
    }

    @Override
    protected ProfileDrawerPage reachProfileDrawer() {
        HomePage home = ensureRentalHomeWarm();
        home.tapDesc("Profile");
        ProfileDrawerPage page = new ProfileDrawerPage();
        page.waitUntilLoaded();
        return page;
    }

    @Step("Ensure rental Home warm")
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
                }
                if ((hasText("Refer now") && (hasText("Your referral code")
                        || hasText("Referrals are not available right now.")))
                        || (hasText("FAQs") && (hasText("Something went wrong")
                        || hasText("Retry")))
                        || hasText("Accepting these terms")
                        || (hasText("Policies") && (hasText("What we collect")
                        || hasText("How we use it")
                        || hasText("Something went wrong")))
                        || (hasText("Settings") && hasText("An unexpected error occurred"))
                        || (hasText("Settings") && hasText("Permissions")
                        && hasText("Notification"))
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
                        || hasText("Kasim Pathan") || hasText("Upcoming Booking")) {
                    return new HomePage();
                }
                if (hasText("Welcome to L2B") || hasText("Verify OTP")
                        || hasText("Enter your mobile number")) {
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
            if (hasText("Current Earning") || new HomePage().isDisplayedNow()) {
                return new HomePage();
            }
            throw new IllegalStateException("Warm Home not found noReset — named="
                    + classifyRentalNow());
        }
        return reachUsableRentalHomeOrFailExtendTime();
    }

    protected void dismissGotItIfPresent() {
        for (String label : new String[] {"Got it", "समझ गए", "OK"}) {
            if (hasText(label)) {
                var els = DriverManager.get().findElements(
                        org.openqa.selenium.By.xpath(
                                "//android.widget.TextView[@text='" + label + "']"));
                if (!els.isEmpty()) {
                    var r = els.get(0).getRect();
                    DriverManager.get().executeScript("mobile: clickGesture",
                            java.util.Map.of("x", r.x + Math.max(1, r.width / 2),
                                    "y", r.y + Math.max(1, r.height / 2)));
                    sleepQuiet(500);
                }
            }
        }
    }

    @Step("Reach Settings via Profile → Settings")
    protected AppSettingsPage reachAppSettings() {
        ProfileDrawerPage drawer = reachProfileDrawer();
        drawer.tapRow("Settings");
        sleepQuiet(1200);
        AppSettingsPage page = new AppSettingsPage();
        try {
            page.waitUntilLoaded();
        } catch (RuntimeException first) {
            ensureRentalHomeWarm();
            new HomePage().tapDesc("Profile");
            sleepQuiet(900);
            new ProfileDrawerPage().waitUntilLoaded();
            new ProfileDrawerPage().tapRow("Settings");
            sleepQuiet(1200);
            page.waitUntilLoaded();
        }
        Allure.parameter("after", classifyRentalNow());
        return page;
    }
}
