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
import org.testng.annotations.Test;

/**
 * Profile drawer interact — Happy Path actions (chrome only).
 * Do not open Account / KYC / Team / Help / … pages here.
 * Never Confirm Log Out / Accept / Decline.
 *
 * <p><b>Happy Path actions (PD-I1–I14)</b>
 * <ol>
 *   <li>I1 Device Back closes → Home</li>
 *   <li>I2 Close navigation menu → Home</li>
 *   <li>I3 Right scrim tap → Home</li>
 *   <li>I4 Reopen Profile after Back</li>
 *   <li>I5 Log Out → Cancel stays drawer</li>
 *   <li>I6 Log Out dialog body chrome then Cancel</li>
 *   <li>I7 Reopen after Close menu</li>
 *   <li>I8 Reopen after scrim</li>
 *   <li>I9 Back → Profile → scrim close</li>
 *   <li>I10 Header chrome survives reopen</li>
 *   <li>I11 All row labels after Cancel</li>
 *   <li>I12 Close + reopen + Log Out Cancel chain</li>
 *   <li>I13 Log Out below Settings after Cancel</li>
 *   <li>I14 Scrim present while open then close</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Profile drawer interact — rental 9000000001")
public class ProfileDrawerInteractTest extends ProfileDrawerBaseTest {

    @Test(priority = 1, description = "PD-I1: device Back closes drawer → Home")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBackClosesDrawer() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.pressDeviceBack();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        page.attachScreenshot("pd-i1-back");
        assertThat(page.isDisplayedNow()).isFalse();
        assertThat(classifyRentalNow()).isIn("home", "extend-time", "quick-booking");
    }

    @Test(priority = 2, description = "PD-I2: Close navigation menu closes → Home")
    @Severity(SeverityLevel.CRITICAL)
    public void closeNavigationMenuCloses() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapCloseNavigationMenu();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(page.isDisplayedNow()).isFalse();
        assertThat(classifyRentalNow()).isIn("home", "extend-time");
    }

    @Test(priority = 3, description = "PD-I3: right scrim tap closes → Home")
    @Severity(SeverityLevel.CRITICAL)
    public void outsideScrimCloses() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapOutsideScrim();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(page.isDisplayedNow()).isFalse();
        assertThat(classifyRentalNow()).isIn("home", "extend-time");
    }

    @Test(priority = 4, description = "PD-I4: reopen Profile after Back")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterBack() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.pressDeviceBack();
        sleepQuiet(800);
        assertThat(classifyRentalNow()).isIn("home", "extend-time", "quick-booking");
        new HomePage().tapDesc("Profile");
        sleepQuiet(900);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 5, description = "PD-I5: Log Out → Cancel stays drawer")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Never tap Log out confirm. Cancel returns to drawer, still Vendor.")
    public void logOutCancelStaysDrawer() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(1000);
        assertThat(page.isLogoutDialogVisible()).isTrue();
        page.tapLogoutCancel();
        sleepQuiet(800);
        page.attachScreenshot("pd-i5-cancelled");
        assertThat(page.isLogoutDialogVisible()).isFalse();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 6, description = "PD-I6: Log Out dialog body + Cancel + Log out chrome")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 10 chrome only — assert dialog copy, Cancel only.")
    public void logOutDialogChromeThenCancel() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(1000);
        page.attachScreenshot("pd-i6-dialog");
        assertThat(page.isLogoutDialogVisible()).isTrue();
        assertThat(page.isLogoutDialogBodyVisible()).isTrue();
        assertThat(page.isLogoutCancelVisible()).isTrue();
        assertThat(page.isLogoutConfirmVisible()).isTrue();
        page.tapLogoutCancel();
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.allMenuRowsVisible()).isTrue();
    }

    @Test(priority = 7, description = "PD-I7: reopen after Close navigation menu")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterCloseMenu() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapCloseNavigationMenu();
        sleepQuiet(800);
        new HomePage().tapDesc("Profile");
        sleepQuiet(900);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isVendorNameVisible()).isTrue();
        assertThat(page.isCompanyCodeVisible()).isTrue();
    }

    @Test(priority = 8, description = "PD-I8: reopen after right scrim")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterScrim() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapOutsideScrim();
        sleepQuiet(800);
        new HomePage().tapDesc("Profile");
        sleepQuiet(900);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.allMenuRowsVisible()).isTrue();
    }

    @Test(priority = 9, description = "PD-I9: close via Back then Profile then scrim")
    @Severity(SeverityLevel.CRITICAL)
    public void backThenProfileThenScrim() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.pressDeviceBack();
        sleepQuiet(700);
        new HomePage().tapDesc("Profile");
        sleepQuiet(900);
        page.waitUntilLoaded();
        page.tapOutsideScrim();
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isFalse();
        assertThat(classifyRentalNow()).isIn("home", "extend-time");
    }

    @Test(priority = 10, description = "PD-I10: header chrome survives reopen")
    @Severity(SeverityLevel.NORMAL)
    public void headerChromeSurvivesReopen() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapCloseNavigationMenu();
        sleepQuiet(700);
        new HomePage().tapDesc("Profile");
        sleepQuiet(900);
        page.waitUntilLoaded();
        assertThat(page.isVendorNameVisible()).isTrue();
        assertThat(page.isCompanyIdLabelVisible()).isTrue();
        assertThat(page.isCompanyCodeVisible()).isTrue();
        assertThat(page.headerLayoutOk()).isTrue();
    }

    @Test(priority = 11, description = "PD-I11: all menu row labels still visible after Cancel")
    @Severity(SeverityLevel.CRITICAL)
    public void allRowsVisibleAfterLogoutCancel() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(900);
        page.tapLogoutCancel();
        sleepQuiet(700);
        assertThat(page.allMenuRowsVisible()).isTrue();
        for (String row : ProfileDrawerPage.MENU_ROWS) {
            assertThat(page.isRowVisible(row)).as(row).isTrue();
        }
    }

    @Test(priority = 12, description = "PD-I12: close + reopen + Log Out Cancel chain")
    @Severity(SeverityLevel.CRITICAL)
    public void closeReopenLogoutCancelChain() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapCloseNavigationMenu();
        sleepQuiet(800);
        new HomePage().tapDesc("Profile");
        sleepQuiet(900);
        page.waitUntilLoaded();
        page.tapLogOut();
        sleepQuiet(900);
        page.tapLogoutCancel();
        sleepQuiet(700);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 13, description = "PD-I13: Log Out CTA still below Settings after Cancel")
    @Severity(SeverityLevel.NORMAL)
    public void logOutBelowSettingsAfterCancel() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(800);
        page.tapLogoutCancel();
        sleepQuiet(700);
        assertThat(page.isLogOutVisible()).isTrue();
        assertThat(page.logOutBelowSettingsOk()).isTrue();
    }

    @Test(priority = 14, description = "PD-I14: scrim Close affordance present while open")
    @Severity(SeverityLevel.NORMAL)
    public void scrimPresentWhileOpen() {
        ProfileDrawerPage page = reachProfileDrawer();
        assertThat(page.isCloseNavigationMenuVisible()).isTrue();
        assertThat(page.scrimRightEdgeOk()).isTrue();
        page.tapCloseNavigationMenu();
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isFalse();
    }
}
