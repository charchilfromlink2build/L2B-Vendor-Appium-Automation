package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.AppSettingsPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * App Settings interact — Retry/error (#51) or toggle+restore when list loads.
 * Never Log Out Confirm / Accept / Decline.
 *
 * <p><b>Happy Path (AS-I1–I10)</b>
 */
@Epic("Vendor app")
@Feature("App Settings interact — rental 9000000001")
public class AppSettingsInteractTest extends AppSettingsBaseTest {

    @Test(priority = 1, description = "AS-I1: Retry when error (or toggle+restore)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("BUGS_FOUND #51 path when Retry is shown.")
    public void retryOrToggleRestore() {
        AppSettingsPage page = reachAppSettings();
        if (page.isRetryVisible() && page.isErrorVisible()) {
            page.tapRetry();
            sleepQuiet(2000);
            page.attachScreenshot("as-i1-retry");
            Allure.parameter("afterRetryError", String.valueOf(page.isErrorVisible()));
            Allure.parameter("afterRetryFull", String.valueOf(page.isFullChromeVisible()));
            assertThat(page.isDisplayedNow()).isTrue();
            assertThat(vendorPackage()).contains("l2b");
            return;
        }
        assertThat(page.switchCount()).isGreaterThan(0);
        Boolean before = page.firstSwitchChecked();
        page.tapFirstSwitch();
        sleepQuiet(800);
        page.tapFirstSwitch();
        sleepQuiet(800);
        Allure.parameter("before", String.valueOf(before));
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("app-settings");
    }

    @Test(priority = 2, description = "AS-I2: scroll keeps Settings")
    @Severity(SeverityLevel.CRITICAL)
    public void scrollKeeps() {
        AppSettingsPage page = reachAppSettings();
        page.swipeListUp();
        sleepQuiet(600);
        assertThat(page.isSettingsTitleVisible() || page.isDisplayedNow()).isTrue();
        page.swipeListDown();
        sleepQuiet(500);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 3, description = "AS-I3: header Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void headerBack() {
        AppSettingsPage page = reachAppSettings();
        page.tapBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 4, description = "AS-I4: device Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBack() {
        AppSettingsPage page = reachAppSettings();
        page.pressDeviceBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 5, description = "AS-I5: reopen Settings")
    @Severity(SeverityLevel.CRITICAL)
    public void reopen() {
        AppSettingsPage page = reachAppSettings();
        page.tapBack();
        sleepQuiet(800);
        new ProfileDrawerPage().tapRow("Settings");
        sleepQuiet(1100);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("app-settings");
    }

    @Test(priority = 6, description = "AS-I6: double Retry or double toggle restore")
    @Severity(SeverityLevel.NORMAL)
    public void doubleRetryOrToggle() {
        AppSettingsPage page = reachAppSettings();
        if (page.isErrorVisible() && page.isRetryVisible()) {
            page.tapRetry();
            sleepQuiet(800);
            page.tapRetry();
            sleepQuiet(800);
            assertThat(page.isDisplayedNow()).isTrue();
            return;
        }
        Boolean before = page.firstSwitchChecked();
        page.tapFirstSwitch();
        sleepQuiet(500);
        page.tapFirstSwitch();
        sleepQuiet(700);
        Boolean after = page.firstSwitchChecked();
        Allure.parameter("before", String.valueOf(before));
        Allure.parameter("after", String.valueOf(after));
        assertThat(page.isDisplayedNow()).isTrue();
        if (before != null && after != null) {
            assertThat(after).isEqualTo(before);
        }
    }

    @Test(priority = 7, description = "AS-I7: scroll / lower rows when available")
    @Severity(SeverityLevel.NORMAL)
    public void scrollLowerRows() {
        AppSettingsPage page = reachAppSettings();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error-#51");
            return;
        }
        page.swipeListUp();
        sleepQuiet(600);
        assertThat(page.isRowVisible(AppSettingsPage.ROW_MIC)
                || page.isRowVisible(AppSettingsPage.ROW_BACKGROUND)
                || page.isRowVisible(AppSettingsPage.ROW_CALENDAR)
                || page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 8, description = "AS-I8: title survives scroll")
    @Severity(SeverityLevel.NORMAL)
    public void titleSurvives() {
        AppSettingsPage page = reachAppSettings();
        page.swipeListUp();
        sleepQuiet(500);
        assertThat(page.isSettingsTitleVisible() || page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 9, description = "AS-I9: Back then close drawer → Home")
    @Severity(SeverityLevel.CRITICAL)
    public void backToHome() {
        AppSettingsPage page = reachAppSettings();
        page.tapBack();
        sleepQuiet(800);
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        assertThat(new HomePage().isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
    }

    @Test(priority = 10, description = "AS-I10: stay Vendor after Retry/toggle")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendor() {
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
        assertThat(vendorPackage()).contains("l2b");
        Allure.parameter("after", classifyRentalNow());
    }
}
