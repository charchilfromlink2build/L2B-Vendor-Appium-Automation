package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Profile drawer landing — Home Profile on {@code 9000000001}.
 * Dump {@code /tmp/l2b-profile-drawer-0001-20260929}.
 *
 * <p><b>Happy Path (PD-L1–L12)</b> — identity / chrome only (no Account/KYC pages):
 * <ol>
 *   <li>L1 Profile opens drawer</li>
 *   <li>L2 Vendor name + Company Id header</li>
 *   <li>L3 All 11 menu row labels present</li>
 *   <li>L4 Log Out CTA at bottom</li>
 *   <li>L5 Close navigation menu scrim</li>
 *   <li>L6 Profile avatar still in chrome</li>
 *   <li>L7 Menu rows layout pitch</li>
 *   <li>L8 Row left inset</li>
 *   <li>L9 Log Out opens dialog chrome (Cancel only)</li>
 *   <li>L10 Stay Vendor package</li>
 *   <li>L11 Company code L2B- prefix</li>
 *   <li>L12 Drawer hides Home greeting</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Profile drawer landing — rental 9000000001")
public class ProfileDrawerLandingTest extends ProfileDrawerBaseTest {

    @Test(priority = 1, description = "PD-L1: Home Profile → drawer")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Tap Profile. Expect classify=home-drawer, Account + Log Out.")
    public void profileOpensDrawer() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.attachScreenshot("pd-l1");
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 2, description = "PD-L2: vendor name + Company Id chrome")
    @Severity(SeverityLevel.CRITICAL)
    public void headerNameAndCompanyId() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.attachScreenshot("pd-l2");
        assertThat(page.isVendorNameVisible()).isTrue();
        assertThat(page.isCompanyIdLabelVisible()).isTrue();
        assertThat(page.isCompanyCodeVisible()).isTrue();
        assertThat(page.headerLayoutOk()).isTrue();
    }

    @Test(priority = 3, description = "PD-L3: all 11 menu rows")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Account → Settings rows from dump 01.")
    public void allMenuRowsPresent() {
        ProfileDrawerPage page = reachProfileDrawer();
        assertThat(page.allMenuRowsVisible()).isTrue();
        for (String row : ProfileDrawerPage.MENU_ROWS) {
            Allure.parameter("row_" + row.replace(' ', '_'),
                    String.valueOf(page.isRowVisible(row)));
            assertThat(page.isRowVisible(row)).as(row).isTrue();
        }
    }

    @Test(priority = 4, description = "PD-L4: Log Out CTA at bottom")
    @Severity(SeverityLevel.CRITICAL)
    public void logOutCtaVisible() {
        ProfileDrawerPage page = reachProfileDrawer();
        assertThat(page.isLogOutVisible()).isTrue();
        assertThat(page.logOutBelowSettingsOk())
                .as("Log Out below Settings with bottom inset")
                .isTrue();
    }

    @Test(priority = 5, description = "PD-L5: Close navigation menu scrim")
    @Severity(SeverityLevel.CRITICAL)
    public void closeNavigationMenuScrim() {
        ProfileDrawerPage page = reachProfileDrawer();
        assertThat(page.isCloseNavigationMenuVisible()).isTrue();
        assertThat(page.scrimRightEdgeOk())
                .as("Scrim on right edge (left drawer)")
                .isTrue();
    }

    @Test(priority = 6, description = "PD-L6: Profile avatar still in chrome")
    @Severity(SeverityLevel.NORMAL)
    public void profileAvatarStillVisible() {
        ProfileDrawerPage page = reachProfileDrawer();
        assertThat(page.isProfileDescVisible()).isTrue();
    }

    @Test(priority = 7, description = "PD-L7: menu rows layout pitch")
    @Severity(SeverityLevel.CRITICAL)
    public void menuRowsLayoutPitch() {
        ProfileDrawerPage page = reachProfileDrawer();
        assertThat(page.menuRowsLayoutOk())
                .as("Rows stacked equal pitch, shared left x")
                .isTrue();
    }

    @Test(priority = 8, description = "PD-L8: row left inset padding")
    @Severity(SeverityLevel.NORMAL)
    public void rowLeftInset() {
        ProfileDrawerPage page = reachProfileDrawer();
        assertThat(page.rowLeftInsetOk())
                .as("Account x in publish band ~179")
                .isTrue();
    }

    @Test(priority = 9, description = "PD-L9: Log Out opens confirm dialog chrome")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 10: Log Out? + body + Cancel + Log out. Cancel only — never confirm.")
    public void logOutOpensDialogChrome() {
        ProfileDrawerPage page = reachProfileDrawer();
        page.tapLogOut();
        sleepQuiet(1000);
        page.attachScreenshot("pd-l9-logout-dialog");
        assertThat(page.isLogoutDialogVisible()).isTrue();
        assertThat(page.isLogoutDialogBodyVisible()).isTrue();
        assertThat(page.isLogoutCancelVisible()).isTrue();
        assertThat(page.isLogoutConfirmVisible()).isTrue();
        page.tapLogoutCancel();
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 10, description = "PD-L10: stay Vendor package")
    @Severity(SeverityLevel.BLOCKER)
    public void stayVendorPackage() {
        ProfileDrawerPage page = reachProfileDrawer();
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
        assertThat(vendorPackage()).contains("l2b");
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 11, description = "PD-L11: Company code L2B- prefix")
    @Severity(SeverityLevel.NORMAL)
    public void companyCodePrefix() {
        ProfileDrawerPage page = reachProfileDrawer();
        boolean ok = page.visibleTexts().stream().anyMatch(t -> t.matches("L2B-[A-Z0-9]+"));
        assertThat(ok).as("L2B- company code").isTrue();
    }

    @Test(priority = 12, description = "PD-L12: drawer hides Home greeting")
    @Severity(SeverityLevel.NORMAL)
    public void drawerHidesHomeGreeting() {
        ProfileDrawerPage page = reachProfileDrawer();
        assertThat(page.visibleTexts().stream().noneMatch(t -> t.startsWith("Good ")))
                .as("Home greeting not primary while drawer open")
                .isTrue();
        assertThat(page.isAccountVisible()).isTrue();
    }
}
