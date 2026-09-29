package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.settings.presentation.pages.AccountPage;
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
 * Account edge — Never Save Changes / Accept / Decline / Log Out Confirm.
 *
 * <p><b>Critical Edge Cases (AC-E1–E14)</b>
 * <ol>
 *   <li>E1 Rapid Edit Cancel stays Vendor</li>
 *   <li>E2 Rapid Update Cancel stays Vendor</li>
 *   <li>E3 Layout publish checks (soft)</li>
 *   <li>E4 Photo open/close twice</li>
 *   <li>E5 Edit+Update Cancel then Back → drawer</li>
 *   <li>E6 Reopen after drawer dismiss</li>
 *   <li>E7 Save Changes visible in Edit but never tapped</li>
 *   <li>E8 Package Vendor through photo+edit+back</li>
 *   <li>E9 Update mode device Back safe</li>
 *   <li>E10 Email intact after Cancel</li>
 *   <li>E11 Account from drawer only</li>
 *   <li>E12 No Accept/Decline from Account path</li>
 *   <li>E13 Labels stacked after Edit Cancel</li>
 *   <li>E14 Profile Info survives photo Close</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Account edge — rental 9000000001")
public class AccountEdgeTest extends AccountBaseTest {

    @Test(priority = 1, description = "AC-E1: rapid Edit Cancel stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidEditCancelStaysVendor() {
        AccountPage page = reachAccount();
        for (int i = 0; i < 3; i++) {
            page.tapEdit();
            sleepQuiet(500);
            page.tapCancel();
            sleepQuiet(500);
        }
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("account");
    }

    @Test(priority = 2, description = "AC-E2: rapid Update Cancel stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidUpdateCancelStaysVendor() {
        AccountPage page = reachAccount();
        for (int i = 0; i < 3; i++) {
            page.tapUpdate();
            sleepQuiet(500);
            page.tapCancel();
            sleepQuiet(500);
        }
        assertThat(page.isUpdateVisible()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 3, description = "AC-E3: layout publish checks")
    @Severity(SeverityLevel.CRITICAL)
    public void layoutPublishChecks() {
        AccountPage page = reachAccount();
        assertThat(page.isDisplayedNow()).as("hard: Account open").isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.headerBackAligned()).as("Back | Account header").isTrue();
        softly.assertThat(page.titleLeftInsetOk()).as("title left inset").isTrue();
        softly.assertThat(page.profileInfoEditLayoutOk()).as("Profile Info | Edit").isTrue();
        softly.assertThat(page.companyInfoUpdateLayoutOk()).as("Company Info | Update").isTrue();
        softly.assertThat(page.fieldLabelsLayoutOk()).as("field label stack").isTrue();
        softly.assertAll();
    }

