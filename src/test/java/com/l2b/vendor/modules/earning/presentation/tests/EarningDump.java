package com.l2b.vendor.modules.earning.presentation.tests;

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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/**
 * Dump-0: Rental Earning on {@code 9000000001}. Reach usable Home, open Earning
 * via bottom tab. Probe Help / Withdraw / Transfer / Add Money / Back / scroll —
 * dismiss overlays without confirming money actions.
 */
@Epic("Vendor app")
@Feature("Earning dump — rental vendor 9000000001")
public class EarningDump extends RentalHomeBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-earning-0001-20260926");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Dump-0: Earning & Incentive chrome + action probes")
    @Description("9000000001. Close QB once. Dump Earning via bottom tab, scroll, Help, "
            + "Withdraw/Transfer/Add Money open+Back (no confirm money).")
    public void dumpEarning0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        writeDump("00-home", classifyRentalNow());

        home.tapDesc("Earning");
        sleepQuiet(1000);
        String viaTab = classifyRentalNow();
        Allure.parameter("viaTab", viaTab);
        writeDump("01-earning-tab", viaTab);

        swipeUp();
        sleepQuiet(600);
        writeDump("02-earning-scrolled", classifyRentalNow());

        swipeDown();
        sleepQuiet(500);

        tapTextIfPresent("Help");
        sleepQuiet(900);
        writeDump("03-help-tap", classifyRentalNow());
        dismissToEarning();

        tapTextIfPresent("Withdraw");
        sleepQuiet(900);
        writeDump("04-withdraw-tap", classifyRentalNow());
        dismissToEarning();

        tapTextIfPresent("Transfer");
        sleepQuiet(900);
        writeDump("05-transfer-tap", classifyRentalNow());
        dismissToEarning();

        tapTextIfPresent("Add Money");
        sleepQuiet(900);
        writeDump("06-add-money-tap", classifyRentalNow());
        dismissToEarning();

        // Device Back from Earning
        DriverManager.get().navigate().back();
        sleepQuiet(700);
        writeDump("07-device-back", classifyRentalNow());

        // Re-enter if needed and tap header Back if present
        if (!"earning".equals(classifyRentalNow())) {
            home = new HomePage();
            if ("home".equals(classifyRentalNow()) || home.isDisplayedNow()) {
                home.tapDesc("Earning");
                sleepQuiet(800);
            }
        }
        writeDump("08-earning-again", classifyRentalNow());
        tapDescIfPresent("Back");
        sleepQuiet(700);
        writeDump("09-header-back", classifyRentalNow());

        Allure.addAttachment("dump-dir", "text/plain", DUMP_DIR.toString());
    }

    private void dismissToEarning() {
        for (int i = 0; i < 4; i++) {
            if ("earning".equals(classifyRentalNow())) {
                return;
            }
            DriverManager.get().navigate().back();
            sleepQuiet(500);
        }
        try {
            new HomePage().tapDesc("Earning");
            sleepQuiet(700);
        } catch (RuntimeException ignored) {
            // stay wherever we are for dump
        }
    }

    private void tapTextIfPresent(String text) {
        try {
            var els = DriverManager.get().findElements(
                    By.xpath("//android.widget.TextView[@text='" + text + "']"));
            if (!els.isEmpty()) {
                els.get(0).click();
                Allure.parameter("tappedText", text);
                return;
            }
            var clickable = DriverManager.get().findElements(
                    By.xpath("//*[@clickable='true'][.//*[@text='" + text + "']]"));
            if (!clickable.isEmpty()) {
                clickable.get(0).click();
                Allure.parameter("tappedClickable", text);
            } else {
                Allure.parameter("missingText", text);
            }
        } catch (RuntimeException e) {
            Allure.parameter("tapFail_" + text, e.getClass().getSimpleName());
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
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1600));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(java.time.Duration.ofMillis(350),
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
        swipe.addAction(finger.createPointerMove(java.time.Duration.ofMillis(350),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1600));
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
            throw new IllegalStateException("Earning dump write failed: " + name, e);
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
