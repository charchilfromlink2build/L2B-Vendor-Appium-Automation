package com.l2b.vendor.modules.settings.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.LanguagePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OnboardingCarouselPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.SignUpPage;
import com.l2b.vendor.modules.settings.domain.LogoutEnvironment;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import java.lang.reflect.Method;
import org.testng.annotations.BeforeSuite;

/**
 * Log Out Confirm + session persistence checks on {@code 9000000001}.
 * After Confirm, restore login via OTP so later tests stay warm.
 */
public abstract class LogoutBaseTest extends ProfileDrawerBaseTest {

    @BeforeSuite(alwaysRun = true)
    public void prepareLogoutEnvironment() {
        LogoutEnvironment.prepareSuite();
    }

    @Override
    protected boolean noReset() {
        return LogoutEnvironment.noReset();
    }

    @Override
    protected boolean newSessionPerMethod() {
        return LogoutEnvironment.newSessionPerMethod();
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
        Allure.parameter("after", classifyRentalNow());
        return page;
    }

    @Step("Ensure rental Home warm (or re-login)")
    protected HomePage ensureRentalHomeWarm() {
        try {
            ((AndroidDriver) DriverManager.get()).activateApp(Config.get("app.package"));
        } catch (RuntimeException ignored) {
        }
        sleepQuiet(900);
        for (int i = 0; i < 20; i++) {
            dismissGotItIfPresent();
            QuickBookingPage qb = new QuickBookingPage();
            if (qb.isDisplayedNow() || hasText("Quick Booking")) {
                if (qb.isCloseVisible()) {
                    qb.tapClose();
                    sleepQuiet(1000);
                }
            }
            if (new ProfileDrawerPage().isLogoutDialogVisible()) {
                new ProfileDrawerPage().tapLogoutCancel();
                sleepQuiet(600);
            }
            if (profileDrawerNow() || (hasText("Account") && hasText("Log Out"))) {
                DriverManager.get().navigate().back();
                sleepQuiet(600);
                continue;
            }
            if (new HomePage().isDisplayedNow() || hasText("Current Earning")
                    || hasText("Kasim Pathan")) {
                return new HomePage();
            }
            if (looksLoggedOutUi()) {
                try {
                    return restoreSession0001();
                } catch (RuntimeException e) {
                    Allure.parameter("warmRestoreFail", e.getClass().getSimpleName());
                }
            }
            sleepQuiet(400);
        }
        if (looksLoggedOutUi() || hasText("Enter your mobile number") || hasText("Verify OTP")
                || hasText("Welcome to L2B")) {
            return restoreSession0001();
        }
        if (noReset() && (hasText("Current Earning") || new HomePage().isDisplayedNow())) {
            return new HomePage();
        }
        return reachUsableRentalHomeOrFailExtendTime();
    }

    @Step("Looks like logged-out auth UI")
    protected boolean looksLoggedOutUi() {
        return hasText("Enter your mobile number")
                || hasText("Enter mobile number")
                || hasText("Get OTP")
                || hasText("Verify OTP")
                || hasText("Welcome to L2B")
                || hasText("Choose your language")
                || (hasText("Choose the language") && (hasText("Get started") || hasText("Get Started")))
                || (hasText("Sign up") && hasText("Get OTP") && !hasText("Current Earning"));
    }

    @Step("Still looks logged-in vendor Home")
    protected boolean looksStillLoggedIn() {
        return new HomePage().isDisplayedNow()
                || hasText("Current Earning")
                || hasText("Kasim Pathan")
                || hasText("Upcoming Booking")
                || (hasText("Account") && hasText("Log Out") && hasText("Manage team"));
    }

    @Step("Confirm Log Out from drawer")
    protected void confirmLogoutFromDrawer() {
        ProfileDrawerPage drawer = reachProfileDrawer();
        drawer.tapLogOut();
        sleepQuiet(800);
        if (!drawer.isLogoutDialogVisible()) {
            throw new IllegalStateException("Log Out? dialog missing");
        }
        drawer.tapLogoutConfirm();
        sleepQuiet(2000);
    }

