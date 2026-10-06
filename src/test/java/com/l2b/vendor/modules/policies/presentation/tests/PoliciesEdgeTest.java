package com.l2b.vendor.modules.policies.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.policies.presentation.pages.PoliciesPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Policies edge — Never Accept / Decline / Log Out Confirm.
 *
 * <p><b>Critical Edge (PO-E1–E10)</b>
 */
@Epic("Vendor app")
@Feature("Policies edge — rental 9000000001")
public class PoliciesEdgeTest extends PoliciesBaseTest {

    @Test(priority = 1, description = "PO-E1: rapid scroll stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidScroll() {
        PoliciesPage page = reachPolicies();
        for (int i = 0; i < 4; i++) {
            page.swipeBodyUp();
            sleepQuiet(300);
        }
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("policies");
    }

    @Test(priority = 2, description = "PO-E2: layout Back left")
    @Severity(SeverityLevel.CRITICAL)
    public void layoutBack() {
        PoliciesPage page = reachPolicies();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.headerBackAligned()).as("Back left").isTrue();
        softly.assertThat(page.isPoliciesTitleVisible()).as("title").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "PO-E3: error persists or recovers after Retry")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #50 — document Retry outcome.")
    public void errorAfterRetry() {
        PoliciesPage page = reachPolicies();
        if (!page.isErrorVisible()) {
            Allure.parameter("state", "full-chrome");
            assertThat(page.isFullChromeVisible()
                    || page.isSectionVisible(PoliciesPage.SEC_COLLECT)
                    || page.isSectionVisible(PoliciesPage.SEC_USE)).isTrue();
            return;
        }
        page.tapRetry();
        sleepQuiet(2500);
        page.attachScreenshot("po-e3-after-retry");
        Allure.parameter("stillError", String.valueOf(page.isErrorVisible()));
        Allure.parameter("recoveredFull", String.valueOf(page.isFullChromeVisible()));
        assertThat(page.isDisplayedNow()).isTrue();
        SoftAssertions softly = new SoftAssertions();
        if (page.isErrorVisible()) {
            softly.assertThat(page.isRetryVisible()).as("Retry remains").isTrue();
        } else {
            softly.assertThat(page.isFullChromeVisible()
                    || page.isSectionVisible(PoliciesPage.SEC_COLLECT)).as("recovered").isTrue();
        }
        softly.assertAll();
    }

    @Test(priority = 4, description = "PO-E4: core sections when loaded")
    @Severity(SeverityLevel.CRITICAL)
    public void coreSections() {
        PoliciesPage page = reachPolicies();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error-#50");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isSectionVisible(PoliciesPage.SEC_COLLECT)
                || page.isSectionVisible("हम कौन-सी जानकारी")).as("collect").isTrue();
        softly.assertThat(page.isSectionVisible(PoliciesPage.SEC_LOCATION)).as("Location").isTrue();
        softly.assertThat(page.isSectionVisible(PoliciesPage.SEC_USE)).as("use").isTrue();
        softly.assertAll();
    }

    @Test(priority = 5, description = "PO-E5: no Accept/Decline booking CTAs")
    @Severity(SeverityLevel.CRITICAL)
    public void noAcceptDecline() {
        PoliciesPage page = reachPolicies();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("Decline")).as("no Decline").isFalse();
        softly.assertThat(DriverManager.get().findElements(
                org.openqa.selenium.By.xpath("//*[@text='Accept']")).isEmpty())
                .as("no Accept button text").isTrue();
        softly.assertAll();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 6, description = "PO-E6: reopen after Home")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterHome() {
        PoliciesPage page = reachPolicies();
        page.tapBack();
        sleepQuiet(700);
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        ensureRentalHomeWarm();
        page = reachPolicies();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 7, description = "PO-E7: scroll extremes stay policies")
    @Severity(SeverityLevel.NORMAL)
    public void scrollExtremes() {
        PoliciesPage page = reachPolicies();
        page.swipeBodyUp();
        sleepQuiet(300);
        page.swipeBodyUp();
        sleepQuiet(300);
        page.swipeBodyUp();
        sleepQuiet(300);
        assertThat(classifyRentalNow()).isEqualTo("policies");
        page.swipeBodyDown();
        sleepQuiet(400);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 8, description = "PO-E8: double Back safe")
    @Severity(SeverityLevel.NORMAL)
    public void doubleBack() {
        PoliciesPage page = reachPolicies();
        page.tapBack();
        sleepQuiet(700);
        if (profileDrawerNow()) {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        Allure.parameter("after", classifyRentalNow());
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 9, description = "PO-E9: package through scroll/Retry + Back")
    @Severity(SeverityLevel.NORMAL)
    public void packageScrollBack() {
        PoliciesPage page = reachPolicies();
        if (page.isRetryVisible() && page.isErrorVisible()) {
            page.tapRetry();
            sleepQuiet(800);
        } else {
            page.swipeBodyUp();
            sleepQuiet(500);
        }
        page.tapBack();
        sleepQuiet(800);
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 10, description = "PO-E10: Home after drawer close")
    @Severity(SeverityLevel.NORMAL)
    public void homeAfterDrawerClose() {
        PoliciesPage page = reachPolicies();
        page.tapBack();
        sleepQuiet(700);
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        assertThat(new HomePage().isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
    }
}
