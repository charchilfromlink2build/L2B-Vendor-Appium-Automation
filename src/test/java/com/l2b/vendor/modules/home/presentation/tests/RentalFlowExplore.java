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
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
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
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.annotations.Test;

/**
 * Live Rental flow map on {@code 9000000001}. Isolated XML. Does not tap
 * Accept / Decline / View More Details. Header and bottom-tab taps use dump
 * bounds that sit outside the extend-time dialog.
 */
@Epic("Vendor app")
@Feature("Rental flow explore — 9000000001")
public class RentalFlowExplore extends QuickBookingBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-rental-flow-0001-20260921");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Explore rental tabs, notifications, drawer — no consume")
    @Description("9000000001. Close QB once. Tap Calendar/Earning/Fleet/Home by dump bounds "
            + "below the dialog. Tap Profile and Notifications in the header. Back after each. "
            + "Do not tap Accept, Decline, or View More Details.")
    public void exploreRentalChrome() {
        if (!"9000000001".equals(rentalCompanyPhone())) {
            throw new IllegalStateException("Refusing explore: phone is " + rentalCompanyPhone());
        }
        OtpPage otp = openOtp(rentalCompanyPhone());
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
            sleepQuiet(400);
        }
        writeDump("00-after-otp", classify());
        if (qb.isDisplayedNow()) {
            qb.tapClose();
            sleepQuiet(1200);
        }
        writeDump("01-after-close", classify());

        tapDump("02-calendar", 135, 2253);
        pressBack();
        sleepQuiet(600);
        tapDump("03-earning", 675, 2253);
        pressBack();
        sleepQuiet(600);
        tapDump("04-fleet", 945, 2253);
        pressBack();
        sleepQuiet(600);
        tapDump("05-home-tab", 405, 2253);
        tapDump("06-notifications", 991, 158);
        pressBack();
        sleepQuiet(600);
        tapDump("07-profile", 95, 158);

        Allure.parameter("phone", rentalCompanyPhone());
        Allure.parameter("dir", DUMP_DIR.toString());
        Allure.parameter("extendStill", String.valueOf(landing.isExtendTimeDialogVisible()));
        home.attachScreenshot("rental-flow-explore-last");
        System.out.println("RENTAL_FLOW_EXPLORE dir=" + DUMP_DIR
                + " extend=" + landing.isExtendTimeDialogVisible()
                + " pkg=" + vendorPackage());
    }

    @Test(description = "Explore rental bookings + drawer modules — no consume / no logout")
    @Description("9000000001. Close QB. Tap Upcoming See all and first booking card. Tap "
            + "Upcoming/Active/Completed if present. Open each drawer row except Log Out. "
            + "Do not tap Accept, Decline, or View More Details.")
    public void exploreBookingsAndDrawer() {
        if (!"9000000001".equals(rentalCompanyPhone())) {
            throw new IllegalStateException("Refusing explore: phone is " + rentalCompanyPhone());
        }
        OtpPage otp = openOtp(rentalCompanyPhone());
        otp.focusOtpField();
        if (!otp.otpFieldText().isEmpty()) {
            otp.clearOtp();
        }
        otp.pressDigitKeys("1234");
        QuickBookingPage qb = new QuickBookingPage();
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            if (qb.isDisplayedNow() || new HomePage().isDisplayedNow()
                    || new QuickBookingLandingPage().isExtendTimeDialogVisible()) {
                break;
            }
            sleepQuiet(400);
        }
        if (qb.isDisplayedNow()) {
            qb.tapClose();
            sleepQuiet(1200);
        }
        writeDump("10-home", classify());

        tapDump("11-see-all-upcoming", 958, 912);
        tapTextIfPresent("Active");
        writeDump("12-bookings-active", classify());
        tapTextIfPresent("Completed");
        writeDump("13-bookings-completed", classify());
        tapTextIfPresent("Upcoming");
        writeDump("14-bookings-upcoming", classify());
        pressBack();
        sleepQuiet(800);
        writeDump("15-after-bookings-back", classify());

        tapDump("16-first-upcoming-card", 465, 1198);
        pressBack();
        sleepQuiet(800);

        tapDump("17-see-all-booking-orders", 958, 1537);
        pressBack();
        sleepQuiet(800);

        String[] drawerRows = {
                "Account", "KYC", "Your machines", "Manage team", "Help",
                "Language", "Refer & Earn", "FAQ", "Terms & Services", "Policies", "Settings"
        };
        int i = 18;
        for (String row : drawerRows) {
            tapDump("d-profile-" + i, 95, 158);
            tapTextIfPresent(row);
            sleepQuiet(900);
            writeDump("d-" + i + "-" + row.replace(' ', '-').replace('&', 'n'), classify());
            pressBack();
            sleepQuiet(700);
            i++;
        }
        new HomePage().attachScreenshot("rental-flow-drawer-last");
    }

    private void tapTextIfPresent(String text) {
        try {
            var els = DriverManager.get().findElements(
                    com.l2b.vendor.core.locators.ComposeLocators.textView(text));
            if (!els.isEmpty()) {
                DriverManager.get().executeScript("mobile: clickGesture", java.util.Map.of(
                        "elementId", ((org.openqa.selenium.remote.RemoteWebElement) els.get(0)).getId()));
                sleepQuiet(800);
            }
        } catch (RuntimeException e) {
            System.out.println("RENTAL_FLOW_EXPLORE tapText miss " + text + " " + e.getMessage());
        }
    }

    private void tapDump(String tag, int x, int y) {
        DriverManager.get().executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
        sleepQuiet(1200);
        writeDump(tag, classify());
    }

    private void pressBack() {
        ((AndroidDriver) DriverManager.get()).pressKey(new KeyEvent(AndroidKey.BACK));
    }

    private void writeDump(String tag, String landing) {
        String xml = DriverManager.get().getPageSource();
        Path dir = DUMP_DIR.resolve(tag);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("landing.txt"),
                    "phone=9000000001\nstep=" + tag + "\nlanding=" + landing
                            + "\npackage=" + vendorPackage()
                            + "\nextendTime=" + new QuickBookingLandingPage().isExtendTimeDialogVisible()
                            + "\nhome=" + new HomePage().isDisplayedNow()
                            + "\nqb=" + new QuickBookingPage().isDisplayedNow()
                            + "\ntexts=\n" + String.join("\n", visibleAttr(xml, TEXT))
                            + "\ndescs=\n" + String.join("\n", visibleAttr(xml, DESC))
                            + "\n",
                    StandardCharsets.UTF_8);
            Files.write(dir.resolve("screen.png"),
                    ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
        } catch (IOException e) {
            throw new IllegalStateException("Could not write explore dump " + tag, e);
        }
        System.out.println("RENTAL_FLOW_EXPLORE " + tag + " landing=" + landing
                + " pkg=" + vendorPackage());
    }

    private static String classify() {
        String xml = DriverManager.get().getPageSource();
        if (xml.contains("Request to extend time")) {
            return "extend-time";
        }
        if (xml.contains("content-desc=\"Close\"") && xml.contains("Quick Booking")) {
            return "quick-booking";
        }
        if (xml.contains("content-desc=\"Fleet\"") || xml.contains(">Fleet<")) {
            if (xml.contains("Your Fleet") || xml.contains("No Machines")) {
                return "fleet";
            }
        }
        if (xml.contains("Schedule") && xml.contains("Calendar")) {
            return "calendar";
        }
        if (xml.contains("Earning") && (xml.contains("Withdraw") || xml.contains("Incentive"))) {
            return "earning";
        }
        if (xml.contains("Upcoming") && xml.contains("Active") && xml.contains("Completed")) {
            return "bookings";
        }
        if (xml.contains("Account") && (xml.contains("Log Out") || xml.contains("Logout"))) {
            return "drawer";
        }
        if (xml.contains("Notifications") && !xml.contains("Current Earning")) {
            return "notifications";
        }
        if (xml.contains("Current Earning") || xml.contains("Good Morning")
                || xml.contains("Good Afternoon") || xml.contains("Good Evening")) {
            return "home";
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
