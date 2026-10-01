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
import java.util.Map;
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
 * Dump-0: in-app Language from Profile drawer on {@code 9000000001}.
 * Select Hindi → Save → probe modules for reflection → restore English → Save.
 * Never Accept / Decline / Log Out Confirm / Chat now / Submit ticket.
 */
@Epic("Vendor app")
@Feature("Language settings dump — rental vendor 9000000001")
public class LanguageSettingsDump extends RentalHomeBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-lang-settings-0001-20261001");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,200})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Dump-0: Language settings + Hindi reflection probes + English restore")
    @Description("Profile → Language. Dump options, Save Hindi, dump Home/drawer/tabs/Account/Help, "
            + "restore English+Save. Never Accept/Decline/Log Out Confirm.")
    public void dumpLanguageSettings0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        writeDump("00-home-en", classifyRentalNow());

        home.tapDesc("Profile");
        sleepQuiet(1000);
        writeDump("01-drawer-en", classifyRentalNow());

        new ProfileDrawerPage().tapRow("Language");
        sleepQuiet(1200);
        writeDump("02-language-landing", classifyRentalNow());
        writeBounds("02-language-landing");

        // Select Hindi (native label)
        if (!tapTextIfPresent("हिंदी") && !tapTextContains("हिं")) {
            tapTextIfPresent("Hindi");
        }
        sleepQuiet(900);
        writeDump("03-hindi-selected-pre-save", classifyRentalNow());

        tapTextIfPresent("Save");
        sleepQuiet(1500);
        writeDump("04-after-hindi-save", classifyRentalNow());

        // Wherever we landed — capture then probe surfaces
        probeModule("05-after-save-surface");

        // Prefer reopen drawer for labeled rows (may be localized)
        openProfileDrawerBestEffort();
        writeDump("06-drawer-hindi", classifyRentalNow());
        writeBounds("06-drawer-hindi");

        // Home
        dismissDrawerBestEffort();
        sleepQuiet(700);
        writeDump("07-home-hindi", classifyRentalNow());

        // Bottom tabs by content-desc (often stay English) + by index fallback
        tapDescIfPresent("Calendar");
        sleepQuiet(1000);
        writeDump("08-calendar-hindi", classifyRentalNow());

        tapDescIfPresent("Home");
        sleepQuiet(800);
        tapDescIfPresent("Earning");
        sleepQuiet(1000);
        writeDump("09-earning-hindi", classifyRentalNow());

        tapDescIfPresent("Home");
        sleepQuiet(800);
        tapDescIfPresent("Fleet");
        sleepQuiet(1000);
        writeDump("10-fleet-hindi", classifyRentalNow());

        tapDescIfPresent("Home");
        sleepQuiet(800);
        tapDescIfPresent("Notification");
        sleepQuiet(1000);
        writeDump("11-notifications-hindi", classifyRentalNow());

        // Account via drawer — try English then first non-header row heuristics
        openProfileDrawerBestEffort();
        if (!tapTextIfPresent("Account") && !tapTextContains("Account")) {
            tapDrawerRowByIndex(0);
        }
        sleepQuiet(1100);
        writeDump("12-account-hindi", classifyRentalNow());
        navigateBackQuiet();
        sleepQuiet(700);

        openProfileDrawerBestEffort();
        if (!tapTextIfPresent("Help") && !tapTextContains("Help") && !tapTextContains("सहायता")) {
            // Help is usually 5th menu row (0=Account … 4=Help)
            tapDrawerRowByIndex(4);
        }
        sleepQuiet(1100);
        writeDump("13-help-hindi", classifyRentalNow());
        navigateBackQuiet();
        sleepQuiet(700);

        // Restore English
        openProfileDrawerBestEffort();
        if (!tapTextIfPresent("Language") && !tapTextContains("Language")
                && !tapTextContains("भाषा") && !tapTextContains("భాష")) {
            tapDrawerRowByIndex(5);
        }
        sleepQuiet(1100);
        writeDump("14-language-hindi-ui", classifyRentalNow());

        if (!tapTextIfPresent("English")) {
            tapTextContains("English");
        }
        sleepQuiet(700);
        writeDump("15-english-selected", classifyRentalNow());
        // Save may be localized
        if (!tapTextIfPresent("Save") && !tapTextContains("Save")
                && !tapTextContains("सहेजें") && !tapTextContains("सेव")) {
            tapBottomPrimaryBestEffort();
        }
        sleepQuiet(1500);
        writeDump("16-after-english-restore", classifyRentalNow());

        dismissDrawerBestEffort();
        sleepQuiet(700);
        writeDump("17-home-en-restored", classifyRentalNow());

        Allure.addAttachment("dump-dir", "text/plain", DUMP_DIR.toString());
    }

    private void probeModule(String name) {
        writeDump(name, classifyRentalNow());
    }

    private void openProfileDrawerBestEffort() {
        dismissDrawerBestEffort();
        sleepQuiet(400);
        if (profileDrawerNow()) {
            return;
        }
        try {
            new HomePage().tapDesc("Profile");
            sleepQuiet(900);
        } catch (RuntimeException e) {
            tapDescIfPresent("Profile");
            sleepQuiet(900);
        }
        if (!profileDrawerNow()) {
            // localized Profile desc?
            tapDescIfPresent("Profile");
            sleepQuiet(800);
        }
    }

    private void dismissDrawerBestEffort() {
        if (!profileDrawerNow()) {
            return;
        }
        if (isDescPresent("Close navigation menu")) {
            tapDescIfPresent("Close navigation menu");
            sleepQuiet(700);
            return;
        }
        navigateBackQuiet();
        sleepQuiet(700);
    }

    private void tapDrawerRowByIndex(int index) {
        // Dump-sourced MENU_ROWS order; clickable rows with TextView children
        List<WebElement> rows = DriverManager.get().findElements(
                By.xpath("//android.view.View[@clickable='true'][.//android.widget.TextView]"));
        List<WebElement> menu = new ArrayList<>();
        for (WebElement row : rows) {
            String t = "";
            try {
                t = row.findElement(By.className("android.widget.TextView")).getAttribute("text");
            } catch (RuntimeException ignored) {
            }
            if (t == null || t.isBlank()) {
                continue;
            }
            if (t.contains("Log Out") || t.contains("Logout") || t.contains("Company")
                    || t.contains("L2B-") || t.length() > 40) {
                continue;
            }
            menu.add(row);
        }
        if (index >= 0 && index < menu.size()) {
            clickGesture(menu.get(index));
            Allure.parameter("drawerRowIndex", index + "=" + safeText(menu.get(index)));
        }
    }

    private void tapBottomPrimaryBestEffort() {
        // Bottom-most clickable with a short TextView (Save)
        List<WebElement> clickable = DriverManager.get().findElements(
                By.xpath("//android.view.View[@clickable='true'][.//android.widget.TextView]"));
        WebElement best = null;
        int bestY = -1;
        for (WebElement el : clickable) {
            Rectangle r = el.getRect();
            if (r.y > bestY && r.y > 1800) {
                bestY = r.y;
                best = el;
            }
        }
        if (best != null) {
            clickGesture(best);
        }
    }

    private void navigateBackQuiet() {
        try {
            DriverManager.get().navigate().back();
        } catch (RuntimeException ignored) {
        }
    }

    private boolean isDescPresent(String desc) {
        return !DriverManager.get().findElements(
                By.xpath("//*[@content-desc='" + desc + "']")).isEmpty();
    }

    private boolean tapTextIfPresent(String text) {
        try {
            var els = DriverManager.get().findElements(
                    By.xpath("//android.widget.TextView[@text=" + xpathLit(text) + "]"));
            if (els.isEmpty()) {
                return false;
            }
            clickGesture(els.get(0));
            Allure.parameter("tappedText", text);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private boolean tapTextContains(String fragment) {
        try {
            var els = DriverManager.get().findElements(
                    By.xpath("//android.widget.TextView[contains(@text," + xpathLit(fragment) + ")]"));
            if (els.isEmpty()) {
                return false;
            }
            clickGesture(els.get(0));
            Allure.parameter("tappedContains", fragment);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void tapDescIfPresent(String desc) {
        try {
            var els = DriverManager.get().findElements(By.xpath("//*[@content-desc='" + desc + "']"));
            if (!els.isEmpty()) {
                clickGesture(els.get(0));
                Allure.parameter("tappedDesc", desc);
            }
        } catch (RuntimeException ignored) {
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
            throw new IllegalStateException("Language dump write failed: " + name, e);
        }
    }

    private void writeBounds(String name) {
        Path dir = DUMP_DIR.resolve(name);
        try {
            Files.createDirectories(dir);
            List<String> lines = new ArrayList<>();
            for (WebElement el : DriverManager.get().findElements(By.className("android.widget.TextView"))) {
                String t = safeText(el);
                if (t.isBlank() || t.length() > 120) {
                    continue;
                }
                Rectangle r = el.getRect();
                lines.add(t + " bounds=[" + r.x + "," + r.y + "][" + (r.x + r.width) + ","
                        + (r.y + r.height) + "]");
            }
            for (String desc : List.of("Back", "Save", "Profile", "Close navigation menu")) {
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

    private void clickGesture(WebElement el) {
        Rectangle r = el.getRect();
        DriverManager.get().executeScript("mobile: clickGesture",
                Map.of("x", r.x + Math.max(1, r.width / 2), "y", r.y + Math.max(1, r.height / 2)));
    }

    private static String xpathLit(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        return "'" + value.replace("'", "") + "'";
    }

    private static String safeText(WebElement el) {
        try {
            String t = el.getAttribute("text");
            return t == null || "null".equalsIgnoreCase(t) ? "" : t.trim();
        } catch (RuntimeException e) {
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
