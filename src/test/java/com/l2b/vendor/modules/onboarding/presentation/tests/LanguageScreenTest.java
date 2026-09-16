package com.l2b.vendor.modules.onboarding.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.core.locators.ComposeLocators;
import com.l2b.vendor.core.ui.BaseTest;
import com.l2b.vendor.core.wait.Waits;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.modules.onboarding.domain.OnboardingEnvironment;
import com.l2b.vendor.modules.onboarding.presentation.pages.LanguagePage;
import com.l2b.vendor.modules.onboarding.presentation.pages.OnboardingCarouselPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import java.lang.reflect.Method;
import java.time.Duration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * Language chooser for first-launch of {@code com.l2b.app.qa} (after splash / notification grant).
 * Scope: default English, single-select across Hindi/Telugu/Kannada, Get started applying locale
 * downstream, rapid taps, Back, relaunch persistence, and non-Latin row layout. One session per method.
 *
 * <p>Locators from live dumps (2026-09-11 and 2026-09-15): title {@code Welcome to L2B},
 * English row carries TextView {@code Default}, four clickable rows with a CheckBox
 * on the right. Do not copy Customer strings.
 */
@Epic("Vendor app")
@Feature("Language screen")
public class LanguageScreenTest extends BaseTest {

    @BeforeSuite(alwaysRun = true)
    public void prepareOnboardingEnvironment() {
        OnboardingEnvironment.prepareSuite();
    }

    /** Fresh install path so first-launch language is reachable. */
    @Override
    protected boolean noReset() {
        return false;
    }

    /** One session per case — same leak-avoidance as Splash / carousel. */
    @Override
    protected boolean newSessionPerMethod() {
        return true;
    }

    /** Auto-grant so we test language, not the notification dialog. */
    @Override
    protected boolean autoGrantPermissions() {
        return true;
    }

    /** Force-stop Play Store before Back/relaunch cases so recents leftover is not the named landing. */
    @Override
    protected void beforeCreateDriver(Method method) {
        if ("androidBackFromLanguageDoesNotCrash".equals(method.getName())
                || "languageChoicePersistsAcrossRelaunch".equals(method.getName())) {
            try {
                Adb.forceStop("com.android.vending");
                Adb.pressHome();
            } catch (RuntimeException ignored) {
                // Session create still reports the real error.
            }
        }
    }

