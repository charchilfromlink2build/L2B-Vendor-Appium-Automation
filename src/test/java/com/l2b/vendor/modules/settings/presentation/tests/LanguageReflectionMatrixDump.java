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
 * Controlled Hindi reflection matrix. Confirms Hindi drawer before each probe.
 * Restores English. Writes /tmp/l2b-lang-matrix-0001-20261001/matrix.txt verdicts.
 */
@Epic("Vendor app")
@Feature("Language Hindi reflection matrix — 9000000001")
public class LanguageReflectionMatrixDump extends RentalHomeBaseTest {

    private static final Path DUMP_DIR = Path.of("/tmp/l2b-lang-matrix-0001-20261001");
    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,200})\"");
    private static final List<String> VERDICTS = new ArrayList<>();

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test
    public void hindiReflectionMatrix() throws Exception {
        Files.createDirectories(DUMP_DIR);
        reachUsableRentalHomeOrFailExtendTime();
        dump("00-home-en");

        // Apply Hindi
        openDrawer();
        tap("Language");
        sleep(1100);
        dump("01-language-en");
        tap("हिंदी");
        sleep(500);
        tap("Save");
        sleep(1500);
        dump("02-after-save");

        // Must see Hindi drawer — if not, reopen Profile
        ensureHindiDrawer();
        dump("03-drawer-hindi");
        record("drawer",
                has("खाता") && has("सहायता") && has("भाषा"),
                has("Account") && has("Help") && has("Language"),
                "Log Out still English=" + has("Log Out") + "; Company Id English=" + src().contains("Company Id"));

        // HOME
        closeDrawer();
        sleep(900);
        // If landed on bookings See-all, Back to Home
        if (has("Upcoming Booking") || has("Quick Booking")) {
            back();
            sleep(800);
        }
        if (!looksHome()) {
            tapDesc("Home");
            tap("होम");
            click(540, 2280);
            sleep(900);
        }
        dump("04-home-hindi");
        record("home",
                hasDevanagari() || has("वर्तमान") || has("कमाई") || has("शुभ") || has("नमस्ते"),
                has("Current Earning") || has("Good Afternoon") || has("Booking Orders"),
                "key EN still present=" + (has("Current Earning") || has("Good Afternoon")));

        // CALENDAR
        goHome();
        tapDesc("Calendar");
        tap("कैलेंडर");
        click(135, 2280);
        sleep(1100);
        dump("05-calendar-hindi");
        record("calendar",
                hasDevanagari() || has("अनुसूची") || has("शेड्यूल"),
                has("Schedule") || has("Previous month") || has("No bookings"),
                "");

        // EARNING
        goHome();
        tapDesc("Earning");
        tap("कमाई");
        click(675, 2280);
        sleep(1100);
        dump("06-earning-hindi");
        record("earning",
                hasDevanagari() || has("बटुआ") || has("कमाई"),
                has("Earning & Incentive") || has("Wallet Balance") || has("Withdraw"),
                "");

        // FLEET
        goHome();
        tapDesc("Fleet");
        tap("फ्लीट");
        click(945, 2280);
        sleep(1100);
        dump("07-fleet-hindi");
        record("fleet",
                hasDevanagari() || has("बेड़ा") || has("मशीन"),
                has("Your Fleet") || has("Add Machine"),
                "");

        // NOTIFICATIONS
        goHome();
        if (!tapDesc("Notifications") && !tapDesc("Notification") && !tapDesc("सूचनाएं") && !tapDesc("सूचना")) {
            click(900, 155);
        }
        sleep(1100);
        dump("08-notifications-hindi");
        record("notifications",
                hasDevanagari() || has("सूचना"),
                has("Notification") && (has("Unread") || has("All")),
                "");
        back();
        sleep(700);

        // BOOKINGS (Upcoming See all from Home if present)
        goHome();
        if (tap("See all") || tap("सभी देखें")) {
            sleep(1100);
            dump("09-bookings-hindi");
            record("bookings",
                    hasDevanagari() || has("आगामी") || has("बुकिंग"),
                    has("Upcoming Booking") || has("Upcoming") && has("Active"),
                    "");
            back();
            sleep(700);
        } else {
            record("bookings", false, true, "See all not found on Home");
        }

        // Drawer modules
        probeDrawer("account", "खाता", "Account", "प्रोफ़ाइल", "Profile Info", "Full Name");
        probeDrawer("kyc", "केवाईसी", "KYC", "केवाईसी", "KYC Details", "Aadhaar");
        probeDrawer("machines", "आपकी मशीनें", "Your machines", "मशीन", "Your machines", "Add Machine");
        probeDrawer("team", "टीम मैनेज करें", "Manage team", "टीम", "Manage team", "Add");
        probeDrawer("help", "सहायता", "Help", "लाइव चैट", "Live chat", "Help & Support");
        probeDrawer("refer", "रेफर करें और कमाएं", "Refer", "रेफर", "Refer & Earn", "referral");
        probeDrawer("faq", "अक्सर पूछे जाने वाले प्रश्न", "FAQ", "अक्सर", "FAQs", "Why is my account");
        probeDrawer("terms", "नियम और शर्तें", "Terms", "नियम", "Terms & Services", "Accepting these terms");
        probeDrawer("policies", "नीतियां", "Policies", "नीति", "Policies", "What we collect");
        probeDrawer("settings", "सेटिंग्स", "Settings", "अनुमति", "Permissions", "Notification");
        probeDrawer("language", "भाषा", "Language", "सेव करें", "Save", "Choose the language");

        // Restore English
        ensureHindiDrawer();
        tap("भाषा");
        sleep(1000);
        tap("English");
        sleep(400);
        if (!tap("Save") && !tap("सेव करें")) {
            click(540, 2200);
        }
        sleep(1500);
        closeDrawer();
        sleep(800);
        goHome();
        dump("99-home-restored");
        record("restore-english",
                has("Current Earning") || has("Good Afternoon") || has("Account"),
                has("खाता") && has("वर्तमान"),
                "");

        Files.writeString(DUMP_DIR.resolve("matrix.txt"), String.join("\n", VERDICTS), StandardCharsets.UTF_8);
        Allure.addAttachment("matrix", "text/plain", String.join("\n", VERDICTS));
        Allure.addAttachment("dump-dir", "text/plain", DUMP_DIR.toString());
    }

    private void probeDrawer(String id, String hiRow, String enRow, String hiMarker, String enMarker,
            String enMarker2) throws Exception {
        ensureHindiDrawer();
        if (!tap(hiRow) && !tap(enRow)) {
            record(id, false, true, "row not tappable");
            return;
        }
        sleep(1100);
        dump("d-" + id);
        boolean hi = has(hiMarker) || hasDevanagari();
        boolean en = has(enMarker) || has(enMarker2);
        record(id, hi && !en || (hi && en), en && !hi, "hiMarker=" + hi + " enMarker=" + en);
        // Back to drawer or home
        back();
        sleep(700);
        if (!drawerHindi()) {
            openDrawer();
            sleep(700);
        }
    }

    private void record(String surface, boolean localized, boolean stillEnglish, String note) {
        String verdict;
        if (localized && !stillEnglish) {
            verdict = "PASS";
        } else if (localized && stillEnglish) {
            verdict = "PARTIAL";
        } else if (!localized && stillEnglish) {
            verdict = "FAIL";
        } else {
            verdict = "UNKNOWN";
        }
        String line = surface + "\t" + verdict + "\tlocalized=" + localized + "\tenglish=" + stillEnglish
                + "\t" + note;
        VERDICTS.add(line);
        Allure.parameter(surface, verdict + " " + note);
    }

    private void ensureHindiDrawer() {
        for (int i = 0; i < 5; i++) {
            if (drawerHindi()) {
                return;
            }
            closeDrawer();
            sleep(400);
            openDrawer();
            sleep(800);
            if (drawerHindi()) {
                return;
            }
            // Maybe language not applied — re-save Hindi
            if (tap("Language") || tap("भाषा")) {
                sleep(900);
                tap("हिंदी");
                sleep(400);
                if (!tap("Save") && !tap("सेव करें")) {
                    click(540, 2200);
                }
                sleep(1200);
            }
        }
    }

    private boolean drawerHindi() {
        return has("खाता") || has("भाषा") || has("सहायता");
    }

    private boolean looksHome() {
        return has("Current Earning") || has("Booking Orders") || has("Good Afternoon")
                || has("वर्तमान") || has("कमाई") || (has("Calendar") && has("Fleet"))
                || (has("कैलेंडर") && has("फ्लीट"));
    }

    private void goHome() {
        closeDrawer();
        sleep(400);
        for (int i = 0; i < 3; i++) {
            if (looksHome()) {
                return;
            }
            if (has("Upcoming Booking") || has("Help & Support") || has("सहायता और समर्थन")
                    || has("Profile Info") || has("प्रोफ़ाइल")) {
                back();
                sleep(700);
                continue;
            }
            tapDesc("Home");
            tap("होम");
            click(405, 2280);
            sleep(800);
        }
    }

    private void openDrawer() {
        closeDrawer();
        sleep(300);
        if (!tapDesc("Profile") && !tapDesc("प्रोफाइल") && !tapDesc("प्रोफ़ाइल")) {
            click(80, 155);
        }
        sleep(900);
    }

    private void closeDrawer() {
        if (tapDesc("Close navigation menu") || tapDesc("नेविगेशन मेन्यू बंद करें")
                || tapDesc("नेविगेशन मेनू बंद करें")) {
            sleep(500);
            return;
        }
        if (drawerHindi() || has("Account") || has("Log Out")) {
            click(999, 400);
            sleep(500);
        }
    }

    private boolean hasDevanagari() {
        String s = src();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0900 && c <= 0x097F) {
                return true;
            }
        }
        return false;
    }

    private boolean has(String t) {
        return !DriverManager.get().findElements(
                By.xpath("//*[contains(@text," + lit(t) + ")]")).isEmpty()
                || src().contains(t);
    }

    private boolean tap(String text) {
        var els = DriverManager.get().findElements(By.xpath("//android.widget.TextView[@text=" + lit(text) + "]"));
        if (els.isEmpty()) {
            els = DriverManager.get().findElements(
                    By.xpath("//android.widget.TextView[contains(@text," + lit(text) + ")]"));
        }
        if (els.isEmpty()) {
            return false;
        }
        clickGesture(els.get(0));
        return true;
    }

    private boolean tapDesc(String d) {
        var els = DriverManager.get().findElements(By.xpath("//*[@content-desc='" + d + "']"));
        if (els.isEmpty()) {
            return false;
        }
        clickGesture(els.get(0));
        return true;
    }

    private void back() {
        DriverManager.get().navigate().back();
    }

    private void click(int x, int y) {
        DriverManager.get().executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    private void clickGesture(WebElement el) {
        Rectangle r = el.getRect();
        click(r.x + Math.max(1, r.width / 2), r.y + Math.max(1, r.height / 2));
    }

    private void dump(String name) throws Exception {
        Path dir = DUMP_DIR.resolve(name);
        Files.createDirectories(dir);
        String xml = src();
        Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
        Set<String> out = new LinkedHashSet<>();
        Matcher m = TEXT.matcher(xml);
        while (m.find()) {
            out.add(m.group(1));
        }
        Files.writeString(dir.resolve("texts.txt"), String.join("\n", out), StandardCharsets.UTF_8);
        Files.write(dir.resolve("screen.png"),
                ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES));
    }

    private String src() {
        try {
            return ((AndroidDriver) DriverManager.get()).getPageSource();
        } catch (Exception e) {
            return "";
        }
    }

    private static String lit(String v) {
        if (!v.contains("'")) {
            return "'" + v + "'";
        }
        return "\"" + v + "\"";
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
