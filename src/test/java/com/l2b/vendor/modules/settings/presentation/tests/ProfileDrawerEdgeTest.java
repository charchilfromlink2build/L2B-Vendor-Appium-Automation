package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Profile drawer edge — chrome only. No Account / KYC / Settings page opens.
 * Never Confirm Log Out / Accept / Decline.
 *
 * <p><b>Critical Edge Cases (PD-E1–E14)</b>
 * <ol>
 *   <li>E1 Rapid Profile open/close stays Vendor</li>
 *   <li>E2 Double Profile tap stays drawer / Vendor</li>
 *   <li>E3 Log Out Cancel twice idempotent</li>
 *   <li>E4 Log Out? device Back must not confirm logout</li>
 *   <li>E5 Header + rows layout publish (soft batch)</li>
 *   <li>E6 Right scrim dismiss only</li>
 *   <li>E7 Reopen after scrim — all rows intact</li>
 *   <li>E8 Rapid Close-menu open/close stays Vendor</li>
 *   <li>E9 Company code L2B- intact after Cancel</li>
 *   <li>E10 Profile avatar while drawer open</li>
 *   <li>E11 Package Vendor through open+Cancel+close</li>
 *   <li>E12 Chrome-only path never launcher</li>
 *   <li>E13 Back → Profile → Cancel → Back chain</li>
 *   <li>E14 All rows still present after Cancel</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Profile drawer edge — rental 9000000001")
public class ProfileDrawerEdgeTest extends ProfileDrawerBaseTest {

    @Test(priority = 1, description = "PD-E1: rapid Profile open/close stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidOpenCloseStaysVendor() {
        ProfileDrawerPage page = reachProfileDrawer();
        for (int i = 0; i < 3; i++) {
            page.pressDeviceBack();
            sleepQuiet(500);
            new HomePage().tapDesc("Profile");
            sleepQuiet(700);
        }
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
    }

