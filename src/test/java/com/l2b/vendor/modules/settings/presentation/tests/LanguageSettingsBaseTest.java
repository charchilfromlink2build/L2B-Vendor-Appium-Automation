package com.l2b.vendor.modules.settings.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.LanguageSettingsPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * Shared path onto in-app Language settings. Save allowed. Always restore English after
 * locale probes. Never Accept / Decline / Log Out Confirm.
 */
public abstract class LanguageSettingsBaseTest extends ProfileDrawerBaseTest {

    @org.testng.annotations.BeforeMethod(alwaysRun = true)
    public void ensureEnglishBeforeMethod() {
        // Account language persists server-side; prefer English before each case when already logged in.
        try {
            if (DriverManager.hasDriver() && (hasText("खाता") || hasText("भाषा चुनें")
                    || hasText("वर्तमान कमाई") || hasNonLatinIndicScript())) {
                restoreEnglishLanguage();
            }
        } catch (RuntimeException ignored) {
        }
    }

    @Step("Reach Language via Profile → Language")
    protected LanguageSettingsPage reachLanguage() {
        ProfileDrawerPage drawer = reachProfileDrawer();
        drawer.tapRow("Language");
        LanguageSettingsPage page = new LanguageSettingsPage();
        page.waitUntilLoaded();
        Allure.parameter("entry", "drawer-Language");
        Allure.parameter("after", classifyRentalNow());
        return page;
    }

    @Step("Restore English language + Save (cleanup)")
    protected void restoreEnglishLanguage() {
        try {
            for (int i = 0; i < 6; i++) {
                LanguageSettingsPage page = new LanguageSettingsPage();
                if (page.isDisplayedNow()) {
                    page.selectEnglish();
                    sleepQuiet(400);
                    page.tapSave();
                    sleepQuiet(1200);
                    return;
                }
                if (profileDrawerNow() || hasText("Account") || hasText("खाता")) {
                    if (hasText("Language")) {
                        new ProfileDrawerPage().tapRow("Language");
                    } else if (hasText("भाषा")) {
                        tapText("भाषा");
                    } else {
                        break;
                    }
                    sleepQuiet(1000);
                    continue;
                }
                if ("home".equals(classifyRentalNow()) || new HomePage().isDisplayedNow()) {
                    new HomePage().tapDesc("Profile");
                    sleepQuiet(900);
                    continue;
                }
                DriverManager.get().navigate().back();
                sleepQuiet(600);
            }
            // last resort: Profile → Language
            reachLanguage();
            LanguageSettingsPage page = new LanguageSettingsPage();
            page.selectEnglish();
            sleepQuiet(400);
            page.tapSave();
            sleepQuiet(1200);
        } catch (RuntimeException e) {
            Allure.parameter("restoreEnglishFail", e.getClass().getSimpleName());
        }
    }

    @Step("Apply Hindi and return (drawer usually open)")
    protected void applyHindi() {
        LanguageSettingsPage page = reachLanguage();
        page.selectHindi();
        sleepQuiet(500);
        page.tapSave();
        sleepQuiet(1300);
    }

    @Step("Apply Telugu and return (drawer usually open)")
    protected void applyTelugu() {
        LanguageSettingsPage page = reachLanguage();
        page.selectTelugu();
        sleepQuiet(500);
        page.tapSave();
        sleepQuiet(1300);
    }

    @Step("Apply Kannada and return (drawer usually open)")
    protected void applyKannada() {
        LanguageSettingsPage page = reachLanguage();
        page.selectKannada();
        sleepQuiet(500);
        page.tapSave();
        sleepQuiet(1300);
    }

    protected boolean hasScriptInRange(int lo, int hi) {
        String src = "";
        try {
            src = DriverManager.get().getPageSource();
        } catch (RuntimeException e) {
            return false;
        }
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c >= lo && c <= hi) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasTeluguOnScreen() {
        return hasScriptInRange(0x0C00, 0x0C7F);
    }

    protected boolean hasKannadaOnScreen() {
        return hasScriptInRange(0x0C80, 0x0CFF);
    }

    protected boolean hasDevanagariOnScreen() {
        String src = "";
        try {
            src = DriverManager.get().getPageSource();
        } catch (RuntimeException e) {
            return false;
        }
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c >= 0x0900 && c <= 0x097F) {
                return true;
            }
        }
        return false;
    }

    /** Telugu / Kannada / Devanagari present — used to force English restore. */
    protected boolean hasNonLatinIndicScript() {
        String src = "";
        try {
            src = DriverManager.get().getPageSource();
        } catch (RuntimeException e) {
            return false;
        }
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if ((c >= 0x0900 && c <= 0x097F)
                    || (c >= 0x0C00 && c <= 0x0C7F)
                    || (c >= 0x0C80 && c <= 0x0CFF)) {
                return true;
            }
        }
        return false;
    }

    protected void tapText(String text) {
        var els = DriverManager.get().findElements(
                By.xpath("//android.widget.TextView[@text='" + text + "']"));
        if (els.isEmpty()) {
            els = DriverManager.get().findElements(
                    By.xpath("//android.widget.TextView[contains(@text,'" + text + "')]"));
        }
        if (!els.isEmpty()) {
            var r = els.get(0).getRect();
            DriverManager.get().executeScript("mobile: clickGesture",
                    java.util.Map.of("x", r.x + Math.max(1, r.width / 2),
                            "y", r.y + Math.max(1, r.height / 2)));
        }
    }

    protected void closeDrawerLocalized() {
        if (tapDesc("Close navigation menu") || tapDesc("नेविगेशन मेन्यू बंद करें")) {
            sleepQuiet(600);
            return;
        }
        DriverManager.get().executeScript("mobile: clickGesture",
                java.util.Map.of("x", 999, "y", 400));
        sleepQuiet(600);
    }

    protected boolean tapDesc(String desc) {
        var els = DriverManager.get().findElements(By.xpath("//*[@content-desc='" + desc + "']"));
        if (els.isEmpty()) {
            return false;
        }
        var r = els.get(0).getRect();
        DriverManager.get().executeScript("mobile: clickGesture",
                java.util.Map.of("x", r.x + Math.max(1, r.width / 2),
                        "y", r.y + Math.max(1, r.height / 2)));
        return true;
    }
}
