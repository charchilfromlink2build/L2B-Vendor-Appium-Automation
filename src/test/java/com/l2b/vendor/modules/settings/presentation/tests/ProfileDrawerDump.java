package com.l2b.vendor.modules.settings.presentation.tests;

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
 * Dump-0: Profile drawer chrome on {@code 9000000001}. Open/close, all rows
 * visible, scroll, Log Out dialog Cancel only. Never Confirm Log Out /
 * Accept / Decline.
 */
@Epic("Vendor app")
@Feature("Profile drawer dump — rental vendor 9000000001")
public class ProfileDrawerDump extends RentalHomeBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-profile-drawer-0001-20260929");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,160})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    /** Labels from RentalFlowExplore live dump; dump also probes alt spellings. */
    private static final String[] ROWS = {
            "Account",
            "KYC",
            "Your machines",
            "Manage team",
            "Help",
            "Language",
            "Refer & Earn",
            "FAQ",
            "Terms & Services",
            "Policies",
            "Settings",
            "Log Out"
    };

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Dump-0: Profile drawer chrome + rows + close + Log Out Cancel")
    @Description("9000000001. Profile → drawer. Dump all rows, scroll, outside/Back dismiss, "
            + "Log Out dialog Cancel only. Never Confirm.")
    public void dumpProfileDrawer0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        writeDump("00-home", classifyRentalNow());
        writeBounds("00-home");

        home.tapDesc("Profile");
        sleepQuiet(1100);
        Allure.parameter("afterProfile", classifyRentalNow());
        writeDump("01-drawer-open", classifyRentalNow());
        writeBounds("01-drawer-open");

        // Scroll drawer to reveal lower rows
        swipeDrawerUp();
        sleepQuiet(600);
        writeDump("02-drawer-scrolled", classifyRentalNow());
        writeBounds("02-drawer-scrolled");

        swipeDrawerDown();
        sleepQuiet(500);
        writeDump("03-drawer-top-again", classifyRentalNow());

        // Probe each row label presence without navigating (except Log Out dialog)
        for (String row : ROWS) {
            boolean present = isTextPresent(row) || isTextPresent(row.replace(" and ", " & "))
                    || isTextContains(row.split(" ")[0]);
            Allure.parameter("row_" + row.replace(' ', '_'), String.valueOf(present));
        }

        // Device Back dismisses drawer
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        writeDump("04-device-back", classifyRentalNow());

        // Re-open
        new HomePage().tapDesc("Profile");
        sleepQuiet(1000);
        writeDump("05-reopen", classifyRentalNow());

        // Close via scrim (right strip content-desc Close navigation menu)
        tapDescIfPresent("Close navigation menu");
        sleepQuiet(900);
        writeDump("06-close-menu-desc", classifyRentalNow());

        if (!profileDrawerNow()) {
            new HomePage().tapDesc("Profile");
            sleepQuiet(900);
        }
        writeDump("07-reopen-for-scrim", classifyRentalNow());

        // Outside / scrim tap — drawer is LEFT; dimmed area is RIGHT (~x 950+)
        tapOutsideDrawer();
        sleepQuiet(900);
        writeDump("08-outside-scrim-tap", classifyRentalNow());

        // Recover if scrim somehow opened a row
        if (isTextPresent("Profile Info") || isTextPresent("Edit")) {
            DriverManager.get().navigate().back();
            sleepQuiet(800);
        }
        if (!profileDrawerNow()) {
            if (!"home".equals(classifyRentalNow())) {
                DriverManager.get().navigate().back();
                sleepQuiet(700);
            }
            new HomePage().tapDesc("Profile");
            sleepQuiet(900);
        }
        writeDump("09-before-logout", classifyRentalNow());
        writeBounds("09-before-logout");

        // Log Out → dialog → Cancel only (never Confirm)
        tapTextIfPresent("Log Out");
        sleepQuiet(1100);
        writeDump("10-logout-dialog", classifyRentalNow());
        writeBounds("10-logout-dialog");

        tapTextIfPresent("Cancel");
        if (!isTextPresent("Account") && !profileDrawerNow()) {
            tapTextIfPresent("No");
            tapDescIfPresent("Close");
        }
        sleepQuiet(900);
        writeDump("11-logout-cancelled", classifyRentalNow());

        if (!profileDrawerNow()) {
            if (!"home".equals(classifyRentalNow()) && !"home-drawer".equals(classifyRentalNow())) {
                DriverManager.get().navigate().back();
                sleepQuiet(700);
            }
            if (!profileDrawerNow()) {
                new HomePage().tapDesc("Profile");
                sleepQuiet(900);
            }
        }
        writeDump("12-final-drawer", classifyRentalNow());
        writeBounds("12-final-drawer");

        Allure.addAttachment("dump-dir", "text/plain", DUMP_DIR.toString());
    }

    private void tapOutsideDrawer() {
        // Left drawer → tap right scrim (Close navigation menu strip ~918–1080)
        DriverManager.get().executeScript("mobile: clickGesture",
                java.util.Map.of("x", 999, "y", 1200));
    }

    private void swipeDrawerUp() {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1800));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(java.time.Duration.ofMillis(400),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 900));
        swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        DriverManager.get().perform(java.util.List.of(swipe));
    }

    private void swipeDrawerDown() {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1000));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(java.time.Duration.ofMillis(400),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1800));
        swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        DriverManager.get().perform(java.util.List.of(swipe));
    }

    private boolean isTextPresent(String text) {
        return !DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[@text='" + text + "']")).isEmpty();
    }

    private boolean isTextContains(String fragment) {
        return !DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[contains(@text,'" + fragment + "')]")).isEmpty();
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
            throw new IllegalStateException("Profile drawer dump write failed: " + name, e);
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
                for (String row : ROWS) {
                    if (t.equals(row) || t.contains(row) || row.contains(t) && t.length() > 3) {
                        Rectangle r = el.getRect();
                        lines.add(t + " bounds=[" + r.x + "," + r.y + "][" + (r.x + r.width) + ","
                                + (r.y + r.height) + "] w=" + r.width + " h=" + r.height);
                        break;
                    }
                }
                if (t.contains("Log Out") || t.contains("Cancel") || t.contains("Confirm")) {
                    Rectangle r = el.getRect();
                    lines.add(t + " bounds=[" + r.x + "," + r.y + "][" + (r.x + r.width) + ","
                            + (r.y + r.height) + "]");
                }
            }
            for (String desc : List.of("Profile", "Back", "Close", "Home", "Calendar")) {
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
