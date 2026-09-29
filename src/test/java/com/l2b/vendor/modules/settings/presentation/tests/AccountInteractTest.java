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
import org.testng.annotations.Test;

/**
 * Account interact — Happy Path actions. Never Save Changes / Accept / Decline / Log Out Confirm.
 *
 * <p><b>Happy Path actions (AC-I1–I14)</b>
 * <ol>
 *   <li>I1 Edit → Cancel</li>
 *   <li>I2 Update → Cancel</li>
 *   <li>I3 Profile photo → Close</li>
 *   <li>I4 Header Back → drawer</li>
 *   <li>I5 Device Back → drawer</li>
 *   <li>I6 Reopen Account after Back</li>
 *   <li>I7 Edit then device Back safe</li>
 *   <li>I8 Update then Edit Cancel chain</li>
 *   <li>I9 Phone unchanged after Cancel</li>
 *   <li>I10 Photo then Back → drawer</li>
 *   <li>I11 Double Edit Cancel</li>
 *   <li>I12 Company Info after Update Cancel</li>
 *   <li>I13 Back then close drawer → Home</li>
 *   <li>I14 Chrome intact after photo</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Account interact — rental 9000000001")
public class AccountInteractTest extends AccountBaseTest {

    @Test(priority = 1, description = "AC-I1: Edit → Cancel restores Edit")
    @Severity(SeverityLevel.BLOCKER)
    public void editCancelRestores() {
        AccountPage page = reachAccount();
        page.tapEdit();
        sleepQuiet(900);
        assertThat(page.isEditModeVisible()).isTrue();
        page.tapCancel();
        sleepQuiet(700);
        assertThat(page.isEditVisible()).isTrue();
        assertThat(page.isEditModeVisible()).isFalse();
        assertThat(classifyRentalNow()).isEqualTo("account");
    }

    @Test(priority = 2, description = "AC-I2: Update → Cancel restores Update")
    @Severity(SeverityLevel.BLOCKER)
    public void updateCancelRestores() {
        AccountPage page = reachAccount();
        page.tapUpdate();
        sleepQuiet(900);
        assertThat(page.isCompanyUpdateModeVisible()).isTrue();
        page.tapCancel();
        sleepQuiet(700);
        assertThat(page.isUpdateVisible()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("account");
    }

    @Test(priority = 3, description = "AC-I3: Profile photo → Close")
    @Severity(SeverityLevel.CRITICAL)
    public void profilePhotoClose() {
        AccountPage page = reachAccount();
        page.tapProfilePhoto();
        sleepQuiet(1000);
        page.attachScreenshot("ac-i3-photo");
        assertThat(page.isPhotoCloseVisible()).isTrue();
        page.tapPhotoClose();
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isProfileInfoVisible()).isTrue();
    }

    @Test(priority = 4, description = "AC-I4: header Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void headerBackToDrawer() {
        AccountPage page = reachAccount();
        page.tapHeaderBack();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
        assertThat(profileDrawerNow()).isTrue();
    }

    @Test(priority = 5, description = "AC-I5: device Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBackToDrawer() {
        AccountPage page = reachAccount();
        page.pressDeviceBack();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 6, description = "AC-I6: reopen Account after Back")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterBack() {
        AccountPage page = reachAccount();
        page.tapHeaderBack();
        sleepQuiet(800);
        new ProfileDrawerPage().tapRow("Account");
        sleepQuiet(1000);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("account");
    }

    @Test(priority = 7, description = "AC-I7: Edit then device Back dismisses safely")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Back must not Save Changes.")
    public void editThenDeviceBackSafe() {
        AccountPage page = reachAccount();
        page.tapEdit();
        sleepQuiet(800);
        page.pressDeviceBack();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        page = ensureAccountReadMode(page);
        assertThat(classifyRentalNow()).isIn("account", "home-drawer", "home");
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 8, description = "AC-I8: Update then Cancel then Edit Cancel chain")
    @Severity(SeverityLevel.CRITICAL)
    public void updateThenEditCancelChain() {
        AccountPage page = reachAccount();
        page.tapUpdate();
        sleepQuiet(800);
        page.tapCancel();
        sleepQuiet(600);
        page.tapEdit();
        sleepQuiet(800);
        page.tapCancel();
        sleepQuiet(600);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isEditVisible()).isTrue();
        assertThat(page.isUpdateVisible()).isTrue();
    }

    @Test(priority = 9, description = "AC-I9: phone value stays 9000000001 after Cancel")
    @Severity(SeverityLevel.CRITICAL)
    public void phoneUnchangedAfterCancel() {
        AccountPage page = reachAccount();
        page.tapEdit();
        sleepQuiet(700);
        page.tapCancel();
        sleepQuiet(700);
        assertThat(page.isPhoneValueVisible()).isTrue();
        assertThat(page.isVendorNameVisible()).isTrue();
    }

    @Test(priority = 10, description = "AC-I10: photo Close then header Back → drawer")
    @Severity(SeverityLevel.NORMAL)
    public void photoThenBackToDrawer() {
        AccountPage page = reachAccount();
        page.tapProfilePhoto();
        sleepQuiet(900);
        if (page.isPhotoCloseVisible()) {
            page.tapPhotoClose();
            sleepQuiet(700);
        }
        page.tapHeaderBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 11, description = "AC-I11: double Edit Cancel idempotent")
    @Severity(SeverityLevel.NORMAL)
    public void doubleEditCancel() {
        AccountPage page = reachAccount();
        for (int i = 0; i < 2; i++) {
            page.tapEdit();
            sleepQuiet(700);
            page.tapCancel();
            sleepQuiet(600);
        }
        assertThat(page.isEditVisible()).isTrue();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 12, description = "AC-I12: Company Info still after Update Cancel")
    @Severity(SeverityLevel.NORMAL)
    public void companyInfoAfterUpdateCancel() {
        AccountPage page = reachAccount();
        page.tapUpdate();
        sleepQuiet(800);
        page.tapCancel();
        sleepQuiet(700);
        assertThat(page.isCompanyInfoVisible()).isTrue();
        assertThat(page.isCompanyNameLabelVisible()).isTrue();
        assertThat(page.isEmailLabelVisible()).isTrue();
    }

    @Test(priority = 13, description = "AC-I13: Back from Account then drawer close → Home")
    @Severity(SeverityLevel.CRITICAL)
    public void backThenCloseDrawerHome() {
        AccountPage page = reachAccount();
        page.tapHeaderBack();
        sleepQuiet(800);
        new ProfileDrawerPage().tapCloseNavigationMenu();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(classifyRentalNow()).isIn("home", "extend-time");
    }

    @Test(priority = 14, description = "AC-I14: read-mode chrome intact after photo")
    @Severity(SeverityLevel.NORMAL)
    public void chromeIntactAfterPhoto() {
        AccountPage page = reachAccount();
        page.tapProfilePhoto();
        sleepQuiet(900);
        if (page.isPhotoCloseVisible()) {
            page.tapPhotoClose();
            sleepQuiet(700);
        }
        assertThat(page.isProfileInfoVisible()).isTrue();
        assertThat(page.isEditVisible()).isTrue();
        assertThat(page.isUpdateVisible()).isTrue();
        assertThat(page.isPhoneValueVisible()).isTrue();
    }
}
