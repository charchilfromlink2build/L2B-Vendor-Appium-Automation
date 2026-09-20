package com.l2b.vendor.modules.quickbooking.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.locators.ComposeLocators;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/**
 * Dump-only: Material vendor {@code 9000000017} Quick Booking. Does not tap Accept or Decline.
 * Isolated {@code quickbooking/material-landing-dump.xml}. Not a product case.
 */
@Epic("Vendor app")
@Feature("Quick Booking — material vendor dump")
public class MaterialQuickBookingDump extends QuickBookingBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-qb-material-0017-20260919");
    private static final Pattern BOOKING_NO = Pattern.compile("L2B-RNT-[A-Z0-9-]+|MAT-[0-9-]+|RB-[A-Z0-9-]+");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Dump 9000000017 Material Quick Booking landing — no Accept/Decline")
    @Description("Cold start, OTP 1234 on 9000000017. Dump collapsed, View More, scrolled. "
            + "Do not tap Accept or Decline.")
    public void dumpMaterialLanding0017() {
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

        QuickBookingPage page = new QuickBookingPage();
        HomePage home = new HomePage();
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            if (page.isDisplayedNow() || home.isDisplayedNow()) {
                break;
            }
            if (otp.isDisplayedNow() && otp.otpFieldText().isEmpty()) {
                otp.pasteOtp("1234");
            }
            sleepQuiet(400);
        }

        String landing = classify(safeSource());
        writeDump("01-landing", landing);
        Allure.parameter("phone", phone);
        Allure.parameter("landing", landing);
        Allure.parameter("quickBooking", String.valueOf(page.isDisplayedNow()));
        Allure.parameter("home", String.valueOf(home.isDisplayedNow()));
        page.attachScreenshot("material-0017-01-landing");

        if (!page.isDisplayedNow()) {
            System.out.println("MATERIAL_DUMP landing=" + landing + " — no Quick Booking, stop");
            return;
        }

        if (page.isViewMoreDetailsVisible()) {
            List<WebElement> more = DriverManager.get().findElements(
                    ComposeLocators.textView("View More Details"));
            if (!more.isEmpty()) {
                more.get(0).click();
                sleepQuiet(1500);
            }
            writeDump("02-view-more", classify(safeSource()));
            page.attachScreenshot("material-0017-02-view-more");
        }

        swipeListUp();
        swipeListUp();
        sleepQuiet(800);
        writeDump("03-scrolled", classify(safeSource()));
        page.attachScreenshot("material-0017-03-scrolled");
        System.out.println("MATERIAL_DUMP done dir=" + DUMP_DIR);
    }

    private void swipeListUp() {
        Dimension size = DriverManager.get().manage().window().getSize();
        DriverManager.get().executeScript("mobile: swipeGesture", Map.of(
                "left", (int) (size.width * 0.15),
                "top", (int) (size.height * 0.35),
                "width", (int) (size.width * 0.70),
                "height", (int) (size.height * 0.40),
                "direction", "up",
                "percent", 0.75));
    }

    private void writeDump(String tag, String landing) {
        String xml = safeSource();
        Path dir = DUMP_DIR.resolve(tag);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("landing.txt"),
                    "phone=9000000017\nlanding=" + landing
                            + "\npackage=" + ((AndroidDriver) DriverManager.get()).getCurrentPackage()
                            + "\ntexts=\n" + String.join("\n", visibleTexts(xml))
                            + "\nids=" + bookingIds(xml) + "\n",
                    StandardCharsets.UTF_8);
            Files.write(dir.resolve("screen.png"),
                    ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write material dump " + tag, e);
        }
        System.out.println("MATERIAL_DUMP " + tag + " landing=" + landing);
    }

    private static String safeSource() {
        return DriverManager.get().getPageSource();
    }

    private static String classify(String xml) {
        if (xml.contains("Verify your OTP")) {
            return "otp";
        }
        if (xml.contains("Quick Booking") || xml.contains("Quick booking")) {
            return "quick-booking";
        }
        if (xml.contains("Good Morning") || xml.contains("Current Earning")
                || xml.contains("My Bookings") || xml.contains("My Machines")) {
            return "home";
        }
        return "unknown";
    }

    private static Set<String> visibleTexts(String xml) {
        Matcher m = Pattern.compile("text=\"([^\"]{1,120})\"").matcher(xml);
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) {
            String t = m.group(1);
            if (!t.startsWith("http") && !t.contains("com.android")) {
                out.add(t);
            }
        }
        return out;
    }

    private static Set<String> bookingIds(String xml) {
        Matcher m = BOOKING_NO.matcher(xml);
        Set<String> out = new LinkedHashSet<>();
        while (m.find()) {
            out.add(m.group());
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
