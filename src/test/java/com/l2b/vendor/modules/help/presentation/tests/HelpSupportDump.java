package com.l2b.vendor.modules.help.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
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
 * Dump-0: Help & Support from Profile drawer on {@code 9000000001}.
 * Probe Live chat / Raise Ticket / Ticket history — Back only.
 * Never Accept / Decline / Log Out Confirm / submit ticket.
 */
@Epic("Vendor app")
@Feature("Help & Support dump — rental vendor 9000000001")
public class HelpSupportDump extends RentalHomeBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-help-0001-20261001");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,200})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Dump-0: Help & Support chrome + Live chat / Raise Ticket / history probes")
    @Description("9000000001. Profile → Help. Dump landing, Live chat, Raise Ticket (no submit), "
            + "Ticket history. Header/device Back only.")
    public void dumpHelpSupport0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        writeDump("00-home", classifyRentalNow());

        home.tapDesc("Profile");
        sleepQuiet(1000);
        writeDump("01-drawer", classifyRentalNow());

        new ProfileDrawerPage().tapRow("Help");
        sleepQuiet(1200);
        writeDump("02-help-landing", classifyRentalNow());
        writeBounds("02-help-landing");

        swipeUp();
        sleepQuiet(500);
        writeDump("03-help-scrolled", classifyRentalNow());
        writeBounds("03-help-scrolled");
        swipeDown();
        sleepQuiet(400);

        // Live chat probe
        if (tapTextIfPresent("Live chat") || tapTextIfPresent("Live Chat")) {
            sleepQuiet(1200);
            writeDump("04-live-chat", classifyRentalNow());
            writeBounds("04-live-chat");
            dismissTowardHelp();
            sleepQuiet(900);
            writeDump("05-after-live-chat", classifyRentalNow());
        }

        ensureHelpLanding();
        // Raise Ticket — open form only, never submit
        if (tapTextIfPresent("Raise Ticket") || tapTextIfPresent("Raise ticket")) {
            sleepQuiet(1200);
            writeDump("06-raise-ticket", classifyRentalNow());
            writeBounds("06-raise-ticket");
            dismissTowardHelp();
            sleepQuiet(900);
            writeDump("07-after-raise-ticket", classifyRentalNow());
        }

        ensureHelpLanding();
        if (tapTextIfPresent("Ticket history") || tapTextIfPresent("Ticket History")) {
            sleepQuiet(1200);
            writeDump("08-ticket-history", classifyRentalNow());
            writeBounds("08-ticket-history");
            dismissTowardHelp();
            sleepQuiet(900);
            writeDump("09-after-ticket-history", classifyRentalNow());
        }

        ensureHelpLanding();
        writeDump("10-help-again", classifyRentalNow());

        // Header Back
        tapDescIfPresent("Back");
        sleepQuiet(900);
        writeDump("11-header-back", classifyRentalNow());

        // Re-open Help
        if (!profileDrawerNow()) {
            if (!"home".equals(classifyRentalNow())) {
                DriverManager.get().navigate().back();
                sleepQuiet(700);
            }
            new HomePage().tapDesc("Profile");
            sleepQuiet(900);
        }
        new ProfileDrawerPage().tapRow("Help");
        sleepQuiet(1100);
        writeDump("12-help-reopen", classifyRentalNow());

        DriverManager.get().navigate().back();
        sleepQuiet(900);
        writeDump("13-device-back", classifyRentalNow());

        Allure.addAttachment("dump-dir", "text/plain", DUMP_DIR.toString());
    }

    private void ensureHelpLanding() {
        for (int i = 0; i < 5; i++) {
            if (isTextPresent("Help & Support") || isTextPresent("Help and Support")
                    || (isTextPresent("Live chat") || isTextPresent("Live Chat"))
                    && (isTextPresent("Raise Ticket") || isTextPresent("Raise ticket"))) {
                return;
            }
            if (profileDrawerNow()) {
                new ProfileDrawerPage().tapRow("Help");
                sleepQuiet(1000);
                return;
            }
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
    }

    private void dismissTowardHelp() {
        if (isTextPresent("Cancel")) {
            tapTextIfPresent("Cancel");
            sleepQuiet(600);
        }
        if (isDescPresent("Close") || isTextPresent("Close")) {
            tapDescIfPresent("Close");
            tapTextIfPresent("Close");
            sleepQuiet(600);
        }
        if (isDescPresent("Back")) {
            tapDescIfPresent("Back");
            sleepQuiet(700);
        } else {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
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
        swipe.addAction(finger.createPointerMove(java.time.Duration.ofMillis(400),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 700));
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
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 700));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(java.time.Duration.ofMillis(400),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1700));
        swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        DriverManager.get().perform(java.util.List.of(swipe));
    }

    private boolean isTextPresent(String text) {
        return !DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[@text='" + text + "']")).isEmpty()
                || !DriverManager.get().findElements(
                By.xpath("//*[contains(@text,'" + text + "')]")).isEmpty();
    }

    private boolean isDescPresent(String desc) {
        return !DriverManager.get().findElements(
                By.xpath("//*[@content-desc='" + desc + "']")).isEmpty();
    }

    private boolean tapTextIfPresent(String text) {
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
                return true;
            }
        } catch (RuntimeException e) {
            Allure.parameter("tapTextFail_" + text, e.getClass().getSimpleName());
        }
        return false;
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
            throw new IllegalStateException("Help dump write failed: " + name, e);
        }
    }

    private void writeBounds(String name) {
        Path dir = DUMP_DIR.resolve(name);
        try {
            Files.createDirectories(dir);
            List<String> lines = new ArrayList<>();
            for (WebElement el : DriverManager.get().findElements(By.className("android.widget.TextView"))) {
                String t = el.getAttribute("text");
                if (t == null || t.isBlank() || t.length() > 120) {
                    continue;
                }
                Rectangle r = el.getRect();
                lines.add(t + " bounds=[" + r.x + "," + r.y + "][" + (r.x + r.width) + ","
                        + (r.y + r.height) + "] w=" + r.width + " h=" + r.height);
            }
            for (String desc : List.of("Back", "Close", "Help", "Live chat")) {
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
