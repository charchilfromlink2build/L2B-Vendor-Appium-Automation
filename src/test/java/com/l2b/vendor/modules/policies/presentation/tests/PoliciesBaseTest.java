package com.l2b.vendor.modules.policies.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.policies.domain.PoliciesEnvironment;
import com.l2b.vendor.modules.policies.presentation.pages.PoliciesPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import com.l2b.vendor.modules.settings.presentation.tests.ProfileDrawerBaseTest;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import java.lang.reflect.Method;
import org.testng.annotations.BeforeSuite;

/** Shared 9000000001 path onto Policies via Profile drawer. */
public abstract class PoliciesBaseTest extends ProfileDrawerBaseTest {

    @BeforeSuite(alwaysRun = true)
    public void preparePoliciesEnvironment() {
        PoliciesEnvironment.prepareSuite();
    }

    @Override
    protected boolean noReset() {
        return PoliciesEnvironment.noReset();
    }

    @Override
    protected boolean newSessionPerMethod() {
        return PoliciesEnvironment.newSessionPerMethod();
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
                        || hasText("Retry")
                        || hasText("Why is my account still under review?")))
                        || hasText("Accepting these terms")
                        || hasText("Your obligations")
                        || (hasText("Policies") && (hasText("What we collect")
                        || hasText("How we use it")
                        || hasText("Your rights")
                        || hasText("Something went wrong")))
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

    @Step("Reach Policies via Profile → Policies")
    protected PoliciesPage reachPolicies() {
        ProfileDrawerPage drawer = reachProfileDrawer();
        drawer.tapRow("Policies");
        sleepQuiet(1200);
        PoliciesPage page = new PoliciesPage();
        try {
            page.waitUntilLoaded();
        } catch (RuntimeException first) {
            ensureRentalHomeWarm();
            new HomePage().tapDesc("Profile");
            sleepQuiet(900);
            new ProfileDrawerPage().waitUntilLoaded();
            new ProfileDrawerPage().tapRow("Policies");
            sleepQuiet(1200);
            page.waitUntilLoaded();
        }
        Allure.parameter("after", classifyRentalNow());
        return page;
    }
}
