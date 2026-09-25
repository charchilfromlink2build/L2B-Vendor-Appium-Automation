package com.l2b.vendor.modules.fleet.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.environment.Config;
import com.l2b.vendor.modules.fleet.presentation.pages.FleetPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Allure;
import org.testng.annotations.Test;

/**
 * Fleet landing — read-only chrome on {@code 9000000001}. Dump
 * {@code /tmp/l2b-fleet-full-0001-20260925} (03-fleet-list). Do not submit
 * Add Machine, confirm operator, Delete, or Disable.
 */
@Epic("Vendor app")
@Feature("Fleet landing — rental 9000000001")
public class FleetLandingTest extends FleetBaseTest {

    @Test(priority = 1, description = "FL-L1: bottom Fleet tab opens Your Fleet; bottom bar stays")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Dump 03-fleet-list: content-desc Fleet from Home → texts Your Fleet + Add Machine; "
            + "descs Back + Calendar·Home·Earning·Fleet. classifyRentalNow=fleet.")
    public void bottomTabOpensFleet() {
        FleetPage fleet = reachFleetViaTab();
        fleet.attachScreenshot("fleet-fl-l1-tab");
        Allure.parameter("tabsStay", String.valueOf(fleet.areBottomTabsVisible()));
        Allure.parameter("yourFleet", String.valueOf(fleet.isYourFleetTitleVisible()));
        Allure.parameter("addMachine", String.valueOf(fleet.isAddMachineVisible()));
        assertThat(vendorPackage()).isEqualTo(Config.get("app.package"));
        assertThat(fleet.isYourFleetTitleVisible()).as("Dump title Your Fleet").isTrue();
        assertThat(fleet.isAddMachineVisible()).as("Dump CTA Add Machine").isTrue();
        assertThat(fleet.areBottomTabsVisible()).as("Bottom bar stays on Fleet tab").isTrue();
        assertThat(classifyRentalNow()).isEqualTo("fleet");
    }

    @Test(priority = 2, description = "FL-L2: title Your Fleet + Add Machine chrome")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Fleet landing shows Your Fleet title and Add Machine CTA. "
            + "Dump 25 Sep texts: Your Fleet, Add Machine.")
    public void titleAndAddMachine() {
        FleetPage fleet = reachFleetViaTab();
        fleet.attachScreenshot("fleet-fl-l2-chrome");
        Allure.parameter("addMachine", String.valueOf(fleet.isAddMachineVisible()));
        assertThat(fleet.isDisplayedNow()).isTrue();
        assertThat(fleet.isAddMachineVisible()).as("Add Machine CTA").isTrue();
        assertThat(fleet.isEmptyStateVisible())
                .as("0001 has machines — empty state must not show")
                .isFalse();
    }

    @Test(priority = 3, description = "FL-L3: Active Fleet See all opens the same Fleet list")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Home Active Fleet See all (index 0) lands on the same Your Fleet surface "
            + "as the bottom tab. Dump 25 Sep 02-active-fleet-see-all.")
    public void activeFleetSeeAllOpensFleet() {
        FleetPage fleet = reachFleetViaActiveFleetSeeAll();
        fleet.attachScreenshot("fleet-fl-l3-see-all");
        assertThat(classifyRentalNow()).isEqualTo("fleet");
        assertThat(fleet.isDisplayedNow()).isTrue();
        assertThat(fleet.areBottomTabsVisible()).isTrue();
    }

    @Test(priority = 4, description = "FL-L4: drawer Your machines opens the same Fleet list")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Profile → Your machines reaches Your Fleet. Dump 25 Sep 04-drawer-your-machines.")
    public void drawerYourMachinesOpensFleet() {
        FleetPage fleet = reachFleetViaDrawerYourMachines();
        fleet.attachScreenshot("fleet-fl-l4-drawer");
        assertThat(classifyRentalNow()).isEqualTo("fleet");
        assertThat(fleet.isDisplayedNow()).isTrue();
    }

    @Test(priority = 5, description = "FL-L5: list shows Active machines with fleet plates")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Live 0001 dump: KA13Z2117, KA02MH2612, MH12SD4444 + Tata Ace; Status Active.")
    public void activeMachinesWithPlates() {
        FleetPage fleet = reachFleetViaTab();
        int plates = fleet.visiblePlateCount();
        Allure.parameter("plates", String.valueOf(plates));
        Allure.parameter("activeStatus", String.valueOf(fleet.hasActiveStatus()));
        fleet.attachScreenshot("fleet-fl-l5-plates");
        assertThat(fleet.hasActiveStatus()).as("Status Active on list").isTrue();
        assertThat(plates).as("Dump had ≥3 KA/MH plates").isGreaterThanOrEqualTo(3);
    }

    @Test(priority = 6, description = "FL-L6: Home Active Fleet count matches Fleet list plates")
    @Severity(SeverityLevel.NORMAL)
    @Description("Home Active Fleet 4/20 (25 Sep dump). Fleet list should expose the same "
            + "card count order-of-magnitude (plates ≥ home numerator when assigned).")
    public void homeActiveFleetCountAligns() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        String homeSrc = DriverManager.get().getPageSource();
        boolean homeShowsFour = homeSrc.contains(">4<") || homeSrc.contains("text=\"4\"");
        Allure.parameter("homeShowsFourHeuristic", String.valueOf(homeShowsFour));
        home.tapDesc("Fleet");
        FleetPage fleet = new FleetPage();
        fleet.waitUntilLoaded();
        int plates = fleet.visiblePlateCount();
        Allure.parameter("plates", String.valueOf(plates));
        fleet.attachScreenshot("fleet-fl-l6-count");
        assertThat(plates).as("Fleet list must show machines when Home Active Fleet is non-zero")
                .isGreaterThanOrEqualTo(1);
        if (homeShowsFour) {
            assertThat(plates).as("Home showed 4 — expect ≥3 plates on Fleet")
                    .isGreaterThanOrEqualTo(3);
        }
    }

    @Test(priority = 7, description = "FL-L7: Change / Select operator affordance on cards")
    @Severity(SeverityLevel.NORMAL)
    @Description("Assigned cards show Change <name>; unassigned show Select. "
            + "Dump: Change Randanberno Ezung, Change Nauman…, Select.")
    public void operatorChangeOrSelectVisible() {
        FleetPage fleet = reachFleetViaTab();
        boolean change = fleet.hasChangeOperator();
        boolean select = DriverManager.get().getPageSource().contains(">Select<")
                || DriverManager.get().getPageSource().contains("text=\"Select\"");
        Allure.parameter("change", String.valueOf(change));
        Allure.parameter("select", String.valueOf(select));
        fleet.attachScreenshot("fleet-fl-l7-operator");
        assertThat(change || select)
                .as("At least one Change… or Select operator control")
                .isTrue();
    }

    @Test(priority = 8, description = "FL-L8: list can show Under Review status")
    @Severity(SeverityLevel.NORMAL)
    @Description("Live 25 Sep: fleet may include Under Review (e.g. newly submitted machine). "
            + "Record presence; Active still required on 0001.")
    public void underReviewStatusWhenPresent() {
        FleetPage fleet = reachFleetViaTab();
        boolean under = fleet.hasUnderReviewStatus();
        Allure.parameter("underReview", String.valueOf(under));
        fleet.attachScreenshot("fleet-fl-l8-under-review");
        assertThat(fleet.hasActiveStatus()).as("Active machines still on 0001").isTrue();
        assertThat(fleet.isDisplayedNow()).isTrue();
    }

    @Test(priority = 9, description = "FL-L9: swipe list keeps Your Fleet + bottom tabs")
    @Severity(SeverityLevel.NORMAL)
    @Description("Dump 04-fleet-scrolled: swipe up on list — title and Calendar·Home·Earning·Fleet stay.")
    public void swipeListKeepsChrome() {
        FleetPage fleet = reachFleetViaTab();
        fleet.swipeFleetListUp();
        fleet.attachScreenshot("fleet-fl-l9-scrolled");
        assertThat(fleet.isYourFleetTitleVisible() || fleet.isDisplayedNow()).isTrue();
        assertThat(fleet.areBottomTabsVisible()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("fleet");
    }
}
