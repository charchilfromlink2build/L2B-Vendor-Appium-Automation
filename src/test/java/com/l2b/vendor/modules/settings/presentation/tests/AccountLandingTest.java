package com.l2b.vendor.modules.settings.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.settings.presentation.pages.AccountPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Account landing — Profile drawer → Account on {@code 9000000001}.
 * Dump {@code /tmp/l2b-account-0001-20260929}. Never Save Changes.
 *
 * <p><b>Happy Path (AC-L1–L12)</b>
 * <ol>
 *   <li>L1 Drawer Account → Profile Info</li>
 *   <li>L2 Account title + Back</li>
 *   <li>L3 Profile Info + Edit</li>
 *   <li>L4 Full Name + Mobile + values</li>
 *   <li>L5 Company Info + Update</li>
 *   <li>L6 Company Name + Email</li>
 *   <li>L7 Profile photo</li>
 *   <li>L8 Field label stack layout</li>
 *   <li>L9 Title left inset</li>
 *   <li>L10 Stay Vendor package</li>
 *   <li>L11 Edit mode chrome (Cancel only)</li>
 *   <li>L12 Update mode chrome (Cancel only)</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Account landing — rental 9000000001")
public class AccountLandingTest extends AccountBaseTest {

    @Test(priority = 1, description = "AC-L1: drawer Account → Profile Info")
    @Severity(SeverityLevel.BLOCKER)
    public void accountOpensProfileInfo() {
        AccountPage page = reachAccount();
        page.attachScreenshot("ac-l1");
        assertThat(classifyRentalNow()).isEqualTo("account");
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 2, description = "AC-L2: Account title + Back chrome")
    @Severity(SeverityLevel.CRITICAL)
    public void titleAndBackChrome() {
        AccountPage page = reachAccount();
        assertThat(page.isAccountTitleVisible()).isTrue();
        assertThat(page.isBackVisible()).isTrue();
        assertThat(page.headerBackAligned()).isTrue();
    }

    @Test(priority = 3, description = "AC-L3: Profile Info + Edit")
    @Severity(SeverityLevel.CRITICAL)
    public void profileInfoAndEdit() {
        AccountPage page = reachAccount();
        assertThat(page.isProfileInfoVisible()).isTrue();
        assertThat(page.isEditVisible()).isTrue();
        assertThat(page.profileInfoEditLayoutOk()).isTrue();
    }

    @Test(priority = 4, description = "AC-L4: Full Name + Mobile Number")
    @Severity(SeverityLevel.CRITICAL)
    public void fullNameAndMobile() {
        AccountPage page = reachAccount();
        assertThat(page.isDisplayedNow()).as("hard: Account open").isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isFullNameLabelVisible()).as("Full Name label").isTrue();
        softly.assertThat(page.isMobileLabelVisible()).as("Mobile Number label").isTrue();
        softly.assertThat(page.isVendorNameVisible()).as("vendor name value").isTrue();
        softly.assertThat(page.isPhoneValueVisible()).as("phone 9000000001").isTrue();
        softly.assertAll();
    }

    @Test(priority = 5, description = "AC-L5: Company Info + Update")
    @Severity(SeverityLevel.CRITICAL)
    public void companyInfoAndUpdate() {
        AccountPage page = reachAccount();
        assertThat(page.isCompanyInfoVisible()).isTrue();
        assertThat(page.isUpdateVisible()).isTrue();
        assertThat(page.companyInfoUpdateLayoutOk()).isTrue();
    }

    @Test(priority = 6, description = "AC-L6: Company Name + Email")
    @Severity(SeverityLevel.CRITICAL)
    public void companyNameAndEmail() {
        AccountPage page = reachAccount();
        assertThat(page.isCompanyNameLabelVisible()).isTrue();
        assertThat(page.isEmailLabelVisible()).isTrue();
        assertThat(page.isEmailValueVisible()).as("email value in EditText").isTrue();
        assertThat(page.isCompanyNameValueVisible()).as("company name value").isTrue();
    }

    @Test(priority = 7, description = "AC-L7: Profile photo affordance")
    @Severity(SeverityLevel.NORMAL)
    public void profilePhotoVisible() {
        AccountPage page = reachAccount();
        assertThat(page.isProfilePhotoVisible()).isTrue();
    }

    @Test(priority = 8, description = "AC-L8: field label stack layout")
    @Severity(SeverityLevel.CRITICAL)
    public void fieldLabelsLayout() {
        AccountPage page = reachAccount();
        assertThat(page.isDisplayedNow()).as("hard: Account open").isTrue();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.fieldLabelsLayoutOk()).as("Full Name→Mobile→Company→Email stack").isTrue();
        softly.assertThat(page.profileInfoEditLayoutOk()).as("Profile Info | Edit band").isTrue();
        softly.assertThat(page.companyInfoUpdateLayoutOk()).as("Company Info | Update band").isTrue();
        softly.assertAll();
    }

    @Test(priority = 9, description = "AC-L9: title left inset")
    @Severity(SeverityLevel.NORMAL)
    public void titleLeftInset() {
        AccountPage page = reachAccount();
        assertThat(page.titleLeftInsetOk()).isTrue();
    }

    @Test(priority = 10, description = "AC-L10: stay Vendor package")
    @Severity(SeverityLevel.BLOCKER)
    public void stayVendorPackage() {
        AccountPage page = reachAccount();
        assertThat(classifyRentalNow()).isEqualTo("account");
        assertThat(vendorPackage()).contains("l2b");
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 11, description = "AC-L11: Edit mode chrome (Cancel + Save Changes)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 04. Never tap Save Changes — Cancel only.")
    public void editModeChrome() {
        AccountPage page = reachAccount();
        page.tapEdit();
        sleepQuiet(900);
        page.attachScreenshot("ac-l11-edit");
        assertThat(page.isEditModeVisible()).isTrue();
        page.tapCancel();
        sleepQuiet(700);
        assertThat(page.isEditVisible()).isTrue();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 12, description = "AC-L12: Update mode chrome (Cancel + Save Changes)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 06. Never tap Save Changes — Cancel only.")
    public void updateModeChrome() {
        AccountPage page = reachAccount();
        page.tapUpdate();
        sleepQuiet(900);
        page.attachScreenshot("ac-l12-update");
        assertThat(page.isCompanyUpdateModeVisible()).isTrue();
        page.tapCancel();
        sleepQuiet(700);
        assertThat(page.isUpdateVisible()).isTrue();
        assertThat(page.isDisplayedNow()).isTrue();
    }
}
