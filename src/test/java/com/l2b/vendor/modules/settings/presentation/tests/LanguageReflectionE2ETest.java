package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Adb;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.bookings.presentation.pages.QuickBookingPage;
import com.l2b.vendor.modules.help.data.api.HelpLegalApi;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.onboarding.data.api.AuthApi;
import com.l2b.vendor.modules.settings.data.api.AccountApi;
import com.l2b.vendor.modules.settings.presentation.pages.LanguageSettingsPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * True E2E language reflection: one Appium session, one login, Hindi once,
 * then every reachable module. Does not logout / clear data / reinstall
 * between modules. Existing LS-L/I/E 38/38 suite is unchanged.
 *
 * <p>Known product defects #40–#46 are re-tested as independent validations
 * and stay FAIL when English remains. They are not folded into each other.
 */
@Epic("Vendor app")
@Feature("Language reflection E2E — one session — rental 9000000001")
public class LanguageReflectionE2ETest extends LanguageSettingsBaseTest {

    private static final String LANG = "hi";
    private static final String PHONE = "9000000001";

    private final AuthApi auth = new AuthApi();
    private final AccountApi accountApi = new AccountApi();
    private final HelpLegalApi helpLegalApi = new HelpLegalApi();
    private final Map<String, String> moduleStatus = new LinkedHashMap<>();
    private final List<String> knownBugs = new ArrayList<>();
    private final List<String> newBugs = new ArrayList<>();
    private final SoftAssertions knownBugAsserts = new SoftAssertions();

    private AppiumDriver journeyDriver;
    private int appiumSessions = 0;
    private String backendLanguage = "UNTESTED";
    private String persistenceResult = "UNTESTED";
    private String crashResult = "UNTESTED";
    private String logcatAfterSave = "";

    @Override
    protected boolean noReset() {
        return false;
    }

    @Override
    protected boolean newSessionPerMethod() {
        return false;
    }

    @Override
    protected void beforeCreateDriver(Method method) {
        Adb.ensureNetworkReady();
        if (!DriverManager.hasDriver()) {
            Adb.forceStop(Config.get("app.package"));
        }
    }

    @Override
    @BeforeMethod(alwaysRun = true)
    public void ensureEnglishBeforeMethod() {
        // Keep Hindi for the whole journey. Do not restore between steps.
    }

    @AfterClass(alwaysRun = true)
    public void restoreEnglishAfterJourney() {
        try {
            if (DriverManager.hasDriver()) {
                restoreEnglishLanguage();
            }
        } catch (RuntimeException e) {
            Allure.parameter("e2eRestoreEnglish", e.getClass().getSimpleName());
        }
    }

    @Test(description = "LR-E2E: one login → Hindi → all modules → persist → relaunch")
    @Severity(SeverityLevel.BLOCKER)
    @Description("One authenticated Appium session. Never logout between modules. "
            + "Re-tests BUGS_FOUND #40–#46 as separate validations.")
    public void hindiReflectionOneSessionJourney() {
        Allure.parameter("account", PHONE);
        Allure.parameter("persona", "rental company vendor");
        Allure.parameter("language", "Hindi (hi)");

        loginOnce();
        try {
            selectHindiAndSave();
            verifyLanguageSelected();
            verifyBackendAfterSave();

            verifyHome();
            verifyCalendar();
            verifyEarning();
            verifyFleet();
            verifyDrawerKnownBug40();
            verifyAccount();
            verifyKyc();
            verifyTeam();
            verifyHelp();
            verifyRefer();
            verifyFaq();
            verifyTerms();
            verifyPolicies();
            verifySettings();
            verifyNotifications();

            verifyBackgroundForeground();
            verifyRelaunchWithoutClear();
            verifyStability();
        } finally {
            attachJourneySummary();
        }

        knownBugAsserts.assertAll();
    }

    @Step("1. Login once (rental 9000000001)")
    private void loginOnce() {
        assertThat(rentalCompanyPhone()).isEqualTo(PHONE);
        reachUsableRentalHomeOrFailExtendTime();
        journeyDriver = DriverManager.get();
        appiumSessions = 1;
        Adb.run("logcat", "-c");
        recordModule("Login", "PASS", "Quick Booking closed; Home visible; one session");
        assertSessionAlive("login");
        attachShot("e2e-01-login-home");
    }