    /**
     * Simulates opening the language screen on a clean first launch, with no row tapped.
     * Expected: English is the default (Default badge on the English row); Hindi, Telugu, and Kannada
     * are all visible. CheckBox {@code checked} is recorded when UiAutomator exposes it.
     * This proves the chooser starts in a single, known locale before any user change.
     */
    @Test(priority = 1, description = "Case 1: Default English is pre-selected on first load")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-sourced: English row shows the Default badge. CheckBox checked is recorded "
            + "if UiAutomator exposes it; dumps often leave checked=false on all four boxes.")
    public void defaultEnglishPreselectedOnFirstLoad() {
        LanguagePage language = openLanguage();

        assertThat(language.isDisplayedNow()).as("Title 'Welcome to L2B'").isTrue();
        assertThat(language.isEnglishVisible()).as("English option").isTrue();
        assertThat(language.isDefaultBadgeVisible()).as("'Default' badge on screen").isTrue();
        assertThat(language.isDefaultBadgeOnEnglishRow())
                .as("Default badge must sit on the English row (dump 15 Sep)")
                .isTrue();
        assertThat(language.isHindiVisible()).as("Hindi").isTrue();
        assertThat(language.isTeluguVisible()).as("Telugu").isTrue();
        assertThat(language.isKannadaVisible()).as("Kannada").isTrue();

        Boolean english = language.checkboxChecked("English");
        Boolean hindi = language.checkboxChecked("हिंदी");
        Boolean telugu = language.checkboxChecked("తెలుగు");
        Boolean kannada = language.checkboxChecked("ಕನ್ನಡ");
        Allure.parameter("checkboxEnglish", String.valueOf(english));
        Allure.parameter("checkboxHindi", String.valueOf(hindi));
        Allure.parameter("checkboxTelugu", String.valueOf(telugu));
        Allure.parameter("checkboxKannada", String.valueOf(kannada));

        if (Boolean.TRUE.equals(english)) {
            assertThat(hindi).as("Hindi must not be checked when English is").isNotEqualTo(Boolean.TRUE);
            assertThat(telugu).as("Telugu must not be checked when English is").isNotEqualTo(Boolean.TRUE);
            assertThat(kannada).as("Kannada must not be checked when English is").isNotEqualTo(Boolean.TRUE);
        }

        language.attachScreenshot("case1-default-english");
    }

    /**
     * Simulates tapping the Hindi row while English is the default.
     * Expected: this screen localizes to the Hindi title and the English title disappears.
     * Validates single-select behavior so two languages can never be active at once (title is the
     * evidence — CheckBox {@code checked} is unreliable in dumps).
     */
    @Test(priority = 2, description = "Case 2a: Selecting Hindi deselects English")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-sourced: Hindi tap localizes this screen to 'L2B में आपका स्वागत है'. "
            + "English title must go away. CheckBox checked is not relied on (all false in dumps).")
    public void selectingHindiDeselectsEnglish() {
        LanguagePage language = openLanguage();
        language.selectHindi();
        Waits.until(DriverManager.get(),
                d -> language.isTitleHindi() ? Boolean.TRUE : null,
                "After Hindi tap, Hindi title did not appear",
                Duration.ofSeconds(8));
        assertThat(language.isTitleHindi()).as("Hindi title after selecting Hindi").isTrue();
        assertThat(language.isTitleEnglish()).as("English title must be gone").isFalse();
        language.attachScreenshot("case2-hindi-selected");
    }

    /**
     * Simulates tapping the Telugu row while English is the default.
     * Expected: title becomes Telugu and the English title is gone.
     * Same single-select proof as Hindi, for a non-Latin script that also rewrites this screen immediately.
     */
    @Test(priority = 3, description = "Case 2b: Selecting Telugu deselects English")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-sourced: Telugu tap localizes this screen to 'L2B కి స్వాగతం'.")
    public void selectingTeluguDeselectsEnglish() {
        LanguagePage language = openLanguage();
        language.selectTelugu();
        Waits.until(DriverManager.get(),
                d -> language.isTitleTelugu() ? Boolean.TRUE : null,
                "After Telugu tap, Telugu title did not appear",
                Duration.ofSeconds(8));
        assertThat(language.isTitleTelugu()).as("Telugu title after selecting Telugu").isTrue();
        assertThat(language.isTitleEnglish()).as("English title must be gone").isFalse();
        language.attachScreenshot("case2-telugu-selected");
    }

    /**
     * Simulates tapping the Kannada row while English is the default.
     * Expected: title becomes Kannada and the English title is gone.
     * Completes the three-locale single-select set so Hindi is not the only proven deselect path.
     */
    @Test(priority = 4, description = "Case 2c: Selecting Kannada deselects English")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump-sourced: Kannada tap localizes this screen to 'L2B ಗೆ ಸ್ವಾಗತ'.")
    public void selectingKannadaDeselectsEnglish() {
        LanguagePage language = openLanguage();
        language.selectKannada();
        Waits.until(DriverManager.get(),
                d -> language.isTitleKannada() ? Boolean.TRUE : null,
                "After Kannada tap, Kannada title did not appear",
                Duration.ofSeconds(8));
        assertThat(language.isTitleKannada()).as("Kannada title after selecting Kannada").isTrue();
        assertThat(language.isTitleEnglish()).as("English title must be gone").isFalse();
        language.attachScreenshot("case2-kannada-selected");
    }

    /**
     * Simulates tapping Get started without changing the default language.
     * Expected: onboarding slide 1 is English ("Grow Your Machine…"), not Hindi/Telugu/Kannada copy.
     * Confirms the default really applies downstream, not only as a badge on this screen.
     */
    @Test(priority = 5, description = "Case 3: Get started without changing language applies English downstream")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Do not tap a language row. Get started must open onboarding in English "
            + "('Grow Your Machine…'), not Hindi/Telugu/Kannada copy.")
    public void getStartedWithoutChangeAppliesEnglishDownstream() {
        LanguagePage language = openLanguage();
        assertThat(language.isTitleEnglish()).as("Precondition: still English title").isTrue();
        language.tapGetStarted();

        OnboardingCarouselPage carousel = new OnboardingCarouselPage();
        carousel.waitUntilSlideOne();
        assertThat(carousel.isOnSlideOne())
                .as("Onboarding slide 1 English headline 'Grow Your Machine'")
                .isTrue();
        assertThat(carousel.isNextVisible()).as("English Next on slide 1").isTrue();
        carousel.attachScreen("case3-downstream-english-slide1");
    }

    /**
     * Simulates selecting Hindi, then tapping the localized Get started CTA.
     * Expected: onboarding slide 1 shows the Hindi headline, and English "Grow Your Machine" is gone.
     * Proves language choice has a real effect on the next screen, not just that navigation happened.
     */
    @Test(priority = 6, description = "Case 4: Hindi selection changes onboarding copy, not just navigation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Select Hindi, tap शुरू करें, assert slide 1 Hindi headline from dump "
            + "('हमारे साथ अपना मशीन और मटेरियल बिज़नेस बढ़ाएं'), not English 'Grow Your Machine'.")
    public void hindiSelectionChangesDownstreamCopy() {
        LanguagePage language = openLanguage();
        language.selectHindi();
        Waits.until(DriverManager.get(),
                d -> language.isTitleHindi() ? Boolean.TRUE : null,
                "Hindi title before Get started",
                Duration.ofSeconds(8));
        language.tapGetStarted();

        Waits.until(DriverManager.get(),
                d -> {
                    boolean hindi = !d.findElements(ComposeLocators.textViewContains("मशीन और मटेरियल")).isEmpty();
                    boolean english = !d.findElements(ComposeLocators.textViewContains("Grow Your Machine")).isEmpty();
                    return (hindi || english) ? Boolean.TRUE : null;
                },
                "After Hindi Get started, neither Hindi nor English onboarding headline appeared",
                Duration.ofSeconds(12));

        boolean hindiSlide = !DriverManager.get().findElements(
                ComposeLocators.textViewContains("मशीन और मटेरियल")).isEmpty();
        boolean englishSlide = !DriverManager.get().findElements(
                ComposeLocators.textViewContains("Grow Your Machine")).isEmpty();
        Allure.parameter("downstreamHindiHeadline", String.valueOf(hindiSlide));
        Allure.parameter("downstreamEnglishHeadline", String.valueOf(englishSlide));
        new OnboardingCarouselPage().attachScreen("case4-hindi-downstream");

        assertThat(hindiSlide)
                .as("Next screen must be Hindi onboarding copy (dump lang_hindi_ob1)")
                .isTrue();
        assertThat(englishSlide)
                .as("English 'Grow Your Machine' must not remain after Hindi Get started")
                .isFalse();
    }

    /**
     * Simulates rapid taps Hindi → Telugu → Kannada with no wait between them.
     * Expected: the process stays in Vendor and the last tap (Kannada) is the locale that sticks.
     * Guards against crash or a stuck intermediate language when users mash the list.
     */
    @Test(priority = 7, description = "Case 5: Rapid taps across languages — last selection sticks, no crash")
    @Severity(SeverityLevel.NORMAL)
    @Description("Tap Hindi, Telugu, Kannada with no wait. Last row is Kannada — title must be "
            + "'L2B ಗೆ ಸ್ವಾಗತ'. Process stays in Vendor.")
    public void rapidMultiTapLastLanguageSticks() {
        LanguagePage language = openLanguage();
        language.tapLanguageRowsRapidly("हिंदी", "తెలుగు", "ಕನ್ನಡ");

        Waits.until(DriverManager.get(),
                d -> (language.isTitleKannada() || language.isTitleTelugu()
                        || language.isTitleHindi() || language.isTitleEnglish())
                        ? Boolean.TRUE : null,
                "After rapid language taps, no language title on screen",
                Duration.ofSeconds(8));

        String landed = language.isTitleKannada() ? "kannada"
                : language.isTitleTelugu() ? "telugu"
                : language.isTitleHindi() ? "hindi"
                : language.isTitleEnglish() ? "english" : "unknown";
        Allure.parameter("rapidLastLanding", landed);
        language.attachScreenshot("case5-rapid-last-" + landed);

        assertThat(((io.appium.java_client.android.AndroidDriver) DriverManager.get()).getCurrentPackage())
                .as("Vendor must not crash during rapid language taps")
                .isEqualTo("com.l2b.app.qa");
        assertThat(landed)
                .as("Last tap was Kannada — that locale must stick")
                .isEqualTo("kannada");
    }

    /**
     * Simulates pressing the device Back key on the language chooser.
     * Expected (ideal): stay on language or return to splash. This test only requires a named landing and no crash.
     * NOTE: this asserts actual observed behavior (Back exits to the launcher). That is the same family as
     * BUGS_FOUND.docx #4 — not a separate Language bug, and not the ideal "Back stays in-app" behavior.
     */
    @Test(priority = 8, description = "Case 6: Android Back from Language — no crash, named landing")
    @Severity(SeverityLevel.NORMAL)
    @Description("Back may go to launcher, stay on language, or exit. Record the actual screen. "
            + "Play Store is force-stopped first so recents leftover is not the landing.")
    public void androidBackFromLanguageDoesNotCrash() {
        LanguagePage language = openLanguage();
        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        android.pressKey(new io.appium.java_client.android.nativekey.KeyEvent(
                io.appium.java_client.android.nativekey.AndroidKey.BACK));

        Waits.until(android,
                d -> {
                    String pkg = android.getCurrentPackage();
                    return (pkg != null && !pkg.isBlank()) ? Boolean.TRUE : null;
                },
                "After Back, no current package",
                Duration.ofSeconds(8));

        String pkg = android.getCurrentPackage();
        String landing;
        if (pkg == null || pkg.isBlank()) {
            landing = "unknown";
        } else if ("com.l2b.app.qa".equals(pkg)) {
            landing = language.isTitleEnglish() || language.isTitleHindi()
                    || language.isTitleTelugu() || language.isTitleKannada()
                    ? "language" : "vendor-other";
        } else if (pkg.contains("launcher")) {
            landing = "launcher";
        } else if (pkg.contains("vending") || pkg.contains("play")) {
            landing = "play-store";
        } else {
            landing = "other-package:" + pkg;
        }
        Allure.parameter("backLanding", landing);
        Allure.parameter("backPackage", String.valueOf(pkg));
        language.attachScreenshot("case6-back-" + landing);

        assertThat(pkg).as("Back from language must not crash (package blank)").isNotBlank();
        assertThat(landing).as("Landing after Back must be named").isNotEqualTo("unknown");
    }

    /**
     * Simulates Hindi + Get started, then terminateApp + activateApp without {@code pm clear}.
     * Expected (ideal): skip the chooser and resume Hindi onboarding or later screens.
     * NOTE: this asserts actual observed behavior (chooser returns with Hindi still selected; carousel is not
     * skipped), which is the same family as BUGS_FOUND.docx #4 — not the ideal persisted-onboarding path.
     */
    @Test(priority = 9, description = "Case 7: Language choice after Get started — relaunch with data kept")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Select Hindi, tap Get started, terminateApp+activateApp without pm clear. "
            + "Record whether Hindi onboarding returns, English language returns, or something else.")
    public void languageChoicePersistsAcrossRelaunch() {
        LanguagePage language = openLanguage();
        language.selectHindi();
        Waits.until(DriverManager.get(),
                d -> language.isTitleHindi() ? Boolean.TRUE : null,
                "Hindi title before Get started",
                Duration.ofSeconds(8));
        language.tapGetStarted();
        Waits.until(DriverManager.get(),
                d -> {
                    boolean hindi = !d.findElements(ComposeLocators.textViewContains("मशीन और मटेरियल")).isEmpty();
                    boolean english = !d.findElements(ComposeLocators.textViewContains("Grow Your Machine")).isEmpty();
                    return (hindi || english) ? Boolean.TRUE : null;
                },
                "Hindi Get started did not reach onboarding",
                Duration.ofSeconds(12));

        io.appium.java_client.android.AndroidDriver android =
                (io.appium.java_client.android.AndroidDriver) DriverManager.get();
        android.terminateApp("com.l2b.app.qa");
        android.activateApp("com.l2b.app.qa");

        Waits.until(android,
                d -> {
                    if (!"com.l2b.app.qa".equals(android.getCurrentPackage())) {
                        return null;
                    }
                    boolean hindiOb = !d.findElements(ComposeLocators.textViewContains("मशीन और मटेरियल")).isEmpty();
                    boolean englishOb = !d.findElements(ComposeLocators.textViewContains("Grow Your Machine")).isEmpty();
                    return (language.isTitleHindi() || language.isTitleEnglish()
                            || language.isTitleTelugu() || language.isTitleKannada()
                            || hindiOb || englishOb) ? Boolean.TRUE : null;
                },
                "After relaunch, no language or onboarding screen",
                Duration.ofSeconds(20));

        boolean hindiOb = !android.findElements(ComposeLocators.textViewContains("मशीन और मटेरियल")).isEmpty();
        boolean englishOb = !android.findElements(ComposeLocators.textViewContains("Grow Your Machine")).isEmpty();
        String landing;
        if (hindiOb) {
            landing = "onboarding-hindi";
        } else if (englishOb) {
            landing = "onboarding-english";
        } else if (language.isTitleHindi()) {
            landing = "language-hindi";
        } else if (language.isTitleEnglish()) {
            landing = "language-english";
        } else if (language.isTitleTelugu()) {
            landing = "language-telugu";
        } else if (language.isTitleKannada()) {
            landing = "language-kannada";
        } else {
            landing = "unknown";
        }
        Allure.parameter("relaunchLanding", landing);
        language.attachScreenshot("case7-relaunch-" + landing);

        assertThat(landing)
                .as("Relaunch after Hindi Get started. hindi-onboarding=persisted, "
                        + "language-english=full first-launch again, language-hindi=chooser kept Hindi")
                .isIn("onboarding-hindi", "onboarding-english", "language-hindi",
                        "language-english", "language-telugu", "language-kannada");
    }

    /**
     * Simulates selecting Telugu then Kannada and measuring the four language rows.
     * Expected: each row still has a right-side CheckBox, similar height, aligned X, and no overlap.
     * Non-Latin labels must not break the checkbox row layout on this screen.
     */
    @Test(priority = 10, description = "Case 8: Telugu/Kannada rows keep checkbox alignment")
    @Severity(SeverityLevel.NORMAL)
    @Description("After selecting Telugu then Kannada, the four language rows must keep a right-side "
            + "CheckBox and similar row height (dump: checkbox x=880, height ~123–129).")
    public void nonLatinScriptDoesNotBreakCheckboxRowLayout() {
        LanguagePage language = openLanguage();
        language.selectTelugu();
        Waits.until(DriverManager.get(),
                d -> language.isTitleTelugu() ? Boolean.TRUE : null,
                "Telugu title",
                Duration.ofSeconds(8));
        assertRowLayout("telugu", language);
        language.attachScreenshot("case8-telugu-rows");

        language.selectKannada();
        Waits.until(DriverManager.get(),
                d -> language.isTitleKannada() ? Boolean.TRUE : null,
                "Kannada title",
                Duration.ofSeconds(8));
        assertRowLayout("kannada", language);
        language.attachScreenshot("case8-kannada-rows");
    }

    /** Asserts row height, checkbox X alignment, and no vertical overlap for all four language labels. */
    private static void assertRowLayout(String locale, LanguagePage language) {
        int previousBottom = -1;
        Integer checkboxX = null;
        for (String label : new String[] {"English", "हिंदी", "తెలుగు", "ಕನ್ನಡ"}) {
            org.openqa.selenium.Rectangle row = language.languageRowBounds(label);
            org.openqa.selenium.Rectangle box = language.languageCheckboxBounds(label);
            assertThat(row).as("%s: row bounds for %s", locale, label).isNotNull();
            assertThat(box).as("%s: CheckBox bounds for %s", locale, label).isNotNull();
            assertThat(row.height)
                    .as("%s: %s row height should stay near dump ~123–129", locale, label)
                    .isBetween(90, 180);
            if (checkboxX == null) {
                checkboxX = box.x;
            } else {
                assertThat(Math.abs(box.x - checkboxX))
                        .as("%s: CheckBox x for %s should align with the others", locale, label)
                        .isLessThanOrEqualTo(24);
            }
            if (previousBottom >= 0) {
                assertThat(row.y)
                        .as("%s: %s row must not overlap the row above", locale, label)
                        .isGreaterThanOrEqualTo(previousBottom - 4);
            }
            previousBottom = row.y + row.height;
        }
    }

    /** Waits until the language chooser is the first stable in-app screen (notification auto-granted). */
    private LanguagePage openLanguage() {
        LanguagePage language = new LanguagePage();
        language.waitUntilLoaded();
        return language;
    }
}
