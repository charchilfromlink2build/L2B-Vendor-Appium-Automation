package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.LanguageSettingsPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Language settings edge + reflection. Documents BUGS_FOUND #40–#46 when strings stay English.
 *
 * <p><b>Edge / Reflection (LS-E1–E16)</b>
 * <ol>
 *   <li>E1 Rapid open/Back Language</li>
 *   <li>E2 Layout soft</li>
 *   <li>E3 Hindi Save → Home reflects (शुभ / वर्तमान कमाई)</li>
 *   <li>E4 Hindi Save → Calendar reflects (अनुसूची) — #45 month EN</li>
 *   <li>E5 Hindi Save → Earning reflects — #44 incentive EN</li>
 *   <li>E6 Hindi Save → Fleet reflects</li>
 *   <li>E7 Hindi Save → Account reflects</li>
 *   <li>E8 Hindi Save → Help reflects</li>
 *   <li>E9 Hindi Save → Refer partial (#41 steps EN)</li>
 *   <li>E10 Hindi Save → FAQ partial (#42 mixed)</li>
 *   <li>E11 Hindi Save → Terms still EN (#43)</li>
 *   <li>E12 Hindi Save → Policies partial (#46)</li>
 *   <li>E13 Hindi Save → Settings reflects</li>
 *   <li>E14 Drawer Log Out stays EN (#40)</li>
 *   <li>E15 Package Vendor through Hindi/English cycle</li>
 *   <li>E16 No Accept/Decline on Language path</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Language settings edge/reflection — rental 9000000001")
public class LanguageSettingsEdgeTest extends LanguageSettingsBaseTest {

    @AfterMethod(alwaysRun = true)
    public void cleanupEnglish() {
        restoreEnglishLanguage();
    }

    @Test(priority = 1, description = "LS-E1: rapid open/Back Language")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidOpenBack() {
        LanguageSettingsPage page = reachLanguage();
        for (int i = 0; i < 3; i++) {
            page.tapHeaderBack();
            sleepQuiet(700);
            if (i < 2) {
                if (profileDrawerNow() || hasText("Account") || hasText("खाता")) {
                    if (hasText("Language")) {
                        new ProfileDrawerPage().tapRow("Language");
                    } else if (hasText("भाषा")) {
                        tapText("भाषा");
                    } else {
                        page = reachLanguage();
                    }
                    sleepQuiet(1000);
                    page.waitUntilLoaded();
                } else {
                    page = reachLanguage();
                }
            }
        }
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 2, description = "LS-E2: layout soft")
    @Severity(SeverityLevel.NORMAL)
    public void layoutSoft() {
        LanguageSettingsPage page = reachLanguage();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.headerBackAligned()).isTrue();
        softly.assertThat(page.saveNearBottomOk()).isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "LS-E3: Hindi → Home reflects")
    @Severity(SeverityLevel.BLOCKER)
    public void hindiHomeReflects() {
        applyHindi();
        closeDrawerLocalized();
        sleepQuiet(900);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("वर्तमान कमाई") || hasText("शुभ") || hasText("आगामी बुकिंग"))
                .as("Home Hindi chrome").isTrue();
        softly.assertThat(hasText("Current Earning")).as("EN Current Earning gone").isFalse();
        softly.assertAll();
    }

    @Test(priority = 4, description = "LS-E4: Hindi → Calendar reflects (#45 month may stay EN)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #45: month label October may remain English.")
    public void hindiCalendarReflects() {
        applyHindi();
        closeDrawerLocalized();
        sleepQuiet(700);
        tapDesc("Calendar");
        tapText("कैलेंडर");
        sleepQuiet(1100);
        assertThat(hasText("अनुसूची") || hasDevanagariOnScreen()).as("Schedule Hindi").isTrue();
        Allure.parameter("monthStillOctober", String.valueOf(hasText("October")));
        // soft-document #45
        if (hasText("October")) {
            Allure.parameter("bug", "#45 Calendar month English");
        }
    }

    @Test(priority = 5, description = "LS-E5: Hindi → Earning reflects (#44 incentive EN)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #44: incentive description may stay English.")
    public void hindiEarningReflects() {
        applyHindi();
        closeDrawerLocalized();
        sleepQuiet(700);
        tapDesc("Earning");
        tapText("कमाई");
        sleepQuiet(1100);
        assertThat(hasText("कमाई और प्रोत्साहन") || hasText("वॉलेट बैलेंस")).isTrue();
        boolean incentiveEn = hasText("Once they reach 10 jobs");
        Allure.parameter("bug44_incentiveEnglish", String.valueOf(incentiveEn));
        if (incentiveEn) {
            // Fail soft? User asked to generate bug — already on sheet. Assert chrome OK.
            Allure.parameter("knownBug", "#44");
        }
        assertThat(hasText("Earning & Incentive")).as("EN title gone").isFalse();
    }

    @Test(priority = 6, description = "LS-E6: Hindi → Fleet reflects")
    @Severity(SeverityLevel.CRITICAL)
    public void hindiFleetReflects() {
        applyHindi();
        closeDrawerLocalized();
        sleepQuiet(700);
        tapDesc("Fleet");
        tapText("फ्लीट");
        sleepQuiet(1100);
        assertThat(hasText("क्षमता") || hasText("ऑपरेटर") || hasText("सक्रिय")
                || hasDevanagariOnScreen()).isTrue();
    }

    @Test(priority = 7, description = "LS-E7: Hindi → Account reflects")
    @Severity(SeverityLevel.CRITICAL)
    public void hindiAccountReflects() {
        applyHindi();
        if (!hasText("खाता")) {
            closeDrawerLocalized();
            new HomePage().tapDesc("Profile");
            sleepQuiet(900);
        }
        tapText("खाता");
        sleepQuiet(1100);
        assertThat(hasText("प्रोफ़ाइल जानकारी") || hasText("पूरा नाम")).isTrue();
        assertThat(hasText("Profile Info")).isFalse();
    }

    @Test(priority = 8, description = "LS-E8: Hindi → Help reflects")
    @Severity(SeverityLevel.CRITICAL)
    public void hindiHelpReflects() {
        applyHindi();
        tapText("सहायता");
        sleepQuiet(1100);
        assertThat(hasText("लाइव चैट") || hasText("सहायता और समर्थन")).isTrue();
        assertThat(hasText("Live chat")).isFalse();
    }

    @Test(priority = 9, description = "LS-E9: Hindi → Refer partial (#41)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #41: how-to steps stay English — assert header Hindi + document steps.")
    public void hindiReferPartialBug41() {
        applyHindi();
        tapText("रेफर करें और कमाएं");
        sleepQuiet(1100);
        assertThat(hasText("रेफर") || hasDevanagariOnScreen()).as("Refer Hindi chrome").isTrue();
        boolean stepsEn = hasText("Share your code") || hasText("They sign up");
        Allure.parameter("bug41_stepsEnglish", String.valueOf(stepsEn));
        if (stepsEn) {
            Allure.parameter("knownBug", "#41");
        }
    }

    @Test(priority = 10, description = "LS-E10: Hindi → FAQ mixed (#42)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #42: some FAQ rows stay English.")
    public void hindiFaqMixedBug42() {
        applyHindi();
        tapText("अक्सर पूछे जाने वाले प्रश्न");
        sleepQuiet(1100);
        assertThat(hasText("सामान्य प्रश्न") || hasDevanagariOnScreen()).isTrue();
        boolean mixed = hasText("How do I start a booked job")
                || hasText("What happens if a customer cancels");
        Allure.parameter("bug42_mixedEnglish", String.valueOf(mixed));
        if (mixed) {
            Allure.parameter("knownBug", "#42");
        }
    }

    @Test(priority = 11, description = "LS-E11: Hindi → Terms still EN (#43)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #43: Terms body remains English.")
    public void hindiTermsStillEnglishBug43() {
        applyHindi();
        tapText("नियम और शर्तें");
        sleepQuiet(1100);
        boolean stillEn = hasText("Accepting these terms") || hasText("Your obligations");
        Allure.parameter("bug43_termsEnglish", String.valueOf(stillEn));
        assertThat(hasDevanagariOnScreen() || stillEn || hasText("Terms"))
                .as("Terms screen opened").isTrue();
        if (stillEn) {
            Allure.parameter("knownBug", "#43");
        }
    }

    @Test(priority = 12, description = "LS-E12: Hindi → Policies partial (#46)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #46: Policies mixed Hindi/English.")
    public void hindiPoliciesPartialBug46() {
        applyHindi();
        tapText("नीतियां");
        sleepQuiet(1100);
        assertThat(hasText("नीति") || hasDevanagariOnScreen()).as("Policies opened").isTrue();
        boolean mixed = hasText("How we use it") || hasText("Your rights") || hasText("Location");
        Allure.parameter("bug46_policiesMixed", String.valueOf(mixed));
        if (mixed) {
            Allure.parameter("knownBug", "#46");
        }
    }

    @Test(priority = 13, description = "LS-E13: Hindi → Settings reflects")
    @Severity(SeverityLevel.CRITICAL)
    public void hindiSettingsReflects() {
        applyHindi();
        tapText("सेटिंग्स");
        sleepQuiet(1100);
        assertThat(hasText("सेटिंग्स") || hasText("अनुमतियाँ") || hasText("सूचना")).isTrue();
        assertThat(hasText("Permissions") && hasText("Camera Access")).isFalse();
    }

    @Test(priority = 14, description = "LS-E14: drawer Log Out stays EN (#40)")
    @Severity(SeverityLevel.NORMAL)
    @Description("BUGS_FOUND #40: Log Out / Company Id remain English.")
    public void drawerLogOutStaysEnglishBug40() {
        applyHindi();
        assertThat(hasText("खाता")).isTrue();
        boolean logOutEn = hasText("Log Out");
        Allure.parameter("bug40_logOutEnglish", String.valueOf(logOutEn));
        Allure.parameter("companyIdEnglish", String.valueOf(hasText("Company Id")));
        if (logOutEn) {
            Allure.parameter("knownBug", "#40");
        }
    }

    @Test(priority = 15, description = "LS-E15: package Vendor through Hindi/English cycle")
    @Severity(SeverityLevel.BLOCKER)
    public void packageThroughCycle() {
        applyHindi();
        restoreEnglishLanguage();
        assertThat(vendorPackage()).contains("l2b");
        assertThat(launcherNow()).isFalse();
    }

    @Test(priority = 16, description = "LS-E16: no Accept/Decline on Language path")
    @Severity(SeverityLevel.CRITICAL)
    public void noAcceptDecline() {
        LanguageSettingsPage page = reachLanguage();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.visibleTexts().stream().noneMatch(t -> t.equals("Accept"))).isTrue();
        softly.assertThat(page.visibleTexts().stream().noneMatch(t -> t.equals("Decline"))).isTrue();
        softly.assertAll();
    }
}
