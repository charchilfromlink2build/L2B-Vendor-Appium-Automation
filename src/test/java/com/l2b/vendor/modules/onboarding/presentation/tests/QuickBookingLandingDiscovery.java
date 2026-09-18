package com.l2b.vendor.modules.onboarding.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.ui.BaseTest;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.onboarding.domain.OnboardingEnvironment;
import com.l2b.vendor.modules.onboarding.presentation.pages.LanguagePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OnboardingCarouselPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import com.l2b.vendor.modules.onboarding.presentation.pages.SignUpPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Discovery only: post-OTP landing for known QA phones. Not a Quick Booking test suite.
 * Isolated {@code quick-booking-discovery.xml}. Do not add to default {@code testng.xml}.
 */
@Epic("Vendor app")
@Feature("Quick Booking landing discovery")
public class QuickBookingLandingDiscovery extends BaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-qb-discovery");
    private static final Pattern BOOKING_NO = Pattern.compile("L2B-RNT-[A-Z0-9-]+|MAT-[0-9-]+|RB-[A-Z0-9-]+");

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
    protected void beforeCreateDriver(Method method) {
        Adb.forceStop("com.l2b.app.qa");
        Adb.pressHome();
    }

    @Test(description = "Discovery: 9000000001 rental company after OTP")
    @Description("Cold start, first-launch to OTP, 1234. Record whether Quick Booking appears before Home.")
    public void observeRentalCompany0001() {
        observe("9000000001", "rental-company-0001");
    }

    @Test(description = "Discovery: 9000000017 material vendor after OTP")
    @Description("Cold start, first-launch to OTP, 1234. Record whether Quick Booking appears before Home.")
    public void observeMaterial0017() {
        observe("9000000017", "material-0017");
    }

    @Test(description = "Discovery: 9000000003 rental individual after OTP")
    @Description("Cold start, first-launch to OTP, 1234. Record whether Quick Booking appears before Home.")
    public void observeRentalIndividual0003() {
        observe("9000000003", "rental-individual-0003");
    }

    private void observe(String phone, String tag) {
        OtpPage otp = openOtp(phone);
        otp.focusOtpField();
        otp.pressDigitKeys("1234");

        String landing = waitForPostOtpLanding();
        String xml = DriverManager.get().getPageSource();
        Set<String> texts = visibleTexts(xml);
        Set<String> ids = bookingIds(xml);
        Path dir = DUMP_DIR.resolve(tag);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("landing.txt"),
                    "phone=" + phone + "\nlanding=" + landing + "\npackage="
                            + ((AndroidDriver) DriverManager.get()).getCurrentPackage()
                            + "\ntexts=\n" + String.join("\n", texts)
                            + "\nids=" + ids + "\n",
                    StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
            Files.write(dir.resolve("screen.png"),
                    ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write discovery dump for " + tag, e);
        }
        System.out.println("DISCOVERY " + phone + " landing=" + landing + " ids=" + ids);
        System.out.println("DISCOVERY texts=" + texts);
        otp.attachScreenshot("discovery-" + tag);
    }

    private OtpPage openOtp(String phone) {
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

    private String waitForPostOtpLanding() {
        long deadline = System.currentTimeMillis() + 20_000;
        String last = "otp";
        while (System.currentTimeMillis() < deadline) {
            last = classify(DriverManager.get().getPageSource());
            if (!"otp".equals(last) && !"unknown".equals(last)) {
                return last;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return last;
            }
        }
        return classify(DriverManager.get().getPageSource());
    }

    private static String classify(String xml) {
        String lower = xml.toLowerCase();
        if (xml.contains("Verify your OTP")) {
            return "otp";
        }
        if (xml.contains("Quick Booking") || xml.contains("Quick booking")) {
            return "quick-booking";
        }
        if (xml.contains("Sign Up Completed") || xml.contains("Register Yourself")) {
            return "signup-completed";
        }
        if (xml.contains("Good Morning") || xml.contains("Current Earning")
                || xml.contains("My Bookings") || xml.contains("My Machines")) {
            return "home";
        }
        if (lower.contains("accept") && lower.contains("reject")) {
            return "accept-reject-card";
        }
        return "unknown";
    }

    private static Set<String> visibleTexts(String xml) {
        Matcher m = Pattern.compile("text=\"([^\"]{2,80})\"").matcher(xml);
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) {
            String t = m.group(1);
            if (!t.startsWith("http") && !t.contains("com.android")) {
                out.add(t);
            }
        }
        return out.stream().limit(80).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static Set<String> bookingIds(String xml) {
        Matcher m = BOOKING_NO.matcher(xml);
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }
}
