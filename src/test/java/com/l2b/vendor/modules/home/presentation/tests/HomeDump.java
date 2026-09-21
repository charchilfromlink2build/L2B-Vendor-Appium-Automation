package com.l2b.vendor.modules.home.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.bookings.presentation.tests.quickbooking.QuickBookingBaseTest;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.annotations.Test;

/**
 * Dump-0: Material vendor {@code 9000000017} Home after empty Quick Booking queue.
 * Isolated {@code home/home-dump.xml}. Not a product case. No taps after OTP
 * except waiting. Do not tap Accept/Decline, extend time, or other tabs.
 */
@Epic("Vendor app")
@Feature("Home dump — material vendor 9000000017")
public class HomeDump extends QuickBookingBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-home-material-0017-20260921");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Dump-0: 9000000017 Home after OTP — no further taps")
    @Description("Cold start, OTP 1234 on 9000000017. Dump landing chrome. Stop if Quick Booking "
            + "intercepts. Do not tap Accept/Decline, tabs, profile, or extend time.")
    public void dumpHome0017() {
        String phone = materialPhone();
        if (!"9000000017".equals(phone)) {
            throw new IllegalStateException("Refusing dump: material phone is " + phone);
        }

        OtpPage otp = openOtp(phone);
        otp.focusOtpField();
        if (!otp.otpFieldText().isEmpty()) {
            otp.clearOtp();
        }
        otp.pressDigitKeys("1234");

        QuickBookingPage qb = new QuickBookingPage();
        HomePage home = new HomePage();
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            if (qb.isDisplayedNow() || home.isDisplayedNow()) {
                break;
            }
            if (otp.isDisplayedNow() && otp.otpFieldText().isEmpty()) {
                otp.pasteOtp("1234");
            }
            sleepQuiet(400);
        }

        String xml = safeSource();
        String landing = classify(xml);
        writeDump("00-landing", landing, xml);
        Allure.parameter("phone", phone);
        Allure.parameter("landing", landing);
        Allure.parameter("quickBooking", String.valueOf(qb.isDisplayedNow()));
        Allure.parameter("homePageObject", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("package", String.valueOf(vendorPackage()));
        qb.attachScreenshot("home-0017-00-landing");

        System.out.println("HOME_DUMP landing=" + landing
                + " qb=" + qb.isDisplayedNow()
                + " homeObj=" + home.isDisplayedNow()
                + " dir=" + DUMP_DIR);
        if (qb.isDisplayedNow()) {
            System.out.println("HOME_DUMP STOP — Quick Booking intercepted. Do not consume.");
        }
    }

    private void writeDump(String tag, String landing, String xml) {
        Path dir = DUMP_DIR.resolve(tag);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("landing.txt"),
                    "phone=9000000017\nlanding=" + landing
                            + "\npackage=" + ((AndroidDriver) DriverManager.get()).getCurrentPackage()
                            + "\nhomePageObject=" + new HomePage().isDisplayedNow()
                            + "\nquickBooking=" + new QuickBookingPage().isDisplayedNow()
                            + "\ntexts=\n" + String.join("\n", visibleAttr(xml, TEXT))
                            + "\ndescs=\n" + String.join("\n", visibleAttr(xml, DESC))
                            + "\n",
                    StandardCharsets.UTF_8);
            Files.write(dir.resolve("screen.png"),
                    ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write home dump " + tag, e);
        }
        System.out.println("HOME_DUMP " + tag + " landing=" + landing);
    }

    private static String safeSource() {
        return DriverManager.get().getPageSource();
    }

    private static String classify(String xml) {
        if (xml.contains("Verify your OTP")) {
            return "otp";
        }
        if (xml.contains("content-desc=\"Close\"")
                && (xml.contains("Quick Booking") || xml.contains("Quick booking"))) {
            return "quick-booking";
        }
        if (xml.contains("Sign Up Completed")) {
            return "signup-completed";
        }
        return "home-or-other";
    }

    private static Set<String> visibleAttr(String xml, Pattern pattern) {
        Matcher m = pattern.matcher(xml);
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) {
            String t = m.group(1);
            if (!t.isBlank() && !t.startsWith("http") && !t.contains("com.android")) {
                out.add(t);
            }
        }
        return out;
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
