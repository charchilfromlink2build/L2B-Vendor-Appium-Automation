package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.LanguageSettingsPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
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
 * Telugu / Kannada reflection matrix (dump-first). Uses ProfileDrawerPage +
 * LanguageSettingsPage (same path as Hindi). Restores English at end.
 */
@Epic("Vendor app")
@Feature("Language TE/KN reflection matrix — 9000000001")
public class LanguageTeKnReflectionMatrixDump extends LanguageSettingsBaseTest {

    private static final Pattern TEXT = Pattern.compile("text=\"([^\"]{1,200})\"");
    private Path dumpDir;
    private final List<String> verdicts = new ArrayList<>();
    private String localeCode;
    private int scriptLo;
    private int scriptHi;

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        Adb.forceStop(Config.get("app.package"));
    }

    @Test
    public void teluguReflectionMatrix() throws Exception {
        runMatrix("te", true, 0x0C00, 0x0C7F);
    }

    @Test
    public void kannadaReflectionMatrix() throws Exception {
        runMatrix("kn", false, 0x0C80, 0x0CFF);
    }

    private void runMatrix(String code, boolean telugu, int lo, int hi) throws Exception {
        this.localeCode = code;
        this.scriptLo = lo;
        this.scriptHi = hi;
        this.verdicts.clear();
        dumpDir = Path.of("/tmp/l2b-lang-matrix-" + code + "-0001-20261002");
        Files.createDirectories(dumpDir);
        Allure.parameter("locale", code);

        reachUsableRentalHomeOrFailExtendTime();
        assertThat(new HomePage().isDisplayedNow()).as("Home before language apply").isTrue();
        dump("00-home-en");

        // Do not re-enter full login (reachProfileDrawer → reachUsableRentalHome).
        new HomePage().tapDesc("Profile");
        sleep(1000);
        ProfileDrawerPage drawer = new ProfileDrawerPage();
        drawer.waitUntilLoaded();
        assertThat(drawer.isDisplayedNow()).as("Profile drawer open").isTrue();
        dump("00b-drawer-en");
        drawer.tapRow("Language");
        sleep(1100);
        LanguageSettingsPage lang = new LanguageSettingsPage();
        lang.waitUntilLoaded();
        assertThat(lang.isDisplayedNow()).as("Language settings opened").isTrue();
        dump("01-language-page");
        if (telugu) {
            lang.selectTelugu();
        } else {
            lang.selectKannada();
        }
        sleep(500);
        lang.tapSave();
        sleep(1800);
        dump("02-after-save");

        // Re-open drawer from Home without full re-login
        goHome();
        openDrawer();
        sleep(900);
        dump("03-drawer-locale");
        List<String> drawerRows = visibleTexts();
        Files.writeString(dumpDir.resolve("drawer-rows.txt"),
                String.join("\n", drawerRows), StandardCharsets.UTF_8);
        boolean drawerScript = hasScript();
        boolean drawerEnRows = has("Account") && has("Language");
        record("drawer", drawerScript, drawerEnRows,
                "Log Out EN=" + has("Log Out")
                        + "; Company Id EN=" + src().contains("Company Id")
                        + "; script=" + drawerScript
                        + "; Account+Language EN=" + drawerEnRows
                        + "; rows=" + drawerRows.size());

        closeDrawer();
        sleep(800);
        goHome();
        dump("04-home-locale");
        boolean homeScript = hasScript();
        boolean homeEn = has("Current Earning") || has("Good Afternoon") || has("Good Morning");
        record("home", homeScript, homeEn && !homeScript,
                "script=" + homeScript + " EN chrome=" + homeEn);

        goHome();
        tapBottom("Calendar", 135);
        dump("05-calendar-locale");
        boolean calScript = hasScript();
        record("calendar", calScript, has("Schedule") && !calScript,
                "script=" + calScript + " October EN=" + has("October"));

        goHome();
        tapBottom("Earning", 675);
        dump("06-earning-locale");
        boolean earnScript = hasScript();
        record("earning", earnScript, has("Earning & Incentive") && !earnScript,
                "script=" + earnScript + " incentive EN=" + has("Once they reach 10 jobs"));

        goHome();
        tapBottom("Fleet", 945);
        dump("07-fleet-locale");
        boolean fleetScript = hasScript();
        record("fleet", fleetScript,
                (has("Your Fleet") || has("Add Machine")) && !fleetScript,
                "script=" + fleetScript);

        goHome();
        if (!tapDesc("Notifications") && !tapDesc("Notification")) {
            try {
                new HomePage().tapDesc("Notifications");
            } catch (RuntimeException ignored) {
                click(900, 155);
            }
        }
        sleep(1100);
        dump("08-notifications-locale");
        boolean notifScript = hasScript();
        boolean notifEn = has("Notification") && (has("Unread") || has("All"));
        record("notifications", notifScript, notifEn && !notifScript,
                "script=" + notifScript + " EN=" + notifEn);
        back();
        sleep(700);

        probe("account", List.of("Account"));
        probe("kyc", List.of("KYC"));
        probe("machines", List.of("Your machines"));
        probe("team", List.of("Manage team"));
        probe("help", List.of("Help"));
        probe("refer", List.of("Refer"), "Share your code", "They sign up");
        probe("faq", List.of("FAQ"), "How do I start a booked job");
        probe("terms", List.of("Terms"), "Accepting these terms", "Your obligations");
        probe("policies", List.of("Policies"), true, "How we use it", "Your rights");
        probe("settings", List.of("Settings"));

        restoreEnglishLanguage();
        sleep(1000);
        goHome();
        dump("99-home-restored");
        record("restore-english",
                has("Current Earning") || has("Good Morning") || has("Good Afternoon"),
                hasScript(),
                "scriptAfterRestore=" + hasScript());

        Files.writeString(dumpDir.resolve("matrix.txt"), String.join("\n", verdicts), StandardCharsets.UTF_8);
        Allure.addAttachment("matrix-" + localeCode, "text/plain", String.join("\n", verdicts));
        Allure.addAttachment("dump-dir", "text/plain", dumpDir.toString());
    }

    private void probe(String id, List<String> enRows, String... bugMarkers) throws Exception {
        probe(id, enRows, false, bugMarkers);
    }

    private void probe(String id, List<String> enRows, boolean scroll, String... bugMarkers)
            throws Exception {
        ensureDrawerOpen();
        boolean tapped = false;
        // Prefer English row (if still EN) then any visible row containing fragment.
        for (String c : enRows) {
            if (tap(c)) {
                tapped = true;
                break;
            }
        }
        if (!tapped) {
            for (String t : visibleTexts()) {
                for (String c : enRows) {
                    if (t.toLowerCase().contains(c.toLowerCase())) {
                        if (tap(t)) {
                            tapped = true;
                            break;
                        }
                    }
                }
                if (tapped) {
                    break;
                }
            }
        }
        // Last resort: if drawer localized, tap by approximate menu order index
        // via all TextViews that look like drawer rows (skip header/name/company).
        if (!tapped && new ProfileDrawerPage().isDisplayedNow()) {
            List<WebElement> rows = DriverManager.get().findElements(
                    By.xpath("//android.widget.TextView"));
            List<String> skip = List.of("Company Id", "Log Out", "Kasim", "L2B-");
            for (WebElement el : rows) {
                String t;
                try {
                    t = el.getText();
                } catch (RuntimeException e) {
                    continue;
                }
                if (t == null || t.isBlank() || t.length() > 60) {
                    continue;
                }
                boolean skipIt = false;
                for (String s : skip) {
                    if (t.contains(s)) {
                        skipIt = true;
                        break;
                    }
                }
                if (skipIt) {
                    continue;
                }
                // match id heuristically later — for now tap first localized-looking row once
            }
            record(id, false, true, "row not tappable; enCandidates=" + enRows
                    + "; drawerTexts=" + String.join(" | ", visibleTexts()).substring(0,
                    Math.min(300, String.join(" | ", visibleTexts()).length())));
            return;
        }
        if (!tapped) {
            record(id, false, true, "row not tappable; enCandidates=" + enRows);
            return;
        }
        sleep(1100);
        dump("d-" + id);
        if (scroll) {
            swipeUp();
            sleep(600);
            swipeUp();
            sleep(600);
            dump("d-" + id + "-scrolled");
        }
        boolean localized = hasScript();
        String bugNote = "";
        for (String m : bugMarkers) {
            if (has(m)) {
                bugNote += " EN_BUG=" + m;
            }
        }
        record(id, localized, !localized,
                "script=" + localized + bugNote);
        back();
        sleep(700);
        if (!new ProfileDrawerPage().isDisplayedNow()) {
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
        String line = surface + "\t" + verdict + "\tlocalized=" + localized
                + "\tenglish=" + stillEnglish + "\t" + note;
        verdicts.add(line);
        Allure.parameter(localeCode + "-" + surface, verdict + " " + note);
    }

    private void ensureDrawerOpen() {
        if (new ProfileDrawerPage().isDisplayedNow() || has("Log Out") || has("Account")) {
            return;
        }
        openDrawer();
        sleep(900);
        if (!new ProfileDrawerPage().isDisplayedNow() && !has("Log Out") && !has("Account")) {
            goHome();
            openDrawer();
            sleep(900);
        }
    }

    private void tapBottom(String en, int x) {
        try {
            new HomePage().tapDesc(en);
        } catch (RuntimeException ignored) {
        }
        tap(en);
        click(x, 2280);
        sleep(1100);
    }

    private boolean looksHome() {
        return has("Current Earning") || has("Booking Orders") || has("Good Afternoon")
                || has("Good Morning") || (has("Calendar") && has("Fleet")) || hasScript();
    }

    private void goHome() {
        closeDrawer();
        sleep(400);
        for (int i = 0; i < 4; i++) {
            if (looksHome()) {
                return;
            }
            if (has("Upcoming Booking") || has("Help & Support") || has("Profile Info")
                    || has("Live chat") || has("FAQ") || has("Choose the language")
                    || has("Schedule") || has("Your Fleet") || has("Add Fleet")) {
                back();
                sleep(700);
                continue;
            }
            try {
                new HomePage().tapDesc("Home");
            } catch (RuntimeException ignored) {
            }
            tap("Home");
            click(405, 2280);
            sleep(800);
        }
    }

    private void openDrawer() {
        closeDrawer();
        sleep(300);
        try {
            new HomePage().tapDesc("Profile");
        } catch (RuntimeException e) {
            if (!tapDesc("Profile")) {
                click(80, 155);
            }
        }
        sleep(900);
    }

    private void closeDrawer() {
        if (tapDesc("Close navigation menu")) {
            sleep(500);
            return;
        }
        if (new ProfileDrawerPage().isDisplayedNow() || has("Account") || has("Log Out")
                || has("Language") || hasScript()) {
            click(999, 400);
            sleep(500);
        }
    }

    private void swipeUp() {
        DriverManager.get().executeScript("mobile: swipeGesture",
                Map.of("left", 200, "top", 1400, "width", 600, "height", 700,
                        "direction", "up", "percent", 0.75));
    }

    private boolean hasScript() {
        String s = src();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= scriptLo && c <= scriptHi) {
                return true;
            }
        }
        return false;
    }

    private List<String> visibleTexts() {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = TEXT.matcher(src());
        while (m.find()) {
            String t = m.group(1).trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return new ArrayList<>(out);
    }

    private boolean has(String t) {
        return !DriverManager.get().findElements(
                By.xpath("//*[contains(@text," + lit(t) + ")]")).isEmpty()
                || src().contains(t);
    }

    private boolean tap(String text) {
        var els = DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[@text=" + lit(text) + "]"));
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

    @Override
    protected boolean tapDesc(String d) {
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
        Path dir = dumpDir.resolve(name);
        Files.createDirectories(dir);
        String xml = src();
        Files.writeString(dir.resolve("window.xml"), xml, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("texts.txt"), String.join("\n", visibleTexts()),
                StandardCharsets.UTF_8);
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
