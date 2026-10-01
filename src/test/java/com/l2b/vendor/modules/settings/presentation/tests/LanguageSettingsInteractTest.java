package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.settings.presentation.pages.LanguageSettingsPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Language settings interact. Save allowed; restore English after locale apply.
 *
 * <p><b>Happy Path (LS-I1–I12)</b>
 * <ol>
 *   <li>I1 Select Hindi (pre-Save chrome)</li>
 *   <li>I2 Hindi + Save → drawer Hindi (खाता)</li>
 *   <li>I3 Restore English + Save → drawer English</li>
 *   <li>I4 Select Telugu then English Cancel via Back (no Save)</li>
 *   <li>I5 Select Kannada then English + Save</li>
 *   <li>I6 Header Back → drawer</li>
 *   <li>I7 Device Back → drawer</li>
 *   <li>I8 Reopen Language after Back</li>
 *   <li>I9 Hindi Save localizes Language chrome (भाषा चुनें / सेव करें)</li>
 *   <li>I10 English Save restores Choose the language / Save</li>
 *   <li>I11 Rapid Hindi→English Save stays Vendor</li>
 *   <li>I12 Default badge present on English row path</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Language settings interact — rental 9000000001")
public class LanguageSettingsInteractTest extends LanguageSettingsBaseTest {

    @AfterMethod(alwaysRun = true)
    public void cleanupEnglish() {
        restoreEnglishLanguage();
    }

    @Test(priority = 1, description = "LS-I1: select Hindi pre-Save")
    @Severity(SeverityLevel.CRITICAL)
    public void selectHindiPreSave() {
        LanguageSettingsPage page = reachLanguage();
        page.selectHindi();
        sleepQuiet(600);
        assertThat(page.isHindiVisible()).isTrue();
        assertThat(page.isSaveVisible()).isTrue();
        page.tapHeaderBack();
        sleepQuiet(800);
    }

    @Test(priority = 2, description = "LS-I2: Hindi + Save → drawer Hindi")
    @Severity(SeverityLevel.BLOCKER)
    @Description("BUGS_FOUND #40: Log Out / Company Id may stay English.")
    public void hindiSaveLocalizesDrawer() {
        applyHindi();
        assertThat(hasText("खाता") || hasText("भाषा") || hasText("सहायता"))
                .as("drawer Hindi rows").isTrue();
        Allure.parameter("logOutStillEnglish", String.valueOf(hasText("Log Out")));
        Allure.parameter("companyIdEnglish", String.valueOf(hasText("Company Id")));
        // Document known #40 without failing the apply path
        assertThat(hasText("खाता")).as("Account→खाता").isTrue();
    }

    @Test(priority = 3, description = "LS-I3: restore English + Save → drawer English")
    @Severity(SeverityLevel.BLOCKER)
    public void restoreEnglishLocalizesDrawer() {
        applyHindi();
        restoreEnglishLanguage();
        sleepQuiet(800);
        if (!profileDrawerNow() && !hasText("Account")) {
            new com.l2b.vendor.modules.home.presentation.pages.HomePage().tapDesc("Profile");
            sleepQuiet(900);
        }
        assertThat(hasText("Account") || hasText("Language")).as("drawer English").isTrue();
        assertThat(hasText("खाता")).as("Hindi Account gone").isFalse();
    }

    @Test(priority = 4, description = "LS-I4: Telugu then Back without Save")
    @Severity(SeverityLevel.CRITICAL)
    public void teluguThenBackWithoutSave() {
        LanguageSettingsPage page = reachLanguage();
        page.selectTelugu();
        sleepQuiet(500);
        page.tapHeaderBack();
        sleepQuiet(800);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 5, description = "LS-I5: Kannada then English + Save")
    @Severity(SeverityLevel.CRITICAL)
    public void kannadaThenEnglishSave() {
        LanguageSettingsPage page = reachLanguage();
        page.selectKannada();
        sleepQuiet(500);
        page.selectEnglish();
        sleepQuiet(400);
        page.tapSave();
        sleepQuiet(1200);
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 6, description = "LS-I6: header Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void headerBackToDrawer() {
        LanguageSettingsPage page = reachLanguage();
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 7, description = "LS-I7: device Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBackToDrawer() {
        LanguageSettingsPage page = reachLanguage();
        page.pressDeviceBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 8, description = "LS-I8: reopen Language after Back")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterBack() {
        LanguageSettingsPage page = reachLanguage();
        page.tapHeaderBack();
        sleepQuiet(800);
        new ProfileDrawerPage().tapRow("Language");
        sleepQuiet(1000);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 9, description = "LS-I9: Hindi Save localizes Language chrome")
    @Severity(SeverityLevel.CRITICAL)
    public void hindiSaveLocalizesLanguageChrome() {
        applyHindi();
        if (hasText("भाषा")) {
            tapText("भाषा");
        } else {
            reachLanguage();
        }
        sleepQuiet(1100);
        LanguageSettingsPage page = new LanguageSettingsPage();
        page.waitUntilLoaded();
        assertThat(page.isChooseLanguageVisible() || hasText("भाषा चुनें")).isTrue();
        assertThat(page.isSaveVisible()).isTrue();
        page.attachScreenshot("ls-i9-hindi-chrome");
    }

    @Test(priority = 10, description = "LS-I10: English Save restores EN drawer chrome")
    @Severity(SeverityLevel.CRITICAL)
    public void englishSaveRestoresChrome() {
        applyHindi();
        assertThat(hasText("खाता")).as("pre: Hindi drawer").isTrue();
        restoreEnglishLanguage();
        sleepQuiet(800);
        if (!hasText("Account") && !profileDrawerNow()) {
            try {
                new com.l2b.vendor.modules.home.presentation.pages.HomePage().tapDesc("Profile");
                sleepQuiet(900);
            } catch (RuntimeException ignored) {
            }
        }
        assertThat(hasText("Account") || hasText("Language")).as("EN drawer restored").isTrue();
        assertThat(hasText("खाता")).as("Hindi Account gone").isFalse();
    }

    @Test(priority = 11, description = "LS-I11: rapid Hindi→English Save stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidHindiEnglishSave() {
        LanguageSettingsPage page = reachLanguage();
        page.selectHindi();
        sleepQuiet(400);
        page.tapSave();
        sleepQuiet(1000);
        restoreEnglishLanguage();
        assertThat(vendorPackage()).contains("l2b");
        assertThat(launcherNow()).isFalse();
    }

    @Test(priority = 12, description = "LS-I12: Default badge on English path")
    @Severity(SeverityLevel.NORMAL)
    public void defaultBadgeOnEnglish() {
        LanguageSettingsPage page = reachLanguage();
        page.selectEnglish();
        sleepQuiet(400);
        assertThat(page.isDefaultVisible()).isTrue();
    }
}
