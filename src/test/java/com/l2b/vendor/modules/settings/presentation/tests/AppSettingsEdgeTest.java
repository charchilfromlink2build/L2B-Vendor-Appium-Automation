package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.AppSettingsPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * App Settings edge — Never Accept / Decline / Log Out Confirm.
 *
 * <p><b>Critical Edge (AS-E1–E10)</b>
 */
@Epic("Vendor app")
@Feature("App Settings edge — rental 9000000001")
public class AppSettingsEdgeTest extends AppSettingsBaseTest {

    @Test(priority = 1, description = "AS-E1: rapid Retry/toggle stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidAction() {
        AppSettingsPage page = reachAppSettings();
        for (int i = 0; i < 4; i++) {
            if (page.isRetryVisible() && page.isErrorVisible()) {
                page.tapRetry();
            } else if (page.switchCount() > 0) {
                page.tapFirstSwitch();
            }
            sleepQuiet(350);
        }
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("app-settings");
    }

    @Test(priority = 2, description = "AS-E2: layout Back left")
    @Severity(SeverityLevel.CRITICAL)
    public void layoutBack() {
        AppSettingsPage page = reachAppSettings();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.headerBackAligned()).as("Back left").isTrue();
        softly.assertThat(page.isSettingsTitleVisible()).as("title").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "AS-E3: error persists or recovers after Retry")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #51 — document Retry outcome.")
    public void errorAfterRetry() {
        AppSettingsPage page = reachAppSettings();
        if (!page.isErrorVisible()) {
            Allure.parameter("state", "full-chrome");
            assertThat(page.isFullChromeVisible()).isTrue();
            return;
        }
        page.tapRetry();
        sleepQuiet(2500);
        page.attachScreenshot("as-e3-after-retry");
        Allure.parameter("stillError", String.valueOf(page.isErrorVisible()));
        Allure.parameter("recoveredFull", String.valueOf(page.isFullChromeVisible()));
        assertThat(page.isDisplayedNow()).isTrue();
        SoftAssertions softly = new SoftAssertions();
        if (page.isErrorVisible()) {
            softly.assertThat(page.isRetryVisible()).as("Retry remains").isTrue();
        } else {
            softly.assertThat(page.isFullChromeVisible()).as("recovered").isTrue();
        }
        softly.assertAll();
    }

    @Test(priority = 4, description = "AS-E4: core rows when list loaded")
    @Severity(SeverityLevel.CRITICAL)
    public void coreRows() {
        AppSettingsPage page = reachAppSettings();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error-#51");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isRowVisible(AppSettingsPage.ROW_NOTIFICATION)).as("Notification").isTrue();
        softly.assertThat(page.isRowVisible(AppSettingsPage.ROW_GPS)).as("GPS").isTrue();
        softly.assertThat(page.isRowVisible(AppSettingsPage.ROW_CAMERA)).as("Camera").isTrue();
        softly.assertThat(page.isRowVisible(AppSettingsPage.ROW_CONTACTS)).as("Contacts").isTrue();
        softly.assertAll();
    }

    @Test(priority = 5, description = "AS-E5: no Accept/Decline")
    @Severity(SeverityLevel.CRITICAL)
    public void noAcceptDecline() {
        AppSettingsPage page = reachAppSettings();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("Decline")).as("no Decline").isFalse();
        softly.assertThat(DriverManager.get().findElements(
                org.openqa.selenium.By.xpath("//*[@text='Accept']")).isEmpty())
                .as("no Accept").isTrue();
        softly.assertAll();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 6, description = "AS-E6: reopen after Home")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterHome() {
        AppSettingsPage page = reachAppSettings();
        page.tapBack();
        sleepQuiet(700);
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        ensureRentalHomeWarm();
        page = reachAppSettings();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 7, description = "AS-E7: scroll extremes stay settings")
    @Severity(SeverityLevel.NORMAL)
    public void scrollExtremes() {
        AppSettingsPage page = reachAppSettings();
        page.swipeListUp();
        sleepQuiet(300);
        page.swipeListUp();
        sleepQuiet(300);
        assertThat(classifyRentalNow()).isEqualTo("app-settings");
        page.swipeListDown();
        sleepQuiet(400);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 8, description = "AS-E8: double Back safe")
    @Severity(SeverityLevel.NORMAL)
    public void doubleBack() {
        AppSettingsPage page = reachAppSettings();
        page.tapBack();
        sleepQuiet(700);
        if (profileDrawerNow()) {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        Allure.parameter("after", classifyRentalNow());
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 9, description = "AS-E9: package through Retry/toggle + Back")
    @Severity(SeverityLevel.NORMAL)
    public void packageActionBack() {
        AppSettingsPage page = reachAppSettings();
        if (page.isRetryVisible() && page.isErrorVisible()) {
            page.tapRetry();
            sleepQuiet(800);
        } else if (page.switchCount() > 0) {
            page.tapFirstSwitch();
            sleepQuiet(400);
            page.tapFirstSwitch();
            sleepQuiet(400);
        }
        page.tapBack();
        sleepQuiet(800);
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 10, description = "AS-E10: Home after drawer close")
    @Severity(SeverityLevel.NORMAL)
    public void homeAfterDrawerClose() {
        AppSettingsPage page = reachAppSettings();
        page.tapBack();
        sleepQuiet(700);
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        assertThat(new HomePage().isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
    }
}
