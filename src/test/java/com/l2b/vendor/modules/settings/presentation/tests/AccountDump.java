package com.l2b.vendor.modules.settings.presentation.tests;

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
 * Dump-1: Account / Profile Info from Profile drawer on {@code 9000000001}.
 * Never Accept / Decline / Log Out Confirm.
 */
@Epic("Vendor app")
@Feature("Account dump — rental vendor 9000000001")
public class AccountDump extends RentalHomeBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-account-0001-20260929");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Dump-1: Account / Profile Info chrome + Edit/Update probes")
    @Description("9000000001. Profile → Account. Dump Profile Info, Edit, Update, Back. "
            + "No destructive save if avoidable.")
    public void dumpAccount0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        writeDump("00-home", classifyRentalNow());

        home.tapDesc("Profile");
        sleepQuiet(1000);
        writeDump("01-drawer", classifyRentalNow());

        new ProfileDrawerPage().tapRow("Account");
        sleepQuiet(1200);
        writeDump("02-account", classifyRentalNow());
        writeBounds("02-account");

        // Scroll if long form
        swipeUp();
        sleepQuiet(500);
        writeDump("03-account-scrolled", classifyRentalNow());
        writeBounds("03-account-scrolled");

        swipeDown();
        sleepQuiet(500);

        // Edit probe
        tapTextIfPresent("Edit");
        sleepQuiet(1000);
        writeDump("04-edit", classifyRentalNow());
        writeBounds("04-edit");

        // Back / Cancel from Edit if opened
        if (isDescPresent("Back") || isTextPresent("Cancel")) {
            if (isTextPresent("Cancel")) {
                tapTextIfPresent("Cancel");
            } else {
                DriverManager.get().navigate().back();
            }
            sleepQuiet(900);
        }
        writeDump("05-after-edit-dismiss", classifyRentalNow());

        // Update probe (Company Info) — do not confirm destructive
        tapTextIfPresent("Update");
        sleepQuiet(1000);
        writeDump("06-update", classifyRentalNow());
        writeBounds("06-update");

        if (isTextPresent("Cancel") || isDescPresent("Back") || isTextPresent("Close")) {
            if (isTextPresent("Cancel")) {
                tapTextIfPresent("Cancel");
            } else if (isTextPresent("Close")) {
                tapTextIfPresent("Close");
            } else {
                DriverManager.get().navigate().back();
            }
            sleepQuiet(900);
        }
        writeDump("07-after-update-dismiss", classifyRentalNow());

        // Profile photo
        tapDescIfPresent("Profile photo");
        sleepQuiet(1000);
        writeDump("08-profile-photo", classifyRentalNow());
        if (!isTextPresent("Profile Info") && !isTextPresent("Full Name")) {
            DriverManager.get().navigate().back();
            sleepQuiet(800);
        }
        writeDump("09-after-photo", classifyRentalNow());

        // Header Back → drawer or home
        tapDescIfPresent("Back");
        sleepQuiet(900);
        writeDump("10-header-back", classifyRentalNow());

        if (!profileDrawerNow() && !"home".equals(classifyRentalNow())) {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        if (!profileDrawerNow()) {
            new HomePage().tapDesc("Profile");
            sleepQuiet(900);
            new ProfileDrawerPage().tapRow("Account");
            sleepQuiet(1000);
        } else {
            new ProfileDrawerPage().tapRow("Account");
            sleepQuiet(1000);
        }
        writeDump("11-account-again", classifyRentalNow());

        DriverManager.get().navigate().back();
        sleepQuiet(900);
        writeDump("12-device-back", classifyRentalNow());

        Allure.addAttachment("dump-dir", "text/plain", DUMP_DIR.toString());
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
                By.xpath("//android.widget.TextView[@text='" + text + "']")).isEmpty();
    }

    private boolean isDescPresent(String desc) {
        return !DriverManager.get().findElements(
                By.xpath("//*[@content-desc='" + desc + "']")).isEmpty();
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
            throw new IllegalStateException("Account dump write failed: " + name, e);
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
                Rectangle r = el.getRect();
                lines.add(t + " bounds=[" + r.x + "," + r.y + "][" + (r.x + r.width) + ","
                        + (r.y + r.height) + "]");
            }
            for (String desc : List.of("Back", "Profile photo", "Close", "Edit", "Update")) {
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