    @Step("Force-stop + relaunch app (session persistence probe)")
    protected void forceStopRelaunch() {
        String pkg = Config.get("app.package");
        Adb.forceStop(pkg);
        sleepQuiet(1000);
        try {
            ((AndroidDriver) DriverManager.get()).activateApp(pkg);
        } catch (RuntimeException e) {
            Adb.run("shell", "am", "start", "-n", pkg + "/" + Config.get("app.activity"));
        }
        sleepQuiet(2500);
        dismissGotItIfPresent();
    }

    @Step("Restore 9000000001 session via OTP after Confirm logout")
    protected HomePage restoreSession0001() {
        Allure.parameter("restore", "otp-0001");
        try {
            return attemptRestoreLogin();
        } catch (RuntimeException first) {
            Allure.parameter("restoreRetry", first.getClass().getSimpleName());
            // Do not pm clear (breaks noReset Appium sessions). Force-stop + full OTP path.
            try {
                Adb.forceStop(Config.get("app.package"));
                sleepQuiet(1000);
                ((AndroidDriver) DriverManager.get()).activateApp(Config.get("app.package"));
                sleepQuiet(2000);
            } catch (RuntimeException ignored) {
            }
            return reachUsableRentalHomeOrFailExtendTime();
        }
    }

    @Step("Attempt login restore without pm clear")
    protected HomePage attemptRestoreLogin() {
        try {
            Adb.forceStop(Config.get("app.package"));
            sleepQuiet(800);
            ((AndroidDriver) DriverManager.get()).activateApp(Config.get("app.package"));
            sleepQuiet(1800);
        } catch (RuntimeException ignored) {
        }
        dismissGotItIfPresent();
        // Prefer full Language → carousel → Sign up → OTP (LanguagePage clicks parent CTA).
        LanguagePage language = new LanguagePage();
        if (language.isDisplayedNow() || hasText("Welcome to L2B") || hasText("Choose your language")) {
            return reachUsableRentalHomeOrFailExtendTime();
        }
        OnboardingCarouselPage carousel = new OnboardingCarouselPage();
        if (carousel.isLoaded()) {
            return reachUsableRentalHomeOrFailExtendTime();
        }
        if (hasText("Enter your mobile number") || hasText("Enter mobile number")
                || (hasText("Sign up") && hasText("Get OTP"))) {
            SignUpPage signUp = new SignUpPage();
            signUp.waitUntilLoaded();
            signUp.enterPhone(rentalCompanyPhone());
            signUp.hideKeyboard();
            signUp.acceptTerms();
            sleepQuiet(500);
            signUp.tapGetOtp();
            com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage otp =
                    new com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage();
            otp.waitUntilLoaded();
            otp.focusOtpField();
            if (!otp.otpFieldText().isEmpty()) {
                otp.clearOtp();
            }
            otp.pressDigitKeys("1234");
            QuickBookingPage qb = new QuickBookingPage();
            com.l2b.vendor.core.wait.Waits.until(DriverManager.get(),
                    d -> qb.isDisplayedNow() || new HomePage().isDisplayedNow()
                            ? Boolean.TRUE : null,
                    "Home/QB after signup restore OTP", java.time.Duration.ofSeconds(20));
            if (qb.isDisplayedNow() && qb.isCloseVisible()) {
                qb.tapClose();
                sleepQuiet(1000);
            }
            return new HomePage();
        }
        if (hasText("Verify OTP")) {
            com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage otp =
                    new com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage();
            otp.focusOtpField();
            otp.pressDigitKeys("1234");
            sleepQuiet(2000);
            QuickBookingPage qb = new QuickBookingPage();
            if (qb.isDisplayedNow() && qb.isCloseVisible()) {
                qb.tapClose();
                sleepQuiet(1000);
            }
            if (new HomePage().isDisplayedNow() || hasText("Current Earning")) {
                return new HomePage();
            }
        }
        // Unknown — full first-launch OTP path
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

    protected void attachSessionParams(String prefix) {
        Allure.parameter(prefix + "Named", classifyRentalNow());
        Allure.parameter(prefix + "LoggedIn", String.valueOf(looksStillLoggedIn()));
        Allure.parameter(prefix + "LoggedOutUi", String.valueOf(looksLoggedOutUi()));
        try {
            Allure.parameter(prefix + "Pkg",
                    ((AndroidDriver) DriverManager.get()).getCurrentPackage());
        } catch (RuntimeException ignored) {
        }
    }
}
