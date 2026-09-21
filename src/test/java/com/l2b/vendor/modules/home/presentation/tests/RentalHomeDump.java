package com.l2b.vendor.modules.home.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.bookings.presentation.tests.quickbooking.QuickBookingBaseTest;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OtpPage;
import com.l2b.vendor.modules.quickbooking.presentation.pages.QuickBookingLandingPage;
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
 * Dump-0: Rental company {@code 9000000001} Home. Isolated
 * {@code home/home-rental-dump.xml}. Not a product case. If Quick Booking
 * intercepts, Close once (not rapid) then dump Home. Do not tap Accept,
 * Decline, extend time, Fleet, Calendar, Bookings, or Earning.
 */
@Epic("Vendor app")
@Feature("Home dump — rental vendor 9000000001")
public class RentalHomeDump extends QuickBookingBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-home-rental-0001-20260921");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Dump-0: 9000000001 Rental Home after OTP — Close only if QB intercepts")
    @Description("Cold start, OTP 1234 on 9000000001. Dump post-OTP. If Quick Booking, Close once "
            + "and dump Home. Do not tap Accept/Decline, extend time, or other tabs.")
    public void dumpHome0001() {
        String phone = rentalCompanyPhone();
        if (!"9000000001".equals(phone)) {
            throw new IllegalStateException("Refusing dump: rental company phone is " + phone);
        }

        OtpPage otp = openOtp(phone);
        otp.focusOtpField();
        if (!otp.otpFieldText().isEmpty()) {
            otp.clearOtp();
        }
        otp.pressDigitKeys("1234");

        QuickBookingPage qb = new QuickBookingPage();
        HomePage home = new HomePage();
        QuickBookingLandingPage landing = new QuickBookingLandingPage();
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            if (qb.isDisplayedNow() || home.isDisplayedNow() || landing.isExtendTimeDialogVisible()) {
                break;
            }
            if (otp.isDisplayedNow() && otp.otpFieldText().isEmpty()) {
                otp.pasteOtp("1234");
            }
            sleepQuiet(400);
        }

        String afterOtp = classify(safeSource());
        writeDump("00-after-otp", afterOtp);
        Allure.parameter("phone", phone);
        Allure.parameter("afterOtp", afterOtp);
        Allure.parameter("quickBooking", String.valueOf(qb.isDisplayedNow()));
        Allure.parameter("extendTime", String.valueOf(landing.isExtendTimeDialogVisible()));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        Allure.parameter("package", String.valueOf(vendorPackage()));
        home.attachScreenshot("rental-home-0001-00-after-otp");

        if (qb.isDisplayedNow()) {
            System.out.println("RENTAL_HOME_DUMP QB intercept — Close once, no Accept/Decline");
            qb.tapClose();
            long closeDeadline = System.currentTimeMillis() + 12_000;
            while (System.currentTimeMillis() < closeDeadline) {
                if (!qb.isDisplayedNow()) {
                    break;
                }
                sleepQuiet(300);
            }
            String afterClose = classify(safeSource());
            writeDump("01-after-close", afterClose);
            Allure.parameter("afterClose", afterClose);
            Allure.parameter("extendAfterClose", String.valueOf(landing.isExtendTimeDialogVisible()));
            Allure.parameter("homeAfterClose", String.valueOf(home.isDisplayedNow()));
            home.attachScreenshot("rental-home-0001-01-after-close");
            System.out.println("RENTAL_HOME_DUMP afterClose=" + afterClose
                    + " extend=" + landing.isExtendTimeDialogVisible()
                    + " home=" + home.isDisplayedNow());

            if (landing.isExtendTimeDialogVisible()) {
                org.openqa.selenium.Dimension size = DriverManager.get().manage().window().getSize();
                DriverManager.get().executeScript("mobile: clickGesture", java.util.Map.of(
                        "x", size.width / 2,
                        "y", (int) (size.height * 0.12)));
                sleepQuiet(800);
                String afterOutside = classify(safeSource());
                writeDump("02-after-outside-tap", afterOutside);
                Allure.parameter("afterOutsideTap", afterOutside);
                home.attachScreenshot("rental-home-0001-02-outside");
                System.out.println("RENTAL_HOME_DUMP afterOutside=" + afterOutside
                        + " extend=" + landing.isExtendTimeDialogVisible()
                        + " home=" + home.isDisplayedNow());
                if (landing.isExtendTimeDialogVisible()) {
                    ((AndroidDriver) DriverManager.get()).pressKey(
                            new io.appium.java_client.android.nativekey.KeyEvent(
                                    io.appium.java_client.android.nativekey.AndroidKey.BACK));
                    sleepQuiet(800);
                    String afterBack = classify(safeSource());
                    writeDump("03-after-back", afterBack);
                    Allure.parameter("afterBack", afterBack);
                    Allure.parameter("packageAfterBack", String.valueOf(vendorPackage()));
                    home.attachScreenshot("rental-home-0001-03-back");
                    System.out.println("RENTAL_HOME_DUMP afterBack=" + afterBack
                            + " pkg=" + vendorPackage()
                            + " extend=" + landing.isExtendTimeDialogVisible()
                            + " home=" + home.isDisplayedNow());
                }
            }
            return;
        }

        System.out.println("RENTAL_HOME_DUMP afterOtp=" + afterOtp
                + " qb=" + qb.isDisplayedNow()
                + " home=" + home.isDisplayedNow()
                + " dir=" + DUMP_DIR);
    }

    private void writeDump(String tag, String landing) {
        String xml = safeSource();
        Path dir = DUMP_DIR.resolve(tag);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("landing.txt"),
                    "phone=9000000001\nlanding=" + landing
                            + "\npackage=" + ((AndroidDriver) DriverManager.get()).getCurrentPackage()
                            + "\nhomePageObject=" + new HomePage().isDisplayedNow()
                            + "\nquickBooking=" + new QuickBookingPage().isDisplayedNow()
                            + "\nextendTime=" + new QuickBookingLandingPage().isExtendTimeDialogVisible()
                            + "\ntexts=\n" + String.join("\n", visibleAttr(xml, TEXT))
                            + "\ndescs=\n" + String.join("\n", visibleAttr(xml, DESC))
                            + "\n",
                    StandardCharsets.UTF_8);
            Files.write(dir.resolve("screen.png"),
                    ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write rental home dump " + tag, e);
        }
        System.out.println("RENTAL_HOME_DUMP " + tag + " landing=" + landing);
    }

    private static String safeSource() {
        return DriverManager.get().getPageSource();
    }

    private static String classify(String xml) {
        if (xml.contains("Verify your OTP")) {
            return "otp";
        }
        if (xml.contains("Request to extend time")) {
            return "extend-time";
        }
        if (xml.contains("content-desc=\"Close\"")
                && (xml.contains("Quick Booking") || xml.contains("Quick booking"))) {
            return "quick-booking";
        }
        if (xml.contains("Sign Up Completed")) {
            return "signup-completed";
        }
        boolean fleet = xml.contains("content-desc=\"Fleet\"") || xml.contains(">Fleet<");
        boolean calendar = xml.contains("content-desc=\"Calendar\"") || xml.contains(">Calendar<");
        boolean orders = xml.contains("content-desc=\"Orders\"");
        boolean inventory = xml.contains("content-desc=\"Inventory\"");
        if (fleet || calendar) {
            return "home-rental";
        }
        if (orders || inventory) {
            return "home-material";
        }
        if (xml.contains("Current Earning") || xml.contains("Good Morning!")
                || xml.contains("Good Afternoon!") || xml.contains("Good Evening!")) {
            return "home-or-other";
        }
        return "other";
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
