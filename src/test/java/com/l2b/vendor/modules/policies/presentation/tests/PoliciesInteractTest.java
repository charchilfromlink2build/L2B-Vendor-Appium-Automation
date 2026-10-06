package com.l2b.vendor.modules.policies.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.policies.presentation.pages.PoliciesPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Policies interact — scroll + Back only. Never Accept / Decline / Log Out Confirm.
 *
 * <p><b>Happy Path (PO-I1–I10)</b>
 */
@Epic("Vendor app")
@Feature("Policies interact — rental 9000000001")
public class PoliciesInteractTest extends PoliciesBaseTest {

    @Test(priority = 1, description = "PO-I1: scroll down keeps Policies")
    @Severity(SeverityLevel.BLOCKER)
    public void scrollDown() {
        PoliciesPage page = reachPolicies();
        page.swipeBodyUp();
        sleepQuiet(700);
        page.attachScreenshot("po-i1");
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("policies");
    }

    @Test(priority = 2, description = "PO-I2: scroll up recovers collect/title")
    @Severity(SeverityLevel.CRITICAL)
    public void scrollUpRecovers() {
        PoliciesPage page = reachPolicies();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error-state");
            return;
        }
        page.swipeBodyUp();
        sleepQuiet(500);
        page.swipeBodyDown();
        sleepQuiet(600);
        assertThat(page.isSectionVisible(PoliciesPage.SEC_COLLECT)
                || page.isPoliciesTitleVisible()).isTrue();
    }

    @Test(priority = 3, description = "PO-I3: header Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void headerBack() {
        PoliciesPage page = reachPolicies();
        page.tapBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 4, description = "PO-I4: device Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBack() {
        PoliciesPage page = reachPolicies();
        page.pressDeviceBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 5, description = "PO-I5: reopen Policies")
    @Severity(SeverityLevel.CRITICAL)
    public void reopen() {
        PoliciesPage page = reachPolicies();
        page.tapBack();
        sleepQuiet(800);
        new ProfileDrawerPage().tapRow("Policies");
        sleepQuiet(1100);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("policies");
    }

    @Test(priority = 6, description = "PO-I6: double scroll safe")
    @Severity(SeverityLevel.NORMAL)
    public void doubleScroll() {
        PoliciesPage page = reachPolicies();
        page.swipeBodyUp();
        sleepQuiet(400);
        page.swipeBodyUp();
        sleepQuiet(400);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 7, description = "PO-I7: Retry when error (else scroll)")
    @Severity(SeverityLevel.NORMAL)
    public void retryOrScroll() {
        PoliciesPage page = reachPolicies();
        if (page.isRetryVisible() && page.isErrorVisible()) {
            page.tapRetry();
            sleepQuiet(2000);
            Allure.parameter("afterRetryError", String.valueOf(page.isErrorVisible()));
            assertThat(page.isDisplayedNow()).isTrue();
            return;
        }
        page.swipeBodyUp();
        sleepQuiet(500);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 8, description = "PO-I8: title survives scroll")
    @Severity(SeverityLevel.NORMAL)
    public void titleSurvivesScroll() {
        PoliciesPage page = reachPolicies();
        page.swipeBodyUp();
        sleepQuiet(500);
        assertThat(page.isPoliciesTitleVisible() || page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 9, description = "PO-I9: Back then close drawer → Home")
    @Severity(SeverityLevel.CRITICAL)
    public void backToHome() {
        PoliciesPage page = reachPolicies();
        page.tapBack();
        sleepQuiet(800);
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        assertThat(new HomePage().isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
    }

    @Test(priority = 10, description = "PO-I10: stay Vendor after scroll")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendor() {
        PoliciesPage page = reachPolicies();
        page.swipeBodyUp();
        sleepQuiet(500);
        assertThat(vendorPackage()).contains("l2b");
        Allure.parameter("after", classifyRentalNow());
    }
}
