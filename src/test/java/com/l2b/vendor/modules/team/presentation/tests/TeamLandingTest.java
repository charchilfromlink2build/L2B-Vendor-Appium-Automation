package com.l2b.vendor.modules.team.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.team.presentation.pages.TeamPage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Manage Team landing — Profile drawer → Manage team on {@code 9000000001}.
 * Dump {@code /tmp/l2b-team-0001-20261002}. Never confirm Assign / remove.
 *
 * <p><b>Happy Path (TM-L1–L10)</b>
 * <ol>
 *   <li>L1 Drawer Manage team → Manage Team</li>
 *   <li>L2 Manage Team title + Back</li>
 *   <li>L3 Your team member heading</li>
 *   <li>L4 Active member chrome</li>
 *   <li>L5 Known member Nauman + phone</li>
 *   <li>L6 Change Assignment visible</li>
 *   <li>L7 Machine / Capacity / Skills</li>
 *   <li>L8 Experience + Primary</li>
 *   <li>L9 Add Member CTA</li>
 *   <li>L10 Stay Vendor package</li>
 * </ol>
 */
@Epic("Vendor app")
@Feature("Team landing — rental 9000000001")
public class TeamLandingTest extends TeamBaseTest {

    @Test(priority = 1, description = "TM-L1: drawer Manage team → Manage Team")
    @Severity(SeverityLevel.BLOCKER)
    public void teamOpensManageTeam() {
        TeamPage page = reachTeam();
        page.attachScreenshot("tm-l1");
        assertThat(page.isDisplayedNow()).as("Manage Team open").isTrue();
    }

    @Test(priority = 2, description = "TM-L2: Manage Team title + Back")
    @Severity(SeverityLevel.CRITICAL)
    public void titleAndBackChrome() {
        TeamPage page = reachTeam();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isManageTeamTitleVisible()).as("Manage Team title").isTrue();
        softly.assertThat(page.isBackVisible()).as("Back").isTrue();
        softly.assertThat(page.headerBackAligned()).as("Back left inset").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "TM-L3: Your team member heading")
    @Severity(SeverityLevel.CRITICAL)
    public void yourTeamMemberHeading() {
        TeamPage page = reachTeam();
        assertThat(page.isYourTeamMemberVisible()).as("Your team member").isTrue();
    }

    @Test(priority = 4, description = "TM-L4: Active status on member card")
    @Severity(SeverityLevel.CRITICAL)
    public void activeMemberChrome() {
        TeamPage page = reachTeam();
        assertThat(page.isActiveVisible()).as("Active").isTrue();
    }

    @Test(priority = 5, description = "TM-L5: Nauman Majid Pathan + 9000000002")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Seeded operator on rental company 9000000001.")
    public void knownMemberNauman() {
        TeamPage page = reachTeam();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isNaumanVisible()).as("Nauman name").isTrue();
        softly.assertThat(page.isNaumanPhoneVisible()).as("phone 9000000002").isTrue();
        softly.assertAll();
    }

    @Test(priority = 6, description = "TM-L6: Change Assignment visible")
    @Severity(SeverityLevel.CRITICAL)
    public void changeAssignmentVisible() {
        TeamPage page = reachTeam();
        assertThat(page.isChangeAssignmentVisible()).as("Change Assignment").isTrue();
    }

    @Test(priority = 7, description = "TM-L7: Machine / Capacity / Skills labels")
    @Severity(SeverityLevel.NORMAL)
    public void machineCapacitySkills() {
        TeamPage page = reachTeam();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isMachineLabelVisible()).as("Machine").isTrue();
        softly.assertThat(page.isCapacityLabelVisible()).as("Capacity").isTrue();
        softly.assertThat(page.isSkillsVisible()).as("Skills").isTrue();
        softly.assertAll();
    }

    @Test(priority = 8, description = "TM-L8: Experience + Primary markers")
    @Severity(SeverityLevel.NORMAL)
    public void experienceAndPrimary() {
        TeamPage page = reachTeam();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isExperienceVisible()).as("Experience").isTrue();
        softly.assertThat(page.isPrimaryVisible()).as("Primary").isTrue();
        softly.assertAll();
    }

    @Test(priority = 9, description = "TM-L9: Add Member CTA")
    @Severity(SeverityLevel.CRITICAL)
    public void addMemberCta() {
        TeamPage page = reachTeam();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isAddMemberVisible()).as("Add Member").isTrue();
        softly.assertThat(page.addMemberTopRight()).as("Add Member top-right app bar").isTrue();
        softly.assertAll();
    }

    @Test(priority = 10, description = "TM-L10: stay on Vendor package")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendorPackage() {
        TeamPage page = reachTeam();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(((io.appium.java_client.android.AndroidDriver)
                com.l2b.vendor.core.driver.DriverManager.get()).getCurrentPackage())
                .isEqualTo("com.l2b.app.qa");
    }
}
