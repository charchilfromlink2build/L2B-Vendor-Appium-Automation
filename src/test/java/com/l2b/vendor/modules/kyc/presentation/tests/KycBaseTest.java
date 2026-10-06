package com.l2b.vendor.modules.kyc.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.kyc.domain.KycEnvironment;
import com.l2b.vendor.modules.kyc.presentation.pages.KycPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import com.l2b.vendor.modules.settings.presentation.tests.ProfileDrawerBaseTest;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import java.lang.reflect.Method;
import org.testng.annotations.BeforeSuite;

/** Shared 9000000001 path onto Profile → KYC. Never Accept / Decline / Log Out Confirm. */
public abstract class KycBaseTest extends ProfileDrawerBaseTest {

    @BeforeSuite(alwaysRun = true)
    public void prepareKycEnvironment() {
        KycEnvironment.prepareSuite();
    }

    @Override
    protected boolean noReset() {
        return KycEnvironment.noReset();
    }

    @Override
    protected boolean newSessionPerMethod() {
        return KycEnvironment.newSessionPerMethod();
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

    @Step("Reach KYC via Profile drawer")
    protected KycPage reachKyc() {
        ProfileDrawerPage drawer = reachProfileDrawer();
        drawer.tapRow("KYC");
        sleepQuiet(1200);
        KycPage page = new KycPage();
        page.waitUntilLoaded();
        Allure.parameter("after", classifyRentalNow());
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
                if (new KycPage().isDisplayedNow()
                        || (hasText("KYC Details") || hasText("केवाईसी विवरण"))
                        || (hasText("FAQs") && hasText("Retry"))
                        || hasText("Accepting these terms")
                        || (hasText("Policies") && hasText("Something went wrong"))
                        || (hasText("Settings") && hasText("An unexpected error occurred"))
                        || hasText("Manage Team") || hasText("Profile Info")
                        || hasText("Help & Support")) {
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
                        || hasText("Enter your mobile number")
                        || hasText("Enter mobile number")) {
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
                    + classifyRentalNow() + " (OTP restore blocked if BUGS_FOUND #52)");
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
                    sleepQuiet(400);
                }
            }
        }
    }
}
