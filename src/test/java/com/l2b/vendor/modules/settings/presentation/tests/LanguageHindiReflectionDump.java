package com.l2b.vendor.modules.settings.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
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
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

/**
 * Reflection probe after Hindi Save. Uses localized desc + bottom-tab coordinates.
 * Restores English at end. Never Accept/Decline/Log Out Confirm.
 */
@Epic("Vendor app")
@Feature("Language Hindi reflection probe — 9000000001")
public class LanguageHindiReflectionDump extends RentalHomeBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-lang-reflect-0001-20261001");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,200})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test
    public void reflectHindiAcrossModules() throws Exception {
        Files.createDirectories(DUMP_DIR);
        reachUsableRentalHomeOrFailExtendTime();
        writeDump("00-home-en");

        openLanguage();
        tapLabel("हिंदी");
        sleepQuiet(600);
        tapLabel("Save");
        sleepQuiet(1200);
        writeDump("01-drawer-after-save");

        closeDrawer();
        sleepQuiet(800);
        writeDump("02-home-hindi");

        // Bottom tabs — try localized + EN desc, then coordinate band
        tapTab("Calendar", "कैलेंडर", 0);
        sleepQuiet(1000);
        writeDump("03-calendar-hindi");

        tapTab("Home", "होम", 1);
        sleepQuiet(800);
        tapTab("Earning", "कमाई", 2);
        sleepQuiet(1000);
        writeDump("04-earning-hindi");

        tapTab("Home", "होम", 1);
        sleepQuiet(700);
        tapTab("Fleet", "फ्लीट", 3);
        sleepQuiet(1000);
        writeDump("05-fleet-hindi");

        tapTab("Home", "होम", 1);
        sleepQuiet(700);
        // Notifications bell — Profile is top; Notification often content-desc
        if (!tapDescAny("Notification", "सूचना", "नोटिफिकेशन")) {
            // top-right-ish notification often near Profile
            clickAt(900, 160);
        }
        sleepQuiet(1000);
        writeDump("06-notifications-hindi");
        DriverManager.get().navigate().back();
        sleepQuiet(700);

        // FAQ / Terms / Policies / Settings / Refer / KYC / Team via drawer indices
        openDrawer();
        tapTextOrIndex("केवाईसी", "KYC", 1);
        sleepQuiet(1000);
        writeDump("07-kyc-hindi");
        backToHomeDrawerClosed();

        openDrawer();
        tapTextOrIndex("टीम मैनेज करें", "Manage team", 3);
        sleepQuiet(1000);
        writeDump("08-team-hindi");
        backToHomeDrawerClosed();

        openDrawer();
        tapTextOrIndex("रेफर करें और कमाएं", "Refer", 6);
        sleepQuiet(1000);
        writeDump("09-refer-hindi");
        backToHomeDrawerClosed();

        openDrawer();
        tapTextOrIndex("अक्सर पूछे जाने वाले प्रश्न", "FAQ", 7);
        sleepQuiet(1000);
        writeDump("10-faq-hindi");
        backToHomeDrawerClosed();

        openDrawer();
        tapTextOrIndex("नियम और शर्तें", "Terms", 8);
        sleepQuiet(1000);
        writeDump("11-terms-hindi");
        backToHomeDrawerClosed();

        openDrawer();
        tapTextOrIndex("नीतियां", "Policies", 9);
        sleepQuiet(1000);
        writeDump("12-policies-hindi");
        backToHomeDrawerClosed();

        openDrawer();
        tapTextOrIndex("सेटिंग्स", "Settings", 10);
        sleepQuiet(1000);
        writeDump("13-settings-hindi");
        backToHomeDrawerClosed();

        // Restore English
        openLanguage();
        tapLabel("English");
        sleepQuiet(500);
        if (!tapLabel("Save") && !tapLabel("सेव करें")) {
            clickAt(540, 2200);
        }
        sleepQuiet(1200);
        closeDrawer();
        sleepQuiet(800);
        writeDump("14-home-en-restored");
        Allure.addAttachment("dump-dir", "text/plain", DUMP_DIR.toString());
    }

    private void openLanguage() {
        openDrawer();
        if (!tapLabel("भाषा") && !tapLabel("Language")) {
            tapTextOrIndex("भाषा", "Language", 5);
        }
        sleepQuiet(1100);
    }

    private void openDrawer() {
        closeDrawer();
        sleepQuiet(400);
        if (!tapDescAny("Profile", "प्रोफाइल", "प्रोफ़ाइल")) {
            // Profile avatar top-left-ish on Home
            clickAt(80, 160);
        }
        sleepQuiet(900);
    }

    private void closeDrawer() {
        if (tapDescAny("Close navigation menu", "नेविगेशन मेन्यू बंद करें", "नेविगेशन मेनू बंद करें")) {
            sleepQuiet(600);
            return;
        }
        // right scrim
        clickAt(999, 400);
        sleepQuiet(600);
    }

    private void backToHomeDrawerClosed() {
        for (int i = 0; i < 4; i++) {
            DriverManager.get().navigate().back();
            sleepQuiet(600);
            if (textVisible("Good") || textVisible("Current Earning") || textVisible("वर्तमान")
                    || textVisible("Booking Orders") || textVisible("Calendar") || textVisible("कैलेंडर")
                    || textVisible("होम") || textVisible("Home")) {
                break;
            }
        }
        closeDrawer();
        sleepQuiet(500);
    }

    private void tapTab(String en, String hi, int index) {
        if (tapDescAny(en, hi)) {
            return;
        }
        if (tapLabel(en) || tapLabel(hi)) {
            return;
        }
        // Bottom tab bar coords on 1080x2400 — 4 tabs Calendar Home Earning Fleet
        int[] xs = {135, 405, 675, 945};
        clickAt(xs[Math.min(index, 3)], 2280);
    }

    private void tapTextOrIndex(String hi, String en, int index) {
        if (tapLabel(hi) || tapLabel(en)) {
            return;
        }
        List<WebElement> rows = DriverManager.get().findElements(
                By.xpath("//android.view.View[@clickable='true'][.//android.widget.TextView]"));
        int n = 0;
        for (WebElement row : rows) {
            String t = "";
            try {
                t = row.findElement(By.className("android.widget.TextView")).getAttribute("text");
            } catch (RuntimeException ignored) {
            }
            if (t == null || t.isBlank() || t.contains("Log Out") || t.contains("Company")
                    || t.contains("L2B-") || t.length() > 50) {
                continue;
            }
            if (n == index) {
                clickGesture(row);
                return;
            }
            n++;
        }
    }

    private boolean tapDescAny(String... descs) {
        for (String d : descs) {
            var els = DriverManager.get().findElements(By.xpath("//*[@content-desc='" + d + "']"));
            if (!els.isEmpty()) {
                clickGesture(els.get(0));
                return true;
            }
        }
        return false;
    }

    private boolean tapLabel(String text) {
        var els = DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[@text='" + text.replace("'", "") + "']"));
        if (els.isEmpty()) {
            els = DriverManager.get().findElements(
                    By.xpath("//android.widget.TextView[contains(@text,'" + text.replace("'", "") + "')]"));
        }
        if (els.isEmpty()) {
            return false;
        }
        clickGesture(els.get(0));
        return true;
    }

    private boolean textVisible(String fragment) {
        return !DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[contains(@text,'" + fragment + "')]")).isEmpty();
    }

    private void clickAt(int x, int y) {
        DriverManager.get().executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    private void clickGesture(WebElement el) {
        Rectangle r = el.getRect();
        clickAt(r.x + Math.max(1, r.width / 2), r.y + Math.max(1, r.height / 2));
    }

    private void writeDump(String name) throws Exception {
        Path dir = DUMP_DIR.resolve(name);
        Files.createDirectories(dir);
        String xml = ((AndroidDriver) DriverManager.get()).getPageSource();
        Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
        Set<String> out = new LinkedHashSet<>();
        Matcher t = TEXT.matcher(xml);
        while (t.find()) {
            out.add("text=" + t.group(1));
        }
        Matcher d = DESC.matcher(xml);
        while (d.find()) {
            out.add("desc=" + d.group(1));
        }
        Files.writeString(dir.resolve("texts.txt"), String.join("\n", out), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("package.txt"),
                ((AndroidDriver) DriverManager.get()).getCurrentPackage(), StandardCharsets.UTF_8);
        Files.write(dir.resolve("screen.png"),
                ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
        Allure.addAttachment(name, "text/plain", String.join("\n", out));
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
