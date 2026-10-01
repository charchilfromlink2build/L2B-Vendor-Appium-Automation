package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.settings.presentation.pages.LanguageSettingsPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Language settings landing — Profile → Language on {@code 9000000001}.
 * Dump {@code /tmp/l2b-lang-settings-0001-20261001}.
 *
 * <p><b>Happy Path (LS-L1–L10)</b>
 * <ol>
 *   <li>L1 Drawer Language → Choose the language</li>
 *   <li>L2 Title + Back + Save</li>
 *   <li>L3 All language heading</li>
 *   <li>L4 English + Default badge</li>
 *   <li>L5 Hindi / Telugu / Kannada rows</li>
 *   <li>L6 All four languages present</li>
 *   <li>L7 Stay Vendor package</li>
 *   <li>L8 Header Back aligned</li>
 *   <li>L9 Save near bottom</li>
 *   <li>L10 Soft layout batch</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Language settings landing — rental 9000000001")
public class LanguageSettingsLandingTest extends LanguageSettingsBaseTest {

    @Test(priority = 1, description = "LS-L1: drawer Language → Choose the language")
    @Severity(SeverityLevel.BLOCKER)
    public void languageOpensFromDrawer() {
        LanguageSettingsPage page = reachLanguage();
        page.attachScreenshot("ls-l1");
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 2, description = "LS-L2: title + Back + Save")
    @Severity(SeverityLevel.CRITICAL)
    public void titleBackSaveChrome() {
        LanguageSettingsPage page = reachLanguage();
        assertThat(page.isChooseLanguageVisible()).isTrue();
        assertThat(page.isBackVisible()).isTrue();
        assertThat(page.isSaveVisible()).isTrue();
    }

    @Test(priority = 3, description = "LS-L3: All language heading")
    @Severity(SeverityLevel.CRITICAL)
    public void allLanguageHeading() {
        LanguageSettingsPage page = reachLanguage();
        assertThat(page.isAllLanguageVisible()).isTrue();
    }

    @Test(priority = 4, description = "LS-L4: English + Default badge")
    @Severity(SeverityLevel.CRITICAL)
    public void englishAndDefault() {
        LanguageSettingsPage page = reachLanguage();
        assertThat(page.isEnglishVisible()).isTrue();
        assertThat(page.isDefaultVisible()).isTrue();
    }

    @Test(priority = 5, description = "LS-L5: Hindi Telugu Kannada rows")
    @Severity(SeverityLevel.CRITICAL)
    public void hindiTeluguKannadaRows() {
        LanguageSettingsPage page = reachLanguage();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isHindiVisible()).as("Hindi").isTrue();
        softly.assertThat(page.isTeluguVisible()).as("Telugu").isTrue();
        softly.assertThat(page.isKannadaVisible()).as("Kannada").isTrue();
        softly.assertAll();
    }

    @Test(priority = 6, description = "LS-L6: all four languages present")
    @Severity(SeverityLevel.CRITICAL)
    public void allFourLanguages() {
        LanguageSettingsPage page = reachLanguage();
        assertThat(page.allLanguageRowsVisible()).isTrue();
    }

    @Test(priority = 7, description = "LS-L7: stay Vendor package")
    @Severity(SeverityLevel.BLOCKER)
    public void staysVendorPackage() {
        LanguageSettingsPage page = reachLanguage();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 8, description = "LS-L8: header Back aligned")
    @Severity(SeverityLevel.NORMAL)
    public void headerBackAligned() {
        LanguageSettingsPage page = reachLanguage();
        assertThat(page.headerBackAligned()).isTrue();
    }

    @Test(priority = 9, description = "LS-L9: Save near bottom")
    @Severity(SeverityLevel.NORMAL)
    public void saveNearBottom() {
        LanguageSettingsPage page = reachLanguage();
        assertThat(page.saveNearBottomOk()).isTrue();
    }

    @Test(priority = 10, description = "LS-L10: soft layout batch")
    @Severity(SeverityLevel.NORMAL)
    public void softLayoutBatch() {
        LanguageSettingsPage page = reachLanguage();
        assertThat(page.isDisplayedNow()).as("hard").isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.headerBackAligned()).as("Back|title").isTrue();
        softly.assertThat(page.saveNearBottomOk()).as("Save bottom").isTrue();
        softly.assertThat(page.allLanguageRowsVisible()).as("4 langs").isTrue();
        softly.assertAll();
    }
}