    @Step("2. Profile Drawer → Language → Hindi → Save")
    private void selectHindiAndSave() {
        // Stay on the authenticated session — never call reachLanguage()/reachUsableRentalHome
        // (those re-run first-launch OTP and break the one-session contract).
        openDrawer();
        if (hasText("Language")) {
            new ProfileDrawerPage().tapRow("Language");
        } else if (!tapLabel("Language") && !tapLabel("भाषा")) {
            throw new IllegalStateException("Language row not on Profile drawer");
        }
        sleepQuiet(1100);
        LanguageSettingsPage page = new LanguageSettingsPage();
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).as("Language settings opened").isTrue();
        page.selectHindi();
        sleepQuiet(500);
        page.tapSave();
        sleepQuiet(1500);
        try {
            logcatAfterSave = Adb.runAndRead("logcat", "-d", "-t", "500");
        } catch (RuntimeException e) {
            logcatAfterSave = "";
        }
        Allure.addAttachment("logcat-after-hindi-save", "text/plain",
                logcatAfterSave.isBlank() ? "(empty)" : trimLog(logcatAfterSave));
        boolean posted = containsAny(logcatAfterSave, "/api/v1/account/language",
                "account/language", "\"language\":\"hi\"", "\"language\": \"hi\"");
        Allure.parameter("appPostedLanguageApi", String.valueOf(posted));
        recordModule("Select Hindi", posted || drawerHindiNow() ? "PASS" : "FAIL",
                "Save tapped; logcat language POST=" + posted);
        assertSessionAlive("select-hindi");
        attachShot("e2e-02-after-hindi-save");
    }

    @Step("3. Verify language persistence on drawer")
    private void verifyLanguageSelected() {
        ensureHindiDrawer();
        boolean hindiRows = hasText("खाता") && (hasText("भाषा") || hasText("सहायता"));
        boolean stillEnRows = hasText("Account") && hasText("Language");
        Allure.parameter("drawerHindi", String.valueOf(hindiRows));
        Allure.parameter("drawerEnglishRows", String.valueOf(stillEnRows));
        assertThat(hindiRows).as("Drawer rows switched to Hindi").isTrue();
        recordModule("Language persistence", hindiRows ? "PASS" : "FAIL",
                "खाता/भाषा present; English Account+Language=" + stillEnRows);
        attachShot("e2e-03-drawer-hindi");
    }

    @Step("20. Backend/API verification (OpenAPI language + help-legal)")
    private void verifyBackendAfterSave() {
        String token = vendorToken();
        if (token.isBlank()) {
            backendLanguage = "BLOCKED";
            recordModule("Backend/API", "BLOCKED", "verify-otp did not return access_token");
            return;
        }
        Response profile = accountApi.profile(token);
        Allure.addAttachment("GET /api/v1/account/profile", "application/json",
                safeBody(profile), ".json");
        String profileLang = firstNonBlank(
                profile.jsonPath().getString("data.language"),
                profile.jsonPath().getString("data.preferred_language"),
                profile.jsonPath().getString("language"),
                profile.jsonPath().getString("preferred_language"));
        Allure.parameter("profileLanguageField", profileLang.isBlank() ? "(absent)" : profileLang);

        Response faqsStored = helpLegalApi.faqs(token, null);
        Response faqsHi = helpLegalApi.faqs(token, LANG);
        Response termsHi = helpLegalApi.document(token, "terms", LANG);
        Response policiesHi = helpLegalApi.document(token, "policies", LANG);
        Response referHi = helpLegalApi.referralsMe(token, LANG);
        attachJson("GET /help-legal/faqs (stored pref)", faqsStored);
        attachJson("GET /help-legal/faqs?locale=hi", faqsHi);
        attachJson("GET /help-legal/documents/terms?locale=hi", termsHi);
        attachJson("GET /help-legal/documents/policies?locale=hi", policiesHi);
        attachJson("GET /help-legal/referrals/me?locale=hi", referHi);

        String faqsLocale = firstNonBlank(faqsStored.jsonPath().getString("locale"),
                faqsStored.jsonPath().getString("data.locale"));
        String faqsHiLocale = firstNonBlank(faqsHi.jsonPath().getString("locale"),
                faqsHi.jsonPath().getString("data.locale"));
        Allure.parameter("faqsStoredLocale", faqsLocale);
        Allure.parameter("faqsHiLocale", faqsHiLocale);

        boolean storedLooksHi = "hi".equalsIgnoreCase(faqsLocale) || profileLang.equalsIgnoreCase("hi");
        boolean posted = containsAny(logcatAfterSave, "/api/v1/account/language", "account/language");
        if (storedLooksHi) {
            backendLanguage = "PASS — backend Hindi (faqs locale or profile)";
        } else if (posted && !storedLooksHi) {
            backendLanguage = "FAIL — app POSTed language but read-back is not hi";
        } else if (!posted && profileLang.isBlank()) {
            backendLanguage = "UNTESTED — profile has no language field; logcat had no POST";
        } else {
            backendLanguage = "FAIL — stored preference is not Hindi";
        }
        recordModule("Backend/API", backendLanguage.startsWith("PASS") ? "PASS"
                : backendLanguage.startsWith("FAIL") ? "FAIL" : backendLanguage.split(" ")[0],
                backendLanguage);

        classifyAdminContent("FAQ", faqsHi, List.of("How do I start a booked job",
                "What happens if a customer cancels"));
        classifyAdminContent("Terms", termsHi, List.of("Accepting these terms", "Your obligations"));
        classifyAdminContent("Policies", policiesHi, List.of("How we use it", "Your rights"));
        classifyAdminContent("Refer", referHi, List.of("Share your code", "They sign up"));
        assertSessionAlive("backend");
    }

    @Step("4. Home")
    private void verifyHome() {
        goHome();
        boolean chrome = hasText("वर्तमान कमाई") || hasText("शुभ") || hasText("आगामी बुकिंग")
                || hasText("नमस्ते") || hasDevanagariOnScreen();
        boolean enTitle = hasText("Current Earning");
        Allure.parameter("homeHindiChrome", String.valueOf(chrome));
        Allure.parameter("homeEnglishEarning", String.valueOf(enTitle));
        assertThat(chrome).as("Home Hindi chrome").isTrue();
        assertThat(enTitle).as("Home Current Earning should be gone").isFalse();
        recordModule("Home", chrome && !enTitle ? "PASS" : "FAIL",
                "Hindi chrome=" + chrome + " EN Current Earning=" + enTitle);
        assertNoCrash("home");
        attachShot("e2e-04-home");
    }

    @Step("5. Calendar")
    private void verifyCalendar() {
        goHome();
        tapTab("Calendar", "कैलेंडर", 0);
        sleepQuiet(1100);
        boolean opened = hasText("अनुसूची") || hasText("शेड्यूल") || hasDevanagariOnScreen()
                || hasText("October") || hasText("Schedule");
        boolean titleHi = hasText("अनुसूची");
        boolean monthEn = hasText("October");
        Allure.parameter("calendarOpened", String.valueOf(opened));
        Allure.parameter("scheduleHindi", String.valueOf(titleHi));
        Allure.parameter("monthOctoberEnglish", String.valueOf(monthEn));
        assertThat(opened).as("Calendar opened").isTrue();
        if (monthEn) {
            failKnown("#45", "Calendar", "Month label remains English October");
        } else {
            Allure.parameter("bug45", "NOT REPRODUCED");
        }
        recordModule("Calendar", titleHi || hasDevanagariOnScreen() ? "PASS" : "FAIL",
                "अनुसूची=" + titleHi + " October EN=" + monthEn + " (#45 separate)");
        assertNoCrash("calendar");
        attachShot("e2e-05-calendar");
        goHome();
    }

    @Step("6. Earning")
    private void verifyEarning() {
        goHome();
        tapTab("Earning", "कमाई", 2);
        sleepQuiet(1100);
        boolean chrome = hasText("कमाई और प्रोत्साहन") || hasText("वॉलेट बैलेंस") || hasText("निकालें");
        boolean enTitle = hasText("Earning & Incentive");
        boolean incentiveEn = hasText("Once they reach 10 jobs");
        Allure.parameter("earningHindiChrome", String.valueOf(chrome));
        Allure.parameter("incentiveEnglish", String.valueOf(incentiveEn));
        assertThat(chrome).as("Earning Hindi chrome").isTrue();
        assertThat(enTitle).as("EN Earning title gone").isFalse();
        if (incentiveEn) {
            failKnown("#44", "Earning", "Incentive description remains English");
        } else {
            Allure.parameter("bug44", "NOT REPRODUCED");
        }
        recordModule("Earning", chrome && !enTitle ? "PASS" : "FAIL",
                "chrome=" + chrome + " incentive EN=" + incentiveEn + " (#44 separate)");
        assertNoCrash("earning");
        attachShot("e2e-06-earning");
        goHome();
    }

    @Step("7. Fleet")
    private void verifyFleet() {
        goHome();
        tapTab("Fleet", "फ्लीट", 3);
        sleepQuiet(1100);
        boolean opened = hasText("क्षमता") || hasText("ऑपरेटर") || hasText("सक्रिय")
                || hasText("मशीन") || hasDevanagariOnScreen()
                || hasText("Your Fleet") || hasText("Add Machine");
        boolean stillEnOnly = (hasText("Your Fleet") || hasText("Add Machine"))
                && !hasDevanagariOnScreen();
        Allure.parameter("fleetOpened", String.valueOf(opened));
        assertThat(opened).as("Fleet opened").isTrue();
        recordModule("Fleet", stillEnOnly ? "FAIL" : "PASS",
                "opened=" + opened + " English-only=" + stillEnOnly);
        assertNoCrash("fleet");
        attachShot("e2e-07-fleet");
        goHome();
    }

    @Step("Drawer #40 — Log Out + Company Id")
    private void verifyDrawerKnownBug40() {
        ensureHindiDrawer();
        boolean logOutEn = hasText("Log Out");
        boolean companyEn = hasText("Company Id") || src().contains("Company Id");
        Allure.parameter("bug40_logOutEnglish", String.valueOf(logOutEn));
        Allure.parameter("bug40_companyIdEnglish", String.valueOf(companyEn));
        if (logOutEn || companyEn) {
            failKnown("#40", "Drawer", "Log Out EN=" + logOutEn + " Company Id EN=" + companyEn);
        } else {
            Allure.parameter("bug40", "NOT REPRODUCED");
        }
        recordModule("Drawer #40", "PASS", "Drawer Hindi rows ok; #40 validated separately");
        attachShot("e2e-drawer-bug40");
    }

    @Step("8. Account")
    private void verifyAccount() {
        openDrawerRow("खाता", "Account");
        boolean chrome = hasText("प्रोफ़ाइल जानकारी") || hasText("पूरा नाम");
        boolean en = hasText("Profile Info");
        assertThat(chrome || hasDevanagariOnScreen()).as("Account opened").isTrue();
        assertThat(en).as("Profile Info English gone").isFalse();
        recordModule("Account", chrome && !en ? "PASS" : chrome ? "PASS" : "FAIL",
                "Hindi chrome=" + chrome + " Profile Info EN=" + en);
        assertNoCrash("account");
        attachShot("e2e-08-account");
        backToDrawerOrHome();
    }

    @Step("9. KYC")
    private void verifyKyc() {
        openDrawerRow("केवाईसी", "KYC");
        boolean opened = hasText("केवाईसी") || hasText("KYC") || hasDevanagariOnScreen()
                || hasText("Aadhaar") || hasText("आधार");
        assertThat(opened).as("KYC opened").isTrue();
        boolean enOnly = hasText("KYC Details") && !hasDevanagariOnScreen();
        recordModule("KYC", enOnly ? "FAIL" : "PASS", "opened=" + opened + " English-only=" + enOnly);
        assertNoCrash("kyc");
        attachShot("e2e-09-kyc");
        backToDrawerOrHome();
    }

    @Step("10. Team / Manage Team")
    private void verifyTeam() {
        openDrawerRow("टीम मैनेज करें", "Manage team");
        boolean opened = hasText("टीम") || hasText("Manage team") || hasDevanagariOnScreen()
                || hasText("Add");
        assertThat(opened).as("Team opened").isTrue();
        boolean enOnly = hasText("Manage team") && !hasDevanagariOnScreen();
        recordModule("Team", enOnly ? "FAIL" : "PASS", "opened=" + opened + " English-only=" + enOnly);
        assertNoCrash("team");
        attachShot("e2e-10-team");
        backToDrawerOrHome();
    }

    @Step("11. Help & Support")
    private void verifyHelp() {
        openDrawerRow("सहायता", "Help");
        boolean chrome = hasText("लाइव चैट") || hasText("सहायता और समर्थन");
        boolean en = hasText("Live chat");
        assertThat(chrome || hasDevanagariOnScreen()).as("Help opened").isTrue();
        assertThat(en).as("Live chat English gone").isFalse();
        recordModule("Help", chrome && !en ? "PASS" : chrome ? "PASS" : "FAIL",
                "Hindi chrome=" + chrome + " Live chat EN=" + en);
        assertNoCrash("help");
        attachShot("e2e-11-help");
        backToDrawerOrHome();
    }

    @Step("12. Refer & Earn")
    private void verifyRefer() {
        openDrawerRow("रेफर करें और कमाएं", "Refer");
        boolean chrome = hasText("रेफर") || hasDevanagariOnScreen();
        boolean stepsEn = hasText("Share your code") || hasText("They sign up") || hasText("You both earn");
        assertThat(chrome).as("Refer opened with Hindi chrome").isTrue();
        Allure.parameter("bug41_stepsEnglish", String.valueOf(stepsEn));
        if (stepsEn) {
            failKnown("#41", "Refer & Earn", "How-to steps remain English");
        } else {
            Allure.parameter("bug41", "NOT REPRODUCED");
        }
        recordModule("Refer & Earn", chrome ? "PASS" : "FAIL",
                "chrome=" + chrome + " steps EN=" + stepsEn + " (#41 separate)");
        assertNoCrash("refer");
        attachShot("e2e-12-refer");
        backToDrawerOrHome();
    }

    @Step("13. FAQ")
    private void verifyFaq() {
        openDrawerRow("अक्सर पूछे जाने वाले प्रश्न", "FAQ");
        boolean opened = hasText("सामान्य प्रश्न") || hasText("अक्सर") || hasDevanagariOnScreen()
                || hasText("FAQ");
        boolean mixed = hasText("How do I start a booked job")
                || hasText("What happens if a customer cancels");
        assertThat(opened).as("FAQ opened").isTrue();
        Allure.parameter("bug42_mixedEnglish", String.valueOf(mixed));
        if (mixed) {
            failKnown("#42", "FAQ", "Mixed Hindi/English questions");
        } else {
            Allure.parameter("bug42", "NOT REPRODUCED");
        }
        recordModule("FAQ", opened ? "PASS" : "FAIL",
                "opened=" + opened + " mixed EN=" + mixed + " (#42 separate; see Backend class A/B)");
        assertNoCrash("faq");
        attachShot("e2e-13-faq");
        backToDrawerOrHome();
    }

    @Step("14. Terms & Services")
    private void verifyTerms() {
        openDrawerRow("नियम और शर्तें", "Terms");
        boolean opened = hasText("Terms") || hasText("नियम") || hasText("Accepting these terms")
                || hasDevanagariOnScreen();
        boolean stillEn = hasText("Accepting these terms") || hasText("Your obligations");
        assertThat(opened).as("Terms opened").isTrue();
        Allure.parameter("bug43_termsEnglish", String.valueOf(stillEn));
        if (stillEn) {
            failKnown("#43", "Terms & Services", "Full page remains English");
        } else {
            Allure.parameter("bug43", "NOT REPRODUCED");
        }
        recordModule("Terms & Services", opened ? "PASS" : "FAIL",
                "opened=" + opened + " body EN=" + stillEn + " (#43 separate; see Backend class A/B)");
        assertNoCrash("terms");
        attachShot("e2e-14-terms");
        backToDrawerOrHome();
    }

    @Step("15. Policies — all loaded sections")
    private void verifyPolicies() {
        openDrawerRow("नीतियां", "Policies");
        boolean opened = hasText("नीति") || hasDevanagariOnScreen() || hasText("Policies");
        assertThat(opened).as("Policies opened").isTrue();
        swipeUp();
        sleepQuiet(700);
        swipeUp();
        sleepQuiet(700);
        boolean laterEn = hasText("How we use it") || hasText("Your rights") || hasText("Location");
        Allure.parameter("bug46_laterSectionsEnglish", String.valueOf(laterEn));
        if (laterEn) {
            failKnown("#46", "Policies", "Later sections remain English after scroll");
        } else {
            Allure.parameter("bug46", "NOT REPRODUCED");
        }
        recordModule("Policies", opened ? "PASS" : "FAIL",
                "opened=" + opened + " later EN=" + laterEn + " (#46 separate; see Backend class A/B)");
        assertNoCrash("policies");
        attachShot("e2e-15-policies");
        backToDrawerOrHome();
    }

    @Step("16. Settings")
    private void verifySettings() {
        openDrawerRow("सेटिंग्स", "Settings");
        boolean chrome = hasText("सेटिंग्स") || hasText("अनुमतियाँ") || hasText("सूचना");
        boolean enOnly = hasText("Permissions") && hasText("Camera Access") && !hasDevanagariOnScreen();
        assertThat(chrome || hasDevanagariOnScreen()).as("Settings opened").isTrue();
        recordModule("Settings", enOnly ? "FAIL" : "PASS",
                "Hindi chrome=" + chrome + " EN-only=" + enOnly);
        assertNoCrash("settings");
        attachShot("e2e-16-settings");
        backToDrawerOrHome();
    }

    @Step("17. Notifications if reachable")
    private void verifyNotifications() {
        goHome();
        boolean tapped = tapDesc("Notification") || tapDesc("Notifications")
                || tapDesc("सूचना") || tapDesc("सूचनाएं") || tapDesc("नोटिफिकेशन");
        if (!tapped) {
            clickAt(900, 155);
            sleepQuiet(1000);
        } else {
            sleepQuiet(1100);
        }
        boolean opened = hasText("सूचना") || hasText("अपठित") || hasText("सभी")
                || hasText("Notification") || hasText("Unread") || hasText("All");
        if (!opened) {
            recordModule("Notifications", "BLOCKED", "Bell not reachable / screen did not open");
            attachShot("e2e-17-notifications-blocked");
            return;
        }
        boolean enOnly = hasText("Notification") && (hasText("Unread") || hasText("All"))
                && !hasDevanagariOnScreen();
        recordModule("Notifications", enOnly ? "FAIL" : "PASS",
                "opened=" + opened + " English-only=" + enOnly);
        assertNoCrash("notifications");
        attachShot("e2e-17-notifications");
        DriverManager.get().navigate().back();
        sleepQuiet(700);
        goHome();
    }

    @Step("18. Background / Foreground")
    private void verifyBackgroundForeground() {
        goHome();
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        android.runAppInBackground(Duration.ofSeconds(5));
        sleepQuiet(800);
        assertSessionAlive("background-resume");
        assertThat(sameJourneyDriver()).as("Same Appium session after background").isTrue();
        boolean hindi = looksHomeHindi() || drawerHindiNow() || hasDevanagariOnScreen();
        boolean reverted = hasText("Current Earning") && !hasDevanagariOnScreen();
        Allure.parameter("hindiAfterBackground", String.valueOf(hindi));
        Allure.parameter("revertedEnglishAfterBackground", String.valueOf(reverted));
        assertThat(reverted).as("Must not revert to English after background").isFalse();
        tapTab("Earning", "कमाई", 2);
        sleepQuiet(1000);
        boolean earningHi = hasText("कमाई") || hasText("वॉलेट बैलेंस") || hasDevanagariOnScreen();
        persistenceResult = hindi && earningHi && !reverted ? "PASS" : "FAIL";
        recordModule("Background/Foreground", persistenceResult,
                "Hindi after resume=" + hindi + " Earning still HI=" + earningHi);
        attachShot("e2e-18-after-background");
        goHome();
    }

    @Step("19. Relaunch without clearing app data")
    private void verifyRelaunchWithoutClear() {
        AndroidDriver android = (AndroidDriver) DriverManager.get();
        String pkg = Config.get("app.package");
        android.terminateApp(pkg);
        android.activateApp(pkg);
        sleepQuiet(2500);
        assertThat(sameJourneyDriver()).as("Relaunch must reuse the same Appium session").isTrue();
        assertThat(appiumSessions).as("Must not create a second Appium session").isEqualTo(1);

        dismissQuickBookingIfPresent();
        String named = classifyAfterRelaunch();
        Allure.parameter("relaunchLanding", named);
        boolean lostAuth = "otp".equals(named) || "language".equals(named) || "signup".equals(named);
        boolean hindi = looksHomeHindi() || drawerHindiNow() || hasText("वर्तमान कमाई")
                || hasText("कमाई") || hasDevanagariOnScreen();
        boolean reverted = hasText("Current Earning") && !hasDevanagariOnScreen();
        if (lostAuth) {
            recordModule("Relaunch", "FAIL", "Landed on " + named + " — session/token lost");
            attachShot("e2e-19-relaunch-auth-lost");
            assertThat(lostAuth).as("Relaunch must stay authenticated").isFalse();
            return;
        }
        assertThat(reverted).as("Must not revert to English after relaunch").isFalse();
        persistenceResult = hindi && !reverted ? "PASS" : "FAIL";
        recordModule("Relaunch", persistenceResult,
                "landing=" + named + " Hindi=" + hindi + " EN revert=" + reverted);
        attachShot("e2e-19-relaunch");
        goHome();
        assertSessionAlive("relaunch");
    }

    @Step("Crash / ANR / session check")
    private void verifyStability() {
        String logcat;
        try {
            logcat = Adb.runAndRead("logcat", "-d", "-t", "300");
        } catch (RuntimeException e) {
            logcat = "";
        }
        boolean fatal = containsAny(logcat, "FATAL EXCEPTION", "AndroidRuntime: FATAL");
        boolean anr = containsAny(logcat, "ANR in " + Config.get("app.package"), "ActivityManager: ANR");
        boolean launcher = launcherNow();
        Allure.parameter("fatalException", String.valueOf(fatal));
        Allure.parameter("anr", String.valueOf(anr));
        Allure.parameter("onLauncher", String.valueOf(launcher));
        if (fatal || anr) {
            Allure.addAttachment("stability-logcat", "text/plain", trimLog(logcat));
            crashResult = "FAIL";
        } else if (launcher) {
            crashResult = "FAIL";
        } else {
            crashResult = "PASS";
        }
        recordModule("Stability", crashResult,
                "FATAL=" + fatal + " ANR=" + anr + " launcher=" + launcher);
        assertThat(fatal).as("No FATAL EXCEPTION").isFalse();
        assertThat(anr).as("No ANR (log evidence)").isFalse();
        assertThat(launcher).as("Still in Vendor, not launcher").isFalse();
        assertThat(vendorPackage()).contains("l2b");
    }

    private void classifyAdminContent(String name, Response response, List<String> englishMarkers) {
        if (response == null) {
            Allure.parameter(name + "ApiClass", "UNTESTED");
            return;
        }
        String body = safeBody(response);
        boolean hasEn = false;
        for (String marker : englishMarkers) {
            if (body.contains(marker)) {
                hasEn = true;
                break;
            }
        }
        boolean hasHi = false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c >= 0x0900 && c <= 0x097F) {
                hasHi = true;
                break;
            }
        }
        String cls;
        if (hasEn && !hasHi) {
            cls = "B — localized content missing from backend/admin source";
        } else if (hasEn && hasHi) {
            cls = "B — mixed translations in admin source (not app switch failure alone)";
        } else if (hasHi) {
            cls = "content Hindi on locale=hi (if UI stays English → A app apply defect)";
        } else {
            cls = "UNTESTED — empty/unknown body";
        }
        Allure.parameter(name + "ApiClass", cls);
    }

    private void failKnown(String id, String module, String detail) {
        String line = id + " " + module + " — " + detail + " — FAIL (still reproduced)";
        knownBugs.add(line);
        Allure.parameter("knownBug", id);
        knownBugAsserts.assertThat(false)
                .as(id + " " + module + ": " + detail)
                .isTrue();
    }

    private void recordModule(String name, String status, String note) {
        moduleStatus.put(name, status + " | " + note);
        Allure.parameter("module." + name, status);
    }

    private void attachJourneySummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Test class: LanguageReflectionE2ETest\n");
        sb.append("Account/persona: ").append(PHONE).append(" rental company vendor\n");
        sb.append("Session count: ").append(appiumSessions).append('\n');
        sb.append("Language selected: Hindi (hi)\n");
        sb.append("Backend language: ").append(backendLanguage).append('\n');
        sb.append("Persistence: ").append(persistenceResult).append('\n');
        sb.append("Crash/ANR: ").append(crashResult).append('\n');
        sb.append("\nModules:\n");
        int pass = 0;
        int fail = 0;
        int blocked = 0;
        int untested = 0;
        for (Map.Entry<String, String> e : moduleStatus.entrySet()) {
            sb.append("  ").append(e.getKey()).append(" = ").append(e.getValue()).append('\n');
            if (e.getValue().startsWith("PASS")) {
                pass++;
            } else if (e.getValue().startsWith("FAIL")) {
                fail++;
            } else if (e.getValue().startsWith("BLOCKED")) {
                blocked++;
            } else {
                untested++;
            }
        }
        sb.append("\nPASS=").append(pass).append(" FAIL=").append(fail)
                .append(" BLOCKED=").append(blocked).append(" UNTESTED=").append(untested).append('\n');
        sb.append("\nExisting bugs re-tested:\n");
        if (knownBugs.isEmpty()) {
            sb.append("  #40–#46 NOT REPRODUCED this run\n");
        } else {
            for (String b : knownBugs) {
                sb.append("  ").append(b).append('\n');
            }
        }
        sb.append("\nNew bugs:\n");
        if (newBugs.isEmpty()) {
            sb.append("  none\n");
        } else {
            for (String b : newBugs) {
                sb.append("  ").append(b).append('\n');
            }
        }
        Allure.addAttachment("language-reflection-e2e-summary", "text/plain", sb.toString());
        Allure.parameter("sessionCount", String.valueOf(appiumSessions));
        Allure.parameter("modulesPass", String.valueOf(pass));
        Allure.parameter("modulesFail", String.valueOf(fail));
        Allure.parameter("modulesBlocked", String.valueOf(blocked));
    }

    private void ensureHindiDrawer() {
        for (int i = 0; i < 5; i++) {
            if (drawerHindiNow()) {
                return;
            }
            closeDrawerLocalized();
            sleepQuiet(400);
            openDrawer();
            sleepQuiet(800);
            if (drawerHindiNow()) {
                return;
            }
        }
        assertThat(drawerHindiNow()).as("Hindi drawer reachable without re-login").isTrue();
    }

    private void openDrawerRow(String hi, String en) {
        ensureHindiDrawer();
        if (!tapLabel(hi) && !tapLabel(en)) {
            throw new IllegalStateException("Drawer row not tappable: " + hi + " / " + en);
        }
        sleepQuiet(1100);
    }

    private void openDrawer() {
        closeDrawerLocalized();
        sleepQuiet(300);
        try {
            // Same path as ProfileDrawerBaseTest — clickable outer wrapping content-desc.
            new HomePage().tapDesc("Profile");
        } catch (RuntimeException e) {
            if (!tapDesc("Profile") && !tapDesc("प्रोफाइल") && !tapDesc("प्रोफ़ाइल")) {
                clickAt(80, 155);
            }
        }
        sleepQuiet(1100);
        // Retry once if drawer chrome not visible yet
        if (!hasText("Account") && !hasText("खाता") && !hasText("Language") && !hasText("भाषा")) {
            try {
                new HomePage().tapDesc("Profile");
            } catch (RuntimeException ignored) {
                clickAt(80, 155);
            }
            sleepQuiet(1100);
        }
    }

    private void goHome() {
        closeDrawerLocalized();
        sleepQuiet(400);
        for (int i = 0; i < 4; i++) {
            if (looksHomeHindi() || new HomePage().isDisplayedNow()) {
                return;
            }
            if (hasText("Upcoming Booking") || hasText("सहायता और समर्थन")
                    || hasText("Profile Info") || hasText("प्रोफ़ाइल")) {
                DriverManager.get().navigate().back();
                sleepQuiet(700);
                continue;
            }
            tapTab("Home", "होम", 1);
            sleepQuiet(800);
        }
    }

    private void backToDrawerOrHome() {
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        if (!drawerHindiNow() && !looksHomeHindi() && !new HomePage().isDisplayedNow()) {
            goHome();
        }
    }

    private void tapTab(String en, String hi, int index) {
        if (tapDesc(en) || tapDesc(hi) || tapLabel(en) || tapLabel(hi)) {
            return;
        }
        int[] xs = {135, 405, 675, 945};
        clickAt(xs[Math.min(index, 3)], 2280);
    }

    private boolean tapLabel(String text) {
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

    private boolean drawerHindiNow() {
        return hasText("खाता") || hasText("भाषा") || hasText("सहायता");
    }

    private boolean looksHomeHindi() {
        return hasText("वर्तमान कमाई") || hasText("शुभ") || hasText("आगामी बुकिंग")
                || hasText("नमस्ते") || (hasText("कैलेंडर") && hasText("फ्लीट"));
    }

    private String classifyAfterRelaunch() {
        if (launcherNow()) {
            return "launcher";
        }
        if (new QuickBookingPage().isDisplayedNow()) {
            return "quick-booking";
        }
        if (looksHomeHindi() || new HomePage().isDisplayedNow()) {
            return "home";
        }
        if (hasText("OTP") || hasText("Get OTP") || hasText("ओटीपी")) {
            return "otp";
        }
        if (hasText("Choose your language") || hasText("Get started")) {
            return "language";
        }
        return namedLanding(new QuickBookingPage(), new HomePage());
    }

    private void dismissQuickBookingIfPresent() {
        QuickBookingPage qb = new QuickBookingPage();
        if (qb.isDisplayedNow()) {
            try {
                qb.tapClose();
                sleepQuiet(900);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void assertSessionAlive(String step) {
        assertThat(DriverManager.hasDriver()).as(step + ": driver present").isTrue();
        assertThat(sameJourneyDriver()).as(step + ": same Appium session").isTrue();
        assertThat(vendorPackage()).as(step + ": still Vendor").contains("l2b");
        try {
            DriverManager.get().getPageSource();
        } catch (RuntimeException e) {
            throw new AssertionError(step + ": session dead — " + e.getMessage(), e);
        }
    }

    private boolean sameJourneyDriver() {
        return journeyDriver != null && journeyDriver == DriverManager.get();
    }

    private void assertNoCrash(String step) {
        assertThat(launcherNow()).as(step + ": not on launcher").isFalse();
        assertThat(vendorPackage()).contains("l2b");
        String srcNow = src();
        assertThat(srcNow == null || srcNow.length() < 80)
                .as(step + ": blank/white hierarchy")
                .isFalse();
    }

    private void swipeUp() {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence swipe =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        swipe.addAction(finger.createPointerMove(Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 1700));
        swipe.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        swipe.addAction(finger.createPointerMove(Duration.ofMillis(400),
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 540, 700));
        swipe.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        DriverManager.get().perform(List.of(swipe));
    }

    private void clickAt(int x, int y) {
        DriverManager.get().executeScript("mobile: clickGesture", Map.of("x", x, "y", y));
    }

    private void clickGesture(WebElement el) {
        var r = el.getRect();
        clickAt(r.x + Math.max(1, r.width / 2), r.y + Math.max(1, r.height / 2));
    }

    private void attachShot(String name) {
        try {
            byte[] png = ((TakesScreenshot) DriverManager.get()).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment(name, "image/png", new java.io.ByteArrayInputStream(png), "png");
        } catch (RuntimeException ignored) {
        }
    }

    private void attachJson(String name, Response response) {
        Allure.addAttachment(name, "application/json", safeBody(response), ".json");
        Allure.parameter(name + ".status", String.valueOf(response.statusCode()));
    }

    private String vendorToken() {
        String phone = Config.get("user.rental.company.phone");
        auth.sendOtp(phone);
        Response verify = auth.verifyOtp(phone, "1234");
        Allure.addAttachment("POST /api/v1/auth/verify-otp", "application/json",
                safeBody(verify), ".json");
        if (verify.statusCode() != 200) {
            return "";
        }
        String token = verify.jsonPath().getString("access_token");
        return token == null ? "" : token;
    }

    private String src() {
        try {
            return DriverManager.get().getPageSource();
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String safeBody(Response response) {
        try {
            return response.asPrettyString();
        } catch (RuntimeException e) {
            return "(unreadable " + e.getClass().getSimpleName() + ")";
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String v : values) {
            if (v != null && !v.isBlank() && !"null".equalsIgnoreCase(v)) {
                return v.trim();
            }
        }
        return "";
    }

    private static boolean containsAny(String hay, String... needles) {
        if (hay == null || hay.isBlank()) {
            return false;
        }
        String lower = hay.toLowerCase();
        for (String n : needles) {
            if (n != null && lower.contains(n.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static String trimLog(String log) {
        if (log.length() <= 8000) {
            return log;
        }
        return log.substring(log.length() - 8000);
    }

    private static String lit(String v) {
        if (!v.contains("'")) {
            return "'" + v + "'";
        }
        return "\"" + v + "\"";
    }
}
