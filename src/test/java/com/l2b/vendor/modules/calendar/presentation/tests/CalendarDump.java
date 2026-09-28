package com.l2b.vendor.modules.calendar.presentation.tests;

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
 * Dump-0: Rental Schedule/Calendar on {@code 9000000001}. Month grid, day agenda,
 * empty day, booked day, month nav, Back/tabs. Read-only — no Accept/Decline/Logout.
 */
@Epic("Vendor app")
@Feature("Calendar dump — rental vendor 9000000001")
public class CalendarDump extends RentalHomeBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-calendar-0001-20260928");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");
    private static final Pattern BOUNDS = Pattern.compile(
            "bounds=\"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Dump-0: Schedule chrome + month/day probes + layout bounds")
    @Description("9000000001. Calendar tab → Schedule. Dump empty/booked days, prev/next month, "
            + "day select, agenda scroll, Back, layout bounds for padding/alignment QA.")
    public void dumpCalendar0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        writeDump("00-home", classifyRentalNow());

        home.tapDesc("Calendar");
        sleepQuiet(1000);
        Allure.parameter("viaTab", classifyRentalNow());
        writeDump("01-calendar-tab", classifyRentalNow());
        writeBounds("01-calendar-tab");

        // Try known empty day (21) then a mid-month day
        tapDayIfPresent("21");
        sleepQuiet(700);
        writeDump("02-day-21", classifyRentalNow());

        tapDayIfPresent("25");
        sleepQuiet(800);
        writeDump("03-day-25", classifyRentalNow());
        writeBounds("03-day-25");

        // Today-ish: 28 Sep 2026
        tapDayIfPresent("28");
        sleepQuiet(700);
        writeDump("04-day-28", classifyRentalNow());

        // Scroll agenda if present
        swipeUp();
        sleepQuiet(500);
        writeDump("05-agenda-scrolled", classifyRentalNow());

        swipeDown();
        sleepQuiet(400);

        // Previous month
        tapDescIfPresent("Previous month");
        if (!tappedDescRecently()) {
            tapDescIfPresent("Previous");
        }
        // Also try left chevron by content-desc from dump
        sleepQuiet(800);
        writeDump("06-prev-month", classifyRentalNow());

        tapDescIfPresent("Next month");
        sleepQuiet(700);
        writeDump("07-next-month-back", classifyRentalNow());

        // Next month ahead
        tapDescIfPresent("Next month");
        sleepQuiet(700);
        writeDump("08-next-month", classifyRentalNow());

        // Return to current-ish
        tapDescIfPresent("Previous month");
        sleepQuiet(500);

        // Tap a machine card / Start Task if visible (read-only probe)
        tapTextIfPresent("Start Task");
        sleepQuiet(900);
        writeDump("09-start-task-or-card", classifyRentalNow());
        dismissToCalendar();

        tapTextIfPresent("Completed");
        sleepQuiet(600);
        writeDump("10-completed-probe", classifyRentalNow());
        dismissToCalendar();

        // Bottom tabs still
        writeDump("11-before-tabs", classifyRentalNow());
        home = new HomePage();
        try {
            home.tapDesc("Home");
            sleepQuiet(700);
            writeDump("12-home-tab", classifyRentalNow());
            home.tapDesc("Calendar");
            sleepQuiet(700);
        } catch (RuntimeException e) {
            Allure.parameter("tabProbe", e.getClass().getSimpleName());
        }
        writeDump("13-calendar-again", classifyRentalNow());

        DriverManager.get().navigate().back();
        sleepQuiet(700);
        writeDump("14-device-back", classifyRentalNow());

        if (!"calendar".equals(classifyRentalNow())) {
            new HomePage().tapDesc("Calendar");
            sleepQuiet(700);
        }
        tapDescIfPresent("Back");
        sleepQuiet(700);
        writeDump("15-header-back", classifyRentalNow());

        Allure.addAttachment("dump-dir", "text/plain", DUMP_DIR.toString());
    }

    private boolean lastDescTapOk;

    private boolean tappedDescRecently() {
        return lastDescTapOk;
    }

    private void dismissToCalendar() {
        for (int i = 0; i < 4; i++) {
            if ("calendar".equals(classifyRentalNow())) {
                return;
            }
            DriverManager.get().navigate().back();
            sleepQuiet(500);
        }
        try {
            new HomePage().tapDesc("Calendar");
            sleepQuiet(700);
        } catch (RuntimeException ignored) {
        }
    }

    private void tapDayIfPresent(String day) {
        // Prefer exact day number TextView that looks like a grid cell (short text)
        List<WebElement> days = DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[@text='" + day + "']"));
        if (days.isEmpty()) {
            Allure.parameter("missingDay", day);
            return;
        }
        // Prefer the one in upper half of screen (month grid, not agenda date copy)
        WebElement best = days.get(0);
        int bestY = Integer.MAX_VALUE;
        for (WebElement el : days) {
            try {
                Rectangle r = el.getRect();
                if (r.y < 1400 && r.y < bestY && r.width < 200) {
                    bestY = r.y;
                    best = el;
                }
            } catch (RuntimeException ignored) {
            }
        }
        try {
            best.click();
            Allure.parameter("tappedDay", day);
        } catch (RuntimeException e) {
            Allure.parameter("dayTapFail", day + ":" + e.getClass().getSimpleName());
        }
    }

    private void tapTextIfPresent(String text) {
        try {
            var els = DriverManager.get().findElements(
                    By.xpath("//android.widget.TextView[@text='" + text + "']"));
            if (!els.isEmpty()) {
                els.get(0).click();
                Allure.parameter("tappedText", text);
            }
        } catch (RuntimeException e) {
            Allure.parameter("tapTextFail_" + text, e.getClass().getSimpleName());
        }
    }

    private void tapDescIfPresent(String desc) {
        lastDescTapOk = false;
        try {
            var els = DriverManager.get().findElements(By.xpath("//*[@content-desc='" + desc + "']"));
            if (!els.isEmpty()) {
                els.get(0).click();
                lastDescTapOk = true;
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

    private void swipeDown() {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 900));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(java.time.Duration.ofMillis(350),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1700));
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
            throw new IllegalStateException("Calendar dump write failed: " + name, e);
        }
    }

    private void writeBounds(String name) {
        Path dir = DUMP_DIR.resolve(name);
        try {
            Files.createDirectories(dir);
            List<String> lines = new ArrayList<>();
            for (WebElement el : DriverManager.get().findElements(By.className("android.widget.TextView"))) {
                String t = el.getAttribute("text");
                if (t == null || t.isBlank() || t.length() > 80) {
                    continue;
                }
                if (t.equals("Schedule") || t.equals("September") || t.equals("2026")
                        || t.contains("September") || t.contains("No bookings")
                        || t.equals("Calendar") || t.equals("Home") || t.equals("Earning")
                        || t.equals("Fleet") || t.matches("\\d{1,2}")) {
                    Rectangle r = el.getRect();
                    lines.add(t + " bounds=[" + r.x + "," + r.y + "][" + (r.x + r.width) + ","
                            + (r.y + r.height) + "] w=" + r.width + " h=" + r.height);
                }
            }
            for (String desc : List.of("Back", "Previous month", "Next month", "Calendar", "Home",
                    "Earning", "Fleet")) {
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
