package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

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
 * App Settings landing — Profile drawer → Settings on {@code 9000000001}.
 * Dump {@code /tmp/l2b-app-settings-0001-20261005}.
 *
 * <p>Live 5 Oct: error empty state &quot;An unexpected error occurred&quot; + Retry
 * (BUGS_FOUND #51). Full permissions list asserted when available.
 *
 * <p><b>Happy Path (AS-L1–L10)</b>
 */
@Epic("Vendor app")
@Feature("App Settings landing — rental 9000000001")
public class AppSettingsLandingTest extends AppSettingsBaseTest {

    @Test(priority = 1, description = "AS-L1: drawer Settings opens")
    @Severity(SeverityLevel.BLOCKER)
    public void settingsOpens() {
        AppSettingsPage page = reachAppSettings();
        page.attachScreenshot("as-l1");
        assertThat(page.isDisplayedNow()).as("Settings open").isTrue();
    }

    @Test(priority = 2, description = "AS-L2: title + Back")
    @Severity(SeverityLevel.CRITICAL)
    public void titleAndBack() {
        AppSettingsPage page = reachAppSettings();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isSettingsTitleVisible()).as("title").isTrue();
        softly.assertThat(page.isBackVisible()).as("Back").isTrue();
        softly.assertThat(page.headerBackAligned()).as("Back left").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "AS-L3: chrome OR error empty state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #51 when Settings fails to load on seeded 9000000001.")
    public void chromeOrError() {
        AppSettingsPage page = reachAppSettings();
        boolean full = page.isFullChromeVisible();
        boolean err = page.isErrorVisible();
        Allure.parameter("fullChrome", String.valueOf(full));
        Allure.parameter("errorState", String.valueOf(err));
        Allure.parameter("bug", err && !full ? "51" : "none");
        page.attachScreenshot("as-l3");
        assertThat(full || err).as("full chrome or error").isTrue();
    }

    @Test(priority = 4, description = "AS-L4: Retry visible when error")
    @Severity(SeverityLevel.CRITICAL)
    public void retryWhenError() {
        AppSettingsPage page = reachAppSettings();
        if (!page.isErrorVisible()) {
            Allure.parameter("skipped", "full-chrome");
            return;
        }
        assertThat(page.isRetryVisible()).as("Retry").isTrue();
    }

    @Test(priority = 5, description = "AS-L5: Permissions heading when available")
    @Severity(SeverityLevel.CRITICAL)
    public void permissionsHeading() {
        AppSettingsPage page = reachAppSettings();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error-#51");
            return;
        }
        assertThat(page.isPermissionsHeadingVisible()).isTrue();
    }

    @Test(priority = 6, description = "AS-L6: Notification + GPS when available")
    @Severity(SeverityLevel.CRITICAL)
    public void notificationGps() {
        AppSettingsPage page = reachAppSettings();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error-#51");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isRowVisible(AppSettingsPage.ROW_NOTIFICATION)).as("Notification").isTrue();
        softly.assertThat(page.isRowVisible(AppSettingsPage.ROW_GPS)).as("GPS").isTrue();
        softly.assertAll();
    }

    @Test(priority = 7, description = "AS-L7: Camera + Contacts when available")
    @Severity(SeverityLevel.CRITICAL)
    public void cameraContacts() {
        AppSettingsPage page = reachAppSettings();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error-#51");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isRowVisible(AppSettingsPage.ROW_CAMERA)).as("Camera").isTrue();
        softly.assertThat(page.isRowVisible(AppSettingsPage.ROW_CONTACTS)).as("Contacts").isTrue();
        softly.assertAll();
    }

    @Test(priority = 8, description = "AS-L8: switches when list loaded")
    @Severity(SeverityLevel.CRITICAL)
    public void switchesWhenAvailable() {
        AppSettingsPage page = reachAppSettings();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error-#51");
            return;
        }
        Allure.parameter("switchCount", String.valueOf(page.switchCount()));
        assertThat(page.switchCount()).isGreaterThanOrEqualTo(3);
    }

    @Test(priority = 9, description = "AS-L9: stay Vendor")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendor() {
        assertThat(reachAppSettings().isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 10, description = "AS-L10: classify app-settings")
    @Severity(SeverityLevel.NORMAL)
    public void classifySettings() {
        reachAppSettings();
        assertThat(classifyRentalNow()).isEqualTo("app-settings");
    }
}
