package com.l2b.vendor.modules.team.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import com.l2b.vendor.modules.team.presentation.pages.TeamPage;
import io.appium.java_client.android.AndroidDriver;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Manage Team interact — dump {@code /tmp/l2b-team-0001-20261002}.
 * Never confirm Assign / remove / Log Out. Change Assignment → Close sheet only.
 * Add Member → Back only (no submit).
 *
 * <p><b>Happy Path (TM-I1–I12)</b>
 * <ol>
 *   <li>I1 Change Assignment → Select machine sheet</li>
 *   <li>I2 Close sheet → Manage Team</li>
 *   <li>I3 Add Member open → Back (never submit)</li>
 *   <li>I4 Header Back → drawer</li>
 *   <li>I5 Device Back → drawer</li>
 *   <li>I6 Reopen Manage team</li>
 *   <li>I7 Scroll list still shows members</li>
 *   <li>I8 Change Assignment then device Back (Close sheet if stuck)</li>
 *   <li>I9 Sheet machine chrome visible (Capacity/Operator)</li>
 *   <li>I10 Second Change Assignment Close</li>
 *   <li>I11 Back then close drawer → Home</li>
 *   <li>I12 Stay Vendor package after sheet</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Team interact — rental 9000000001")
public class TeamInteractTest extends TeamBaseTest {

    @Test(priority = 1, description = "TM-I1: Change Assignment → Select machine sheet")
    @Severity(SeverityLevel.BLOCKER)
    public void changeAssignmentOpensSheet() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        sleepQuiet(400);
        page.tapChangeAssignment();
        sleepQuiet(1100);
        page.attachScreenshot("tm-i1-sheet");
        assertThat(page.isChangeAssignmentSheetVisible()).as("Select machine sheet").isTrue();
        Allure.parameter("after", classifyRentalNow());
        page.dismissSheetIfPresent();
        sleepQuiet(800);
        assertThat(page.isDisplayedNow() || !page.isChangeAssignmentSheetVisible())
                .as("sheet dismissed").isTrue();
    }

    @Test(priority = 2, description = "TM-I2: Close sheet restores Manage Team")
    @Severity(SeverityLevel.BLOCKER)
    public void closeSheetRestoresTeam() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapChangeAssignment();
        sleepQuiet(1000);
        assertThat(page.isChangeAssignmentSheetVisible()).as("sheet open").isTrue();
        page.dismissSheetIfPresent();
        sleepQuiet(900);
        assertThat(page.isDisplayedNow()).as("Manage Team").isTrue();
        assertThat(page.isChangeAssignmentSheetVisible()).as("sheet gone").isFalse();
        assertThat(classifyRentalNow()).isEqualTo("team");
    }

    @Test(priority = 3, description = "TM-I3: Add Member → Back only (never submit)")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 06 was missing when sheet blocked CTA; re-probe with sheet closed.")
    public void addMemberBackOnly() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        sleepQuiet(400);
        assertThat(page.isAddMemberVisible()).isTrue();
        page.tapAddMember();
        sleepQuiet(1200);
        page.attachScreenshot("tm-i3-add-member");
        Allure.parameter("afterAdd", classifyRentalNow());
        // Prefer header/device Back — never confirm invite/submit.
        if (page.isBackVisible()) {
            page.tapBack();
        } else {
            page.pressDeviceBack();
        }
        sleepQuiet(900);
        page = dismissToTeam(page);
        assertThat(page.isDisplayedNow() || page.isAddMemberVisible()
                || classifyRentalNow().equals("team")
                || classifyRentalNow().equals("home-drawer"))
                .as("safe after Add Member Back").isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 4, description = "TM-I4: header Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void headerBackToDrawer() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapBack();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
        assertThat(profileDrawerNow()).isTrue();
    }

    @Test(priority = 5, description = "TM-I5: device Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBackToDrawer() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.pressDeviceBack();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 6, description = "TM-I6: reopen Manage team after Back")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterBack() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapBack();
        sleepQuiet(800);
        new ProfileDrawerPage().tapRow("Manage team");
        sleepQuiet(1100);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("team");
    }

    @Test(priority = 7, description = "TM-I7: scroll list still shows members / chrome")
    @Severity(SeverityLevel.NORMAL)
    public void scrollListKeepsChrome() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.swipeListUp();
        sleepQuiet(600);
        page.attachScreenshot("tm-i7-scrolled");
        assertThat(page.isManageTeamTitleVisible()).isTrue();
        assertThat(page.isAddMemberVisible()).isTrue();
        assertThat(page.hasActiveMember() || page.isRandanbernoVisible() || page.isNaumanVisible())
                .as("member still present after scroll").isTrue();
        page.swipeListDown();
        sleepQuiet(500);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 8, description = "TM-I8: Change Assignment then device Back; Close sheet if stuck")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump showed device Back may leave Select machine sheet open — Close sheet recovers.")
    public void changeAssignmentDeviceBackSafe() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapChangeAssignment();
        sleepQuiet(1000);
        page.pressDeviceBack();
        sleepQuiet(900);
        Allure.parameter("afterDeviceBack", classifyRentalNow());
        if (page.isChangeAssignmentSheetVisible() || page.isCloseSheetVisible()) {
            Allure.parameter("deviceBackLeftSheet", "true");
            page.dismissSheetIfPresent();
            sleepQuiet(800);
        }
        page = dismissToTeam(page);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 9, description = "TM-I9: sheet shows Select machine + Capacity/Operator")
    @Severity(SeverityLevel.NORMAL)
    public void sheetMachineChrome() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapChangeAssignment();
        sleepQuiet(1200);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isChangeAssignmentSheetVisible()).as("Select machine sheet").isTrue();
        softly.assertThat(hasText("Select machine for operator/driver")).as("sheet title").isTrue();
        softly.assertThat(hasText("Operator") || hasText("Machine Type") || hasText("Capacity"))
                .as("sheet machine chrome").isTrue();
        softly.assertAll();
        page.dismissSheetIfPresent();
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 10, description = "TM-I10: second Change Assignment Close idempotent")
    @Severity(SeverityLevel.NORMAL)
    public void secondChangeAssignmentClose() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        for (int i = 0; i < 2; i++) {
            page.tapChangeAssignment();
            sleepQuiet(900);
            page.dismissSheetIfPresent();
            sleepQuiet(700);
        }
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.isChangeAssignmentVisible()).isTrue();
    }

    @Test(priority = 11, description = "TM-I11: Back then close drawer → Home")
    @Severity(SeverityLevel.CRITICAL)
    public void backThenCloseDrawerHome() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapBack();
        sleepQuiet(800);
        assertThat(profileDrawerNow()).isTrue();
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        Allure.parameter("after", classifyRentalNow());
        assertThat(new HomePage().isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
    }

    @Test(priority = 12, description = "TM-I12: stay Vendor package after sheet cycle")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendorAfterSheet() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapChangeAssignment();
        sleepQuiet(900);
        page.dismissSheetIfPresent();
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(((AndroidDriver) DriverManager.get()).getCurrentPackage())
                .isEqualTo("com.l2b.app.qa");
    }
}
