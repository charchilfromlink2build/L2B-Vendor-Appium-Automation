package com.l2b.vendor.modules.help.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.help.presentation.pages.HelpSupportPage;
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
 * Deep dump: concern options, enablement, ticket detail, Call Support.
 * Never Chat now / Submit / Accept / Decline / Log Out Confirm.
 */
@Epic("Vendor app")
@Feature("Help & Support deep dump — rental 9000000001")
public class HelpSupportDeepDump extends RentalHomeBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-help-deep-0001-20261001");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,200})\"");
    private static final Pattern DESC = Pattern.compile("content-desc=\"([^\"]{1,160})\"");

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test(description = "Deep dump: concern picker, enablement, ticket detail, Call Support")
    @Description("9000000001. Select concern (never Chat now/Submit). Tap ticket. Call Support recover.")
    public void deepDumpHelp0001() throws IOException {
        Files.createDirectories(DUMP_DIR);
        reachUsableRentalHomeOrFailExtendTime();
        new HomePage().tapDesc("Profile");
        sleepQuiet(900);
        new ProfileDrawerPage().tapRow("Help");
        sleepQuiet(1100);
        HelpSupportPage help = new HelpSupportPage();
        writeDump("00-landing", classifyRentalNow());

        // Live chat concern options
        help.tapLiveChat();
        sleepQuiet(1000);
        writeDump("01-live-chat", classifyRentalNow());
        help.tapTicketConcern();
        sleepQuiet(1000);
        writeDump("02-live-concern-sheet", classifyRentalNow());
        writeBounds("02-live-concern-sheet");
        String picked = pickFirstConcernOption();
        Allure.parameter("liveConcernPicked", picked == null ? "none" : picked);
        sleepQuiet(900);
        writeDump("03-live-after-pick", classifyRentalNow());
        writeEnabled("03-live-after-pick", "Chat now");
        // NEVER Chat now
        dismissToHelpLanding();
        sleepQuiet(800);
        writeDump("04-back-help", classifyRentalNow());

        // Raise Ticket: concern + description enablement
        help.tapRaiseTicket();
        sleepQuiet(1000);
        writeDump("05-raise", classifyRentalNow());
        writeEnabled("05-raise", "Submit");
        help.tapTicketConcern();
        sleepQuiet(900);
        writeDump("06-raise-concern-sheet", classifyRentalNow());
        pickFirstConcernOption();
        sleepQuiet(800);
        writeDump("07-raise-after-concern", classifyRentalNow());
        writeEnabled("07-raise-after-concern", "Submit");
        typeIntoDescription("Automation probe — do not submit");
        sleepQuiet(600);
        writeDump("08-raise-after-description", classifyRentalNow());
        writeEnabled("08-raise-after-description", "Submit");
        // NEVER Submit
        dismissToHelpLanding();
        sleepQuiet(800);

        // Ticket history → open first ticket
        help.tapTicketHistory();
        sleepQuiet(1100);
        writeDump("09-history", classifyRentalNow());
        swipeUp();
        sleepQuiet(500);
        writeDump("10-history-scrolled", classifyRentalNow());
        swipeDown();
        sleepQuiet(400);
        if (tapFirstTicketId()) {
            sleepQuiet(1100);
            writeDump("11-ticket-detail", classifyRentalNow());
            writeBounds("11-ticket-detail");
            DriverManager.get().navigate().back();
            sleepQuiet(900);
            writeDump("12-after-ticket-detail", classifyRentalNow());
        }
        dismissToHelpLanding();
        sleepQuiet(700);

        // Call Support
        help.tapCallSupport();
        sleepQuiet(1500);
        writeDump("13-call-support", classifyRentalNow());
        Files.writeString(DUMP_DIR.resolve("13-call-support/package.txt"),
                safePackage(), StandardCharsets.UTF_8);
        for (int i = 0; i < 4; i++) {
            if (safePackage().contains("l2b") && help.isDisplayedNow()) {
                break;
            }
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        writeDump("14-after-call", classifyRentalNow());
        Allure.addAttachment("dump-dir", "text/plain", DUMP_DIR.toString());
    }

    private String pickFirstConcernOption() {
        // Prefer known billing option from history dump; else first non-chrome TextView
        List<String> prefer = List.of(
                "Payment & Billing Issues",
                "Payment and Billing Issues",
                "Booking Issues",
                "Technical Issues",
                "Other");
        for (String p : prefer) {
            if (tapTextExact(p) || tapTextContains(p.split(" ")[0])) {
                return p;
            }
        }
        for (WebElement el : DriverManager.get().findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el);
            if (t.isBlank() || t.length() > 80) {
                continue;
            }
            if (Set.of("Ticket Concern", "Submit a query", "Live chat", "Raise Ticket",
                    "Chat now", "Submit", "Help & Support", "Back").contains(t)) {
                continue;
            }
            if (t.startsWith("Working") || t.startsWith("TKT-") || t.contains("ready to help")) {
                continue;
            }
            clickGesture(el);
            return t;
        }
        return null;
    }

    private void typeIntoDescription(String value) {
        List<WebElement> edits = DriverManager.get().findElements(By.className("android.widget.EditText"));
        if (edits.isEmpty()) {
            Allure.parameter("editText", "missing");
            return;
        }
        edits.get(0).click();
        sleepQuiet(300);
        edits.get(0).clear();
        edits.get(0).sendKeys(value);
    }

    private boolean tapFirstTicketId() {
        for (WebElement el : DriverManager.get().findElements(By.className("android.widget.TextView"))) {
            String t = safeText(el);
            if (t.startsWith("TKT-")) {
                clickGesture(el);
                Allure.parameter("tappedTicket", t);
                return true;
            }
        }
        return false;
    }

    private void dismissToHelpLanding() {
        for (int i = 0; i < 6; i++) {
            HelpSupportPage page = new HelpSupportPage();
            if (page.isHelpTitleVisible() && page.isLiveChatVisible() && page.isRaiseTicketVisible()
                    && page.isTicketHistoryVisible() && !page.isChatNowVisible() && !page.isSubmitVisible()) {
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

    private boolean tapTextExact(String text) {
        var els = DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[@text='" + text.replace("'", "\\'") + "']"));
        if (els.isEmpty()) {
            // ampersand
            els = DriverManager.get().findElements(
                    By.xpath("//android.widget.TextView[@text=" + xpathLit(text) + "]"));
        }
        if (!els.isEmpty()) {
            clickGesture(els.get(0));
            return true;
        }
        return false;
    }

    private boolean tapTextContains(String fragment) {
        var els = DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[contains(@text," + xpathLit(fragment) + ")]"));
        if (!els.isEmpty()) {
            clickGesture(els.get(0));
            return true;
        }
        return false;
    }

    private static String xpathLit(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        return "\"" + value + "\"";
    }

    private void writeEnabled(String name, String label) {
        try {
            Path dir = DUMP_DIR.resolve(name);
            Files.createDirectories(dir);
            var parents = DriverManager.get().findElements(
                    By.xpath("//android.view.View[@clickable='true'][.//android.widget.TextView[@text='"
                            + label + "']]"));
            String line = label + " parents=" + parents.size();
            if (!parents.isEmpty()) {
                line += " enabled=" + parents.get(0).getAttribute("enabled")
                        + " clickable=" + parents.get(0).getAttribute("clickable");
            }
            Files.writeString(dir.resolve("enabled-" + label.replace(' ', '_') + ".txt"),
                    line, StandardCharsets.UTF_8);
            Allure.parameter(name + "_" + label, line);
        } catch (Exception e) {
            Allure.parameter("enabledFail_" + label, e.getClass().getSimpleName());
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
            throw new IllegalStateException("deep dump write failed: " + name, e);
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

    private String safePackage() {
        try {
            Object pkg = ((AndroidDriver) DriverManager.get()).getCurrentPackage();
            return pkg == null ? "" : pkg.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void clickGesture(WebElement el) {
        Rectangle r = el.getRect();
        int x = r.x + Math.max(1, r.width / 2);
        int y = r.y + Math.max(1, r.height / 2);
        DriverManager.get().executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
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
