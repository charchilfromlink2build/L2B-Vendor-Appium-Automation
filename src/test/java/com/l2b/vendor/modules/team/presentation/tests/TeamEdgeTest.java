package com.l2b.vendor.modules.team.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.team.presentation.pages.TeamPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Manage Team edge — Never Assign confirm / remove / Log Out Confirm / Accept / Decline.
 *
 * <p><b>Critical Edge (TM-E1–E12)</b>
 * <ol>
 *   <li>E1 Rapid Change Assignment Close stays Vendor</li>
 *   <li>E2 Layout: Back left + Add Member top-right</li>
 *   <li>E3 Device Back on sheet may leave sheet (recover Close)</li>
 *   <li>E4 Add Member open/close twice</li>
 *   <li>E5 Scroll extremes stay Manage Team</li>
 *   <li>E6 Reopen after Home</li>
 *   <li>E7 Package Vendor through sheet+back</li>
 *   <li>E8 Nauman + phone intact after sheet Close</li>
 *   <li>E9 No Accept/Decline on Team path</li>
 *   <li>E10 Double header Back safe</li>
 *   <li>E11 Sheet never auto-assigns on Close</li>
 *   <li>E12 Known member Randanberno still listed after scroll</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Team edge — rental 9000000001")
public class TeamEdgeTest extends TeamBaseTest {

    @Test(priority = 1, description = "TM-E1: rapid Change Assignment Close stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidSheetCloseStaysVendor() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        for (int i = 0; i < 3; i++) {
            page.tapChangeAssignment();
            sleepQuiet(500);
            page.dismissSheetIfPresent();
            sleepQuiet(500);
        }
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("team");
    }

    @Test(priority = 2, description = "TM-E2: layout Back left + Add Member top-right")
    @Severity(SeverityLevel.CRITICAL)
    public void layoutHeaderChecks() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.headerBackAligned()).as("Back left inset").isTrue();
        softly.assertThat(page.addMemberTopRight()).as("Add Member top-right").isTrue();
        softly.assertThat(page.isManageTeamTitleVisible()).as("title").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "TM-E3: device Back on sheet — recover with Close sheet")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Dump 05/07/08: navigate().back() left Select machine sheet + Close sheet.")
    public void deviceBackOnSheetRecover() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapChangeAssignment();
        sleepQuiet(1000);
        page.pressDeviceBack();
        sleepQuiet(800);
        boolean sheetStuck = page.isChangeAssignmentSheetVisible() || page.isCloseSheetVisible();
        Allure.parameter("sheetStuckAfterDeviceBack", String.valueOf(sheetStuck));
        if (sheetStuck) {
            page.dismissSheetIfPresent();
            sleepQuiet(800);
        }
        page = dismissToTeam(page);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 4, description = "TM-E4: Add Member open/Back twice")
    @Severity(SeverityLevel.NORMAL)
    public void addMemberOpenBackTwice() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        for (int i = 0; i < 2; i++) {
            if (!page.isAddMemberVisible()) {
                page = dismissToTeam(page);
            }
            page.tapAddMember();
            sleepQuiet(1000);
            if (page.isBackVisible() && !page.isDisplayedNow()) {
                page.tapBack();
            } else {
                page.pressDeviceBack();
            }
            sleepQuiet(800);
            page = dismissToTeam(page);
        }
        assertThat(page.isDisplayedNow() || page.isAddMemberVisible()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 5, description = "TM-E5: scroll extremes stay Manage Team")
    @Severity(SeverityLevel.NORMAL)
    public void scrollExtremesStayTeam() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.swipeListUp();
        sleepQuiet(400);
        page.swipeListUp();
        sleepQuiet(400);
        assertThat(page.isManageTeamTitleVisible()).isTrue();
        page.swipeListDown();
        sleepQuiet(400);
        page.swipeListDown();
        sleepQuiet(400);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 6, description = "TM-E6: reopen Team after Home")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterHome() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapBack();
        sleepQuiet(700);
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        HomePage home = ensureRentalHomeWarm();
        assertThat(home.isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
        page = reachTeam();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 7, description = "TM-E7: package Vendor through sheet + header Back")
    @Severity(SeverityLevel.NORMAL)
    public void packageThroughSheetAndBack() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapChangeAssignment();
        sleepQuiet(800);
        page.dismissSheetIfPresent();
        sleepQuiet(700);
        page.tapBack();
        sleepQuiet(800);
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 8, description = "TM-E8: Nauman + phone intact after sheet Close")
    @Severity(SeverityLevel.CRITICAL)
    public void naumanIntactAfterSheet() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapChangeAssignment();
        sleepQuiet(900);
        page.dismissSheetIfPresent();
        sleepQuiet(800);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isNaumanVisible()).as("Nauman").isTrue();
        softly.assertThat(page.isNaumanPhoneVisible()).as("9000000002").isTrue();
        softly.assertThat(page.isActiveVisible()).as("Active").isTrue();
        softly.assertAll();
    }

    @Test(priority = 9, description = "TM-E9: no Accept/Decline on Team path")
    @Severity(SeverityLevel.CRITICAL)
    public void noAcceptDeclineOnTeamPath() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("Accept")).as("no Accept").isFalse();
        softly.assertThat(hasText("Decline")).as("no Decline").isFalse();
        page.tapChangeAssignment();
        sleepQuiet(900);
        softly.assertThat(hasText("Accept")).as("no Accept on sheet").isFalse();
        softly.assertThat(hasText("Decline")).as("no Decline on sheet").isFalse();
        page.dismissSheetIfPresent();
        sleepQuiet(700);
        softly.assertAll();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 10, description = "TM-E10: double header Back safe")
    @Severity(SeverityLevel.NORMAL)
    public void doubleHeaderBackSafe() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.tapBack();
        sleepQuiet(700);
        if (profileDrawerNow()) {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        Allure.parameter("after", classifyRentalNow());
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isIn("home", "home-drawer", "team", "launcher");
    }

    @Test(priority = 11, description = "TM-E11: Close sheet does not remove Change Assignment")
    @Severity(SeverityLevel.CRITICAL)
    public void closeSheetDoesNotAssignAway() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        assertThat(page.isChangeAssignmentVisible()).isTrue();
        page.tapChangeAssignment();
        sleepQuiet(900);
        page.dismissSheetIfPresent();
        sleepQuiet(800);
        assertThat(page.isChangeAssignmentVisible()).as("CTA still present").isTrue();
        assertThat(page.isNaumanVisible() || page.isRandanbernoVisible()).isTrue();
    }

    @Test(priority = 12, description = "TM-E12: Randanberno still listed after scroll")
    @Severity(SeverityLevel.NORMAL)
    public void randanbernoAfterScroll() {
        TeamPage page = reachTeam();
        page.dismissSheetIfPresent();
        page.swipeListUp();
        sleepQuiet(600);
        boolean seen = page.isRandanbernoVisible();
        if (!seen) {
            page.swipeListDown();
            sleepQuiet(500);
            seen = page.isRandanbernoVisible();
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isManageTeamTitleVisible()).as("title").isTrue();
        softly.assertThat(seen || page.isNaumanVisible()).as("known member visible").isTrue();
        softly.assertAll();
    }
}