    @Test(priority = 4, description = "AC-E4: photo open/close twice")
    @Severity(SeverityLevel.NORMAL)
    public void photoOpenCloseTwice() {
        AccountPage page = reachAccount();
        for (int i = 0; i < 2; i++) {
            page.tapProfilePhoto();
            sleepQuiet(800);
            if (page.isPhotoCloseVisible()) {
                page.tapPhotoClose();
                sleepQuiet(600);
            }
        }
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isPhoneValueVisible()).isTrue();
    }

    @Test(priority = 5, description = "AC-E5: Edit Cancel + Update Cancel + Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void editUpdateCancelThenBack() {
        AccountPage page = reachAccount();
        page.tapEdit();
        sleepQuiet(600);
        page.tapCancel();
        sleepQuiet(500);
        page.tapUpdate();
        sleepQuiet(600);
        page.tapCancel();
        sleepQuiet(500);
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 6, description = "AC-E6: reopen Account after drawer dismiss+Profile")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterDrawerDismiss() {
        AccountPage page = reachAccount();
        page.tapHeaderBack();
        sleepQuiet(700);
        new ProfileDrawerPage().tapCloseNavigationMenu();
        sleepQuiet(800);
        // Stay on session — do not full re-login
        new com.l2b.vendor.modules.home.presentation.pages.HomePage().tapDesc("Profile");
        sleepQuiet(900);
        new ProfileDrawerPage().tapRow("Account");
        sleepQuiet(1100);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isPhoneValueVisible()).isTrue();
    }

    @Test(priority = 7, description = "AC-E7: Save Changes visible in Edit but never tapped")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Guard: Save Changes present in edit mode; Cancel only.")
    public void saveChangesPresentButNeverTapped() {
        AccountPage page = reachAccount();
        page.tapEdit();
        sleepQuiet(800);
        assertThat(page.isEditModeVisible()).isTrue();
        assertThat(page.visibleTexts()).contains("Save Changes");
        page.tapCancel();
        sleepQuiet(700);
        assertThat(page.visibleTexts()).doesNotContain("Save Changes");
        assertThat(page.isPhoneValueVisible()).isTrue();
    }

    @Test(priority = 8, description = "AC-E8: package stays Vendor through photo+edit+back")
    @Severity(SeverityLevel.BLOCKER)
    public void packageStaysVendorThroughChrome() {
        AccountPage page = reachAccount();
        page.tapProfilePhoto();
        sleepQuiet(700);
        if (page.isPhotoCloseVisible()) {
            page.tapPhotoClose();
            sleepQuiet(600);
        }
        page.tapEdit();
        sleepQuiet(600);
        page.tapCancel();
        sleepQuiet(500);
        page.tapHeaderBack();
        sleepQuiet(800);
        Allure.parameter("end", classifyRentalNow());
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isNotEqualTo("launcher");
    }

    @Test(priority = 9, description = "AC-E9: Update mode device Back safe")
    @Severity(SeverityLevel.CRITICAL)
    public void updateModeDeviceBackSafe() {
        AccountPage page = reachAccount();
        page.tapUpdate();
        sleepQuiet(800);
        page.pressDeviceBack();
        sleepQuiet(900);
        page = ensureAccountReadMode(page);
        assertThat(classifyRentalNow()).isIn("account", "home-drawer", "home");
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 10, description = "AC-E10: email still contains @ after Cancel")
    @Severity(SeverityLevel.NORMAL)
    public void emailIntactAfterCancel() {
        AccountPage page = reachAccount();
        page.tapUpdate();
        sleepQuiet(700);
        page.tapCancel();
        sleepQuiet(700);
        assertThat(page.isEmailValueVisible()).as("email intact").isTrue();
    }

    @Test(priority = 11, description = "AC-E11: Account after KYC-style recover from drawer only")
    @Severity(SeverityLevel.NORMAL)
    public void accountFromDrawerOnly() {
        AccountPage page = reachAccount();
        page.tapHeaderBack();
        sleepQuiet(700);
        assertThat(profileDrawerNow()).isTrue();
        new ProfileDrawerPage().tapRow("Account");
        sleepQuiet(1000);
        page.waitUntilLoaded();
        assertThat(classifyRentalNow()).isEqualTo("account");
    }

    @Test(priority = 12, description = "AC-E12: no Accept/Decline from Account path")
    @Severity(SeverityLevel.CRITICAL)
    public void noAcceptDeclineFromAccount() {
        AccountPage page = reachAccount();
        page.tapEdit();
        sleepQuiet(600);
        page.tapCancel();
        sleepQuiet(500);
        page.tapHeaderBack();
        sleepQuiet(700);
        new ProfileDrawerPage().tapCloseNavigationMenu();
        sleepQuiet(800);
        String end = classifyRentalNow();
        Allure.parameter("end", end);
        assertThat(end).isNotEqualTo("launcher");
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 13, description = "AC-E13: read labels after Edit Cancel still stacked")
    @Severity(SeverityLevel.NORMAL)
    public void labelsStackedAfterEditCancel() {
        AccountPage page = reachAccount();
        page.tapEdit();
        sleepQuiet(600);
        page.tapCancel();
        sleepQuiet(600);
        assertThat(page.fieldLabelsLayoutOk()).isTrue();
    }

    @Test(priority = 14, description = "AC-E14: Profile Info heading survives photo Close")
    @Severity(SeverityLevel.NORMAL)
    public void profileInfoSurvivesPhoto() {
        AccountPage page = reachAccount();
        page.tapProfilePhoto();
        sleepQuiet(800);
        if (page.isPhotoCloseVisible()) {
            page.tapPhotoClose();
            sleepQuiet(700);
        }
        assertThat(page.isProfileInfoVisible()).isTrue();
        assertThat(page.profileInfoEditLayoutOk()).isTrue();
    }
}
