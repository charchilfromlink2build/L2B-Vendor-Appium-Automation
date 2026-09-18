package com.l2b.vendor.modules.bookings.presentation.tests.quickbooking;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.BaseTest;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.domain.OnboardingEnvironment;
import com.l2b.vendor.modules.onboarding.presentation.pages.LanguagePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OnboardingCarouselPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.SignUpPage;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import java.lang.reflect.Method;
import java.time.Duration;
import org.testng.annotations.BeforeSuite;

/**
 * Shared first-launch OTP path for Quick Booking persona tests.
 * {@code noReset=false}, one session per method. Never tap Accept or Decline.
 */
public abstract class QuickBookingBaseTest extends BaseTest {

    @BeforeSuite(alwaysRun = true)
    public void prepareOnboardingEnvironment() {
        OnboardingEnvironment.prepareSuite();
    }

    @Override
    protected boolean noReset() {
        return false;
    }

    @Override
    protected boolean newSessionPerMethod() {
        return true;
    }

    @Override
    protected boolean autoGrantPermissions() {
        return true;
    }

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.forceStop("com.l2b.app.qa");
        String name = method.getName();
        if (name.toLowerCase().contains("back") || name.toLowerCase().contains("session")
                || name.toLowerCase().contains("relaunch")) {
            try {
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

    protected QuickBookingPage loginToQuickBooking(String phone) {
        OtpPage otp = openOtp(phone);
        otp.focusOtpField();
        otp.pressDigitKeys("1234");
        QuickBookingPage page = new QuickBookingPage();
        page.waitUntilLoaded();
        return page;
    }

    protected void submitStaticOtp(String phone) {
        OtpPage otp = openOtp(phone);
        otp.focusOtpField();
        otp.pressDigitKeys("1234");
    }

    protected void waitForLoggedInLanding(QuickBookingPage page, HomePage home, AndroidDriver android,
            String pkg) {
        Waits.until(android,
                d -> {
                    if (!pkg.equals(android.getCurrentPackage())) {
                        return null;
                    }
                    return (page.isDisplayedNow()
                            || home.isDisplayedNow()
                            || new LanguagePage().isTitleEnglish()
                            || new SignUpPage().isDisplayedNow()
                            || new OnboardingCarouselPage().isLoaded()) ? Boolean.TRUE : null;
                },
                "After relaunch, no Quick Booking / Home / first-launch screen",
                Duration.ofSeconds(20));
    }

    protected static String namedLanding(QuickBookingPage page, HomePage home) {
        String pkg = vendorPackage();
        if (pkg == null || pkg.isBlank()) {
            return "unknown";
        }
        if (!"com.l2b.app.qa".equals(pkg)) {
            if (pkg.contains("launcher")) {
                return "launcher";
            }
            return "other-package:" + pkg;
        }
        if (page.isDisplayedNow()) {
            return "quick-booking";
        }
        if (profileDrawerNow()) {
            return "home-drawer";
        }
        if (home.isDisplayedNow()) {
            return "home";
        }
        if (new LanguagePage().isTitleEnglish()) {
            return "language";
        }
        if (new SignUpPage().isDisplayedNow()) {
            return "signup";
        }
        return "vendor-other";
    }

    protected static boolean launcherNow() {
        String pkg = vendorPackage();
        return pkg != null && pkg.contains("launcher");
    }

    /** Home account sheet: Account + Log Out. Do not tap Log Out. */
    protected static boolean profileDrawerNow() {
        AppiumDriver driver = DriverManager.get();
        return !driver.findElements(ComposeLocators.textView("Account")).isEmpty()
                && !driver.findElements(ComposeLocators.textView("Log Out")).isEmpty();
    }

    protected static String vendorPackage() {
        return ((AndroidDriver) DriverManager.get()).getCurrentPackage();
    }

    protected static String rentalCompanyPhone() {
        return Config.get("user.rental.company.phone");
    }

    protected static String rentalIndividualPhone() {
        return Config.get("user.rental.individual.phone");
    }

    protected static String materialPhone() {
        return Config.get("user.material.phone");
    }

    protected static String operatorPhone() {
        return Config.get("user.operator.phone");
    }
}
