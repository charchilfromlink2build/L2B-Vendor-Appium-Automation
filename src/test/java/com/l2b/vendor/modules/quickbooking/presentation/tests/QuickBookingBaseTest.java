package com.l2b.vendor.modules.quickbooking.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.ui.BaseTest;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.onboarding.presentation.pages.LanguagePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OnboardingCarouselPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.SignUpPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.quickbooking.domain.QuickBookingEnvironment;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import java.lang.reflect.Method;
import java.time.Duration;
import org.openqa.selenium.TimeoutException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;

/**
 * Shared first-launch OTP path for this Quick Booking phase.
 * Account: rental company / vendor owner {@code 9000000001}, OTP {@code 1234}.
 * Do not log in as rental individual, material vendor, or operator here.
 * Isolated XML under {@code src/test/resources/quickbooking/}. Not in default
 * {@code testng.xml}.
 */
public abstract class QuickBookingBaseTest extends BaseTest {

    private static final String STATIC_OTP = "1234";

    @BeforeSuite(alwaysRun = true)
    public void prepareQuickBookingEnvironment() {
        QuickBookingEnvironment.prepareSuite();
    }

    @Override
    protected boolean noReset() {
        return QuickBookingEnvironment.noReset();
    }

    @Override
    protected boolean newSessionPerMethod() {
        return QuickBookingEnvironment.newSessionPerMethod();
    }

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
        String name = method.getName().toLowerCase();
        if (name.contains("direction") || name.contains("reopen") || name.contains("close")) {
            try {
                Adb.forceStop("com.google.android.apps.maps");
                Adb.forceStop("com.android.vending");
                Adb.pressHome();
            } catch (RuntimeException ignored) {
                // Session create still reports the real error.
            }
        }
    }

    protected OtpPage openOtp(String phone) {
        LanguagePage language = new LanguagePage();
        language.waitUntilLoaded();
        language.tapGetStarted();

        OnboardingCarouselPage carousel = new OnboardingCarouselPage();
        carousel.waitUntilSlideOne();
        carousel.tapNext();
        carousel.waitUntilSlideTwo();
        carousel.tapGetStarted();

        SignUpPage signUp = new SignUpPage();
        signUp.waitUntilLoaded();
        signUp.enterPhone(phone);
        signUp.hideKeyboard();
        signUp.acceptTerms();
        Waits.until(DriverManager.get(),
                d -> signUp.isGetOtpEnabled() ? Boolean.TRUE : null,
                "Get OTP stayed disabled before opening OTP",
                Duration.ofSeconds(8));
        signUp.tapGetOtp();

        OtpPage otp = new OtpPage();
        otp.waitUntilLoaded();
        return otp;
    }

    protected QuickBookingLandingPage loginToLanding(String phone) {
        OtpPage otp = openOtp(phone);
        otp.focusOtpField();
        if (!otp.otpFieldText().isEmpty()) {
            otp.clearOtp();
        }
        otp.pressDigitKeys(STATIC_OTP);

        QuickBookingLandingPage page = new QuickBookingLandingPage();
        try {
            Waits.until(DriverManager.get(),
                    d -> page.isDisplayedNow() ? Boolean.TRUE : null,
                    "Quick Booking landing did not appear after OTP",
                    Duration.ofSeconds(8));
            return page;
        } catch (TimeoutException first) {
            Allure.parameter("otpFieldAfterKeys", otp.otpFieldText());
            Allure.parameter("stillOnOtp", String.valueOf(otp.isDisplayedNow()));
            if (otp.isDisplayedNow()) {
                otp.pasteOtp(STATIC_OTP);
            }
            page.waitUntilLoaded();
            return page;
        }
    }

    @AfterMethod(alwaysRun = true)
    public void restoreRadiosAfterQuickBooking() {
        try {
            Adb.ensureNetworkReady();
        } catch (RuntimeException ignored) {
            // radio restore must not hide the test failure
        }
    }

    protected static boolean hasOfflineCopy(String source) {
        String s = source == null ? "" : source.toLowerCase();
        return s.contains("no internet") || s.contains("no network") || s.contains("offline")
                || s.contains("retry") || s.contains("check your connection")
                || s.contains("something went wrong") || s.contains("unable to");
    }

    protected void waitForLoggedInLanding(QuickBookingLandingPage page, HomePage home) {
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        Waits.until(android,
                d -> {
                    if (!pkg.equals(android.getCurrentPackage())) {
                        return null;
                    }
                    return (page.isDisplayedNow() || home.isDisplayedNow()) ? Boolean.TRUE : null;
                },
                "After relaunch, no Quick Booking or Home",
                Duration.ofSeconds(20));
    }

    protected static String rentalOwnerPhone() {
        return Config.get("user.rental.company.phone");
    }

    protected static String vendorPackage() {
        return ((AndroidDriver) DriverManager.get()).getCurrentPackage();
    }
}
