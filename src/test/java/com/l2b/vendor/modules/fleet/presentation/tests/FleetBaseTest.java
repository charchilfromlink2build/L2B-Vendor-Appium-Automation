package com.l2b.vendor.modules.fleet.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.fleet.presentation.pages.FleetPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

/**
 * Shared 9000000001 path onto Fleet. Close QB only. Never Accept / Decline /
 * Log Out / Add Machine submit / operator confirm.
 */
public abstract class FleetBaseTest extends RentalHomeBaseTest {

    @Step("Reach Fleet via bottom Fleet tab")
    protected FleetPage reachFleetViaTab() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc("Fleet");
        FleetPage fleet = new FleetPage();
        fleet.waitUntilLoaded();
        Allure.parameter("entry", "bottom-tab");
        Allure.parameter("after", classifyRentalNow());
        return fleet;
    }

    @Step("Reach Fleet via Home Active Fleet See all")
    protected FleetPage reachFleetViaActiveFleetSeeAll() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapNthSeeAll(0);
        FleetPage fleet = new FleetPage();
        fleet.waitUntilLoaded();
        Allure.parameter("entry", "active-fleet-see-all");
        Allure.parameter("after", classifyRentalNow());
        return fleet;
    }

    @Step("Reach Fleet via Profile drawer Your machines")
    protected FleetPage reachFleetViaDrawerYourMachines() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc("Profile");
        assertThat(profileDrawerNow()).as("Profile must open drawer").isTrue();
        home.tapText("Your machines");
        FleetPage fleet = new FleetPage();
        fleet.waitUntilLoaded();
        Allure.parameter("entry", "drawer-your-machines");
        Allure.parameter("after", classifyRentalNow());
        return fleet;
    }
}