    @Test(priority = 2, description = "PD-E2: double Profile tap stays drawer / Vendor")
    @Severity(SeverityLevel.NORMAL)
    public void doubleProfileTapStaysDrawer() {
        ProfileDrawerPage page = reachProfileDrawer();
        try {
            new HomePage().tapDesc("Profile");
        } catch (RuntimeException e) {
            Allure.parameter("secondProfile", e.getClass().getSimpleName());
        }
        sleepQuiet(800);
        Allure.parameter("after", classifyRentalNow());
        assertThat(page.isDisplayedNow() || "home".equals(classifyRentalNow())
                || page.isLogoutDialogVisible()).isTrue();
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 3, description = "PD-E3: Log Out Cancel twice idempotent")
    @Severity(SeverityLevel.CRITICAL)
    public void logOutCancelTwice() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(900);
        page.tapLogoutCancel();
        sleepQuiet(700);
        assertThat(page.isDisplayedNow()).isTrue();
        page.tapLogOut();
        sleepQuiet(900);
        page.tapLogoutCancel();
        sleepQuiet(700);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isLogoutDialogVisible()).isFalse();
    }

    @Test(priority = 4, description = "PD-E4: logout dialog device Back dismisses safely")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Back on Log Out? must not confirm logout.")
    public void logoutDialogDeviceBackSafe() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(900);
        assertThat(page.isLogoutDialogVisible()).isTrue();
        page.pressDeviceBack();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        page.attachScreenshot("pd-e4-dialog-back");
        assertThat(page.isLogoutDialogVisible()).isFalse();
        assertThat(classifyRentalNow()).isIn("home-drawer", "home", "vendor-other");
        assertThat(vendorPackage()).contains("l2b");
        page = ensureDrawerOpen(page);
        assertThat(page.isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
    }

    @Test(priority = 5, description = "PD-E5: header + rows layout publish checks")
    @Severity(SeverityLevel.CRITICAL)
    public void layoutPublishChecks() {
        ProfileDrawerPage page = reachProfileDrawer();
        assertThat(page.isDisplayedNow()).as("hard: drawer open").isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.headerLayoutOk()).as("header name above company code").isTrue();
        softly.assertThat(page.menuRowsLayoutOk()).as("menu row pitch / shared x").isTrue();
        softly.assertThat(page.rowLeftInsetOk()).as("row left inset band").isTrue();
        softly.assertThat(page.logOutBelowSettingsOk()).as("Log Out below Settings").isTrue();
        softly.assertThat(page.scrimRightEdgeOk()).as("scrim on right edge").isTrue();
        softly.assertAll();
    }

    @Test(priority = 6, description = "PD-E6: right scrim dismiss (left drawer body is not dismiss)")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump lesson: x=80 hit Account row. True dismiss is right scrim only.")
    public void rightScrimDismisses() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapOutsideScrim();
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isFalse();
        assertThat(classifyRentalNow()).isIn("home", "extend-time");
    }

    @Test(priority = 7, description = "PD-E7: reopen after scrim — all rows intact")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterScrimAllRows() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapOutsideScrim();
        sleepQuiet(800);
        new HomePage().tapDesc("Profile");
        sleepQuiet(900);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.allMenuRowsVisible()).isTrue();
    }

    @Test(priority = 8, description = "PD-E8: rapid Close-menu open/close stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidCloseMenuOpenClose() {
        ProfileDrawerPage page = reachProfileDrawer();
        for (int i = 0; i < 3; i++) {
            page.tapCloseNavigationMenu();
            sleepQuiet(500);
            new HomePage().tapDesc("Profile");
            sleepQuiet(700);
            page.waitUntilLoaded();
        }
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 9, description = "PD-E9: company code L2B- intact after Cancel")
    @Severity(SeverityLevel.NORMAL)
    public void companyCodeIntactAfterCancel() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(800);
        page.tapLogoutCancel();
        sleepQuiet(700);
        assertThat(page.isCompanyCodeVisible()).isTrue();
        assertThat(page.visibleTexts().stream().anyMatch(t -> t.matches("L2B-[A-Z0-9]+"))).isTrue();
    }

    @Test(priority = 10, description = "PD-E10: Profile avatar desc still present while drawer open")
    @Severity(SeverityLevel.NORMAL)
    public void profileAvatarWhileOpen() {
        ProfileDrawerPage page = reachProfileDrawer();
        assertThat(page.isProfileDescVisible()).isTrue();
        page.tapLogOut();
        sleepQuiet(800);
        page.tapLogoutCancel();
        sleepQuiet(700);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 11, description = "PD-E11: package stays Vendor through open+logout-cancel+close")
    @Severity(SeverityLevel.BLOCKER)
    public void packageStaysVendorThroughChrome() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(800);
        page.tapLogoutCancel();
        sleepQuiet(600);
        page.tapCloseNavigationMenu();
        sleepQuiet(800);
        Allure.parameter("end", classifyRentalNow());
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
    }

    @Test(priority = 12, description = "PD-E12: chrome-only path never leaves launcher")
    @Severity(SeverityLevel.CRITICAL)
    public void chromeOnlyNeverLauncher() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(700);
        page.tapLogoutCancel();
        sleepQuiet(500);
        page.pressDeviceBack();
        sleepQuiet(800);
        String end = classifyRentalNow();
        Allure.parameter("end", end);
        assertThat(end).isNotEqualTo("launcher");
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 13, description = "PD-E13: Back → Profile → Log Out Cancel → Back chain")
    @Severity(SeverityLevel.CRITICAL)
    public void backProfileLogoutCancelBackChain() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.pressDeviceBack();
        sleepQuiet(700);
        new HomePage().tapDesc("Profile");
        sleepQuiet(900);
        page.waitUntilLoaded();
        page.tapLogOut();
        sleepQuiet(800);
        page.tapLogoutCancel();
        sleepQuiet(600);
        page.pressDeviceBack();
        sleepQuiet(800);
        assertThat(classifyRentalNow()).isIn("home", "home-drawer", "extend-time");
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
    }

    @Test(priority = 14, description = "PD-E14: all rows still present after Cancel")
    @Severity(SeverityLevel.CRITICAL)
    public void allRowsAfterLogoutCancel() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(900);
        page.tapLogoutCancel();
        sleepQuiet(700);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.allMenuRowsVisible()).isTrue();
        assertThat(page.isLogOutVisible()).isTrue();
    }
}
