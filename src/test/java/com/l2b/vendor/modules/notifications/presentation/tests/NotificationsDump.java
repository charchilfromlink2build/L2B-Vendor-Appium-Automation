package com.l2b.vendor.modules.notifications.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;
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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/**
 * Dump-0: Notifications (Home bell) on {@code 9000000001}. Unread/All tabs,
 * empty copy, list rows, View Details, Back. Read-only — no Accept/Decline/Logout.
 */
@Epic("Vendor app")
@Feature("Notifications dump — rental vendor 9000000001")
public class NotificationsDump extends RentalHomeBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-notifications-0001-20260928");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Dump-0: Notification chrome + Unread/All + View Details + Back")
    @Description("9000000001. Home bell → Notification. Dump Unread empty, All list, "
            + "View all / See all / View Details probes, scroll, Back, layout bounds.")
    public void dumpNotifications0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        writeDump("00-home", classifyRentalNow());

        home.tapDesc("Notifications");
        sleepQuiet(1100);
        Allure.parameter("viaBell", classifyRentalNow());
        writeDump("01-bell-unread", classifyRentalNow());
        writeBounds("01-bell-unread");

        // Unread tab (default) — empty copy / View all
        tapTextIfPresent("Unread");
        sleepQuiet(700);
        writeDump("02-unread-tab", classifyRentalNow());

        tapTextIfPresent("View all notifications");
        sleepQuiet(900);
        writeDump("03-view-all-notifications", classifyRentalNow());

        // All tab
        tapTextIfPresent("All");
        sleepQuiet(900);
        writeDump("04-all-tab", classifyRentalNow());
        writeBounds("04-all-tab");

        swipeUp();
        sleepQuiet(500);
        writeDump("05-all-scrolled", classifyRentalNow());

        // Category See all
        tapTextIfPresent("See all");
        sleepQuiet(900);
        writeDump("06-see-all", classifyRentalNow());
        dismissTowardNotifications();

        // Ensure All again
        if (!"notifications".equals(classifyRentalNow())) {
            try {
                new HomePage().tapDesc("Notifications");
                sleepQuiet(900);
            } catch (RuntimeException ignored) {
            }
        }
        tapTextIfPresent("All");
        sleepQuiet(700);
        writeDump("07-all-again", classifyRentalNow());

        // View Details on first row
        tapTextIfPresent("View Details");
        sleepQuiet(1100);
        writeDump("08-view-details", classifyRentalNow());
        writeBounds("08-view-details");

        DriverManager.get().navigate().back();
        sleepQuiet(800);
        writeDump("09-back-from-details", classifyRentalNow());

        // Mark as read / Mark all if present (probe only)
        tapTextIfPresent("Mark all as read");
        sleepQuiet(600);
        tapTextIfPresent("Mark as read");
        sleepQuiet(600);
        writeDump("10-mark-read-probe", classifyRentalNow());

        // Re-tap Unread
        tapTextIfPresent("Unread");
        sleepQuiet(700);
        writeDump("11-unread-again", classifyRentalNow());

        tapTextIfPresent("All");
        sleepQuiet(600);

        // Header Back
        tapDescIfPresent("Back");
        sleepQuiet(800);
        writeDump("12-header-back", classifyRentalNow());

        // Re-open and device Back
        if (!"notifications".equals(classifyRentalNow())) {
            try {
                new HomePage().tapDesc("Notifications");
                sleepQuiet(900);
            } catch (RuntimeException ignored) {
            }
        }
        writeDump("13-reopen", classifyRentalNow());
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        writeDump("14-device-back", classifyRentalNow());

        // Double open for layout
        try {
            new HomePage().tapDesc("Notifications");
            sleepQuiet(900);
            writeDump("15-final", classifyRentalNow());
            writeBounds("15-final");
        } catch (RuntimeException e) {
            Allure.parameter("finalOpenFail", e.getClass().getSimpleName());
        }

        Allure.addAttachment("dump-dir", "text/plain", DUMP_DIR.toString());
    }

    private void dismissTowardNotifications() {
        for (int i = 0; i < 4; i++) {
            if ("notifications".equals(classifyRentalNow())) {
                return;
            }
            if ("home".equals(classifyRentalNow()) || "extend-time".equals(classifyRentalNow())) {
                try {
                    new HomePage().tapDesc("Notifications");
                    sleepQuiet(800);
                } catch (RuntimeException ignored) {
                }
                return;
            }
            DriverManager.get().navigate().back();
            sleepQuiet(500);
        }
    }

    private void tapTextIfPresent(String text) {
        try {
            var els = DriverManager.get().findElements(
                    By.xpath("//android.widget.TextView[@text='" + text + "']"));
            if (els.isEmpty()) {
                els = DriverManager.get().findElements(
                        By.xpath("//*[contains(@text,'" + text + "')]"));
            }
            if (!els.isEmpty()) {
                els.get(0).click();
                Allure.parameter("tappedText", text);
            }
        } catch (RuntimeException e) {
            Allure.parameter("tapTextFail_" + text, e.getClass().getSimpleName());
        }
    }

    private void tapDescIfPresent(String desc) {
        try {
            var els = DriverManager.get().findElements(By.xpath("//*[@content-desc='" + desc + "']"));
            if (!els.isEmpty()) {
                els.get(0).click();
                Allure.parameter("tappedDesc", desc);
            }
        } catch (RuntimeException e) {
            Allure.parameter("tapDescFail_" + desc, e.getClass().getSimpleName());
        }
    }

    private void swipeUp() {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1700));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(java.time.Duration.ofMillis(350),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 900));
        swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        DriverManager.get().perform(java.util.List.of(swipe));
    }

    private void writeDump(String name, String classified) {
        Path dir = DUMP_DIR.resolve(name);
        try {
            Files.createDirectories(dir);
            String xml = safeSource();
            Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("texts.txt"), extract(xml), StandardCharsets.UTF_8);
            Files.writeString(dir.resolve("classified.txt"), classified, StandardCharsets.UTF_8);
            byte[] png = ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES);
            Files.write(dir.resolve("screen.png"), png);
            Allure.addAttachment(name + "-screen", "image/png",
                    new java.io.ByteArrayInputStream(png), ".png");
            Allure.addAttachment(name + "-texts", "text/plain", extract(xml));
        } catch (IOException e) {
            throw new IllegalStateException("Notifications dump write failed: " + name, e);
        }
    }

    private void writeBounds(String name) {
        Path dir = DUMP_DIR.resolve(name);
        try {
            Files.createDirectories(dir);
            List<String> lines = new ArrayList<>();
            for (WebElement el : DriverManager.get().findElements(By.className("android.widget.TextView"))) {
                String t = el.getAttribute("text");
                if (t == null || t.isBlank() || t.length() > 100) {
                    continue;
                }
                if (t.equals("Notification") || t.equals("Unread") || t.equals("All")
                        || t.contains("caught up") || t.contains("View all")
                        || t.equals("See all") || t.equals("View Details")
                        || t.equals("Rentals") || t.contains("booking")
                        || t.contains("ago") || t.contains("AM") || t.contains("PM")) {
                    Rectangle r = el.getRect();
                    lines.add(t + " bounds=[" + r.x + "," + r.y + "][" + (r.x + r.width) + ","
                            + (r.y + r.height) + "] w=" + r.width + " h=" + r.height);
                }
            }
            for (String desc : List.of("Back", "Notifications", "Home", "Calendar", "Earning", "Fleet")) {
                var els = DriverManager.get().findElements(By.xpath("//*[@content-desc='" + desc + "']"));
                if (!els.isEmpty()) {
                    Rectangle r = els.get(0).getRect();
                    lines.add("desc:" + desc + " bounds=[" + r.x + "," + r.y + "]["
                            + (r.x + r.width) + "," + (r.y + r.height) + "]");
                }
            }
            Files.writeString(dir.resolve("bounds.txt"), String.join("\n", lines), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Allure.parameter("boundsFail_" + name, e.getClass().getSimpleName());
        }
    }

    private static String extract(String xml) {
        Set<String> out = new LinkedHashSet<>();
        Matcher t = TEXT.matcher(xml);
        while (t.find()) {
            out.add("text=" + t.group(1));
        }
        Matcher d = DESC.matcher(xml);
        while (d.find()) {
            out.add("desc=" + d.group(1));
        }
        return String.join("\n", out);
    }

    private String safeSource() {
        try {
            return ((AndroidDriver) DriverManager.get()).getPageSource();
        } catch (Exception e) {
            return "";
        }
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
