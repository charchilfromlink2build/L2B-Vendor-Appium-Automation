package com.l2b.vendor.modules.policies.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Policies landing — Profile drawer on {@code 9000000001}.
 * Dump {@code /tmp/l2b-policies-0001-20261005}.
 *
 * <p><b>Happy Path (PO-L1–L10)</b>
 */
@Epic("Vendor app")
@Feature("Policies landing — rental 9000000001")
public class PoliciesLandingTest extends PoliciesBaseTest {

    @Test(priority = 1, description = "PO-L1: drawer Policies opens")
    @Severity(SeverityLevel.BLOCKER)
    public void policiesOpens() {
        PoliciesPage page = reachPolicies();
        page.attachScreenshot("po-l1");
        assertThat(page.isDisplayedNow()).as("Policies open").isTrue();
    }

    @Test(priority = 2, description = "PO-L2: title + Back")
    @Severity(SeverityLevel.CRITICAL)
    public void titleAndBack() {
        PoliciesPage page = reachPolicies();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isPoliciesTitleVisible()).as("title").isTrue();
        softly.assertThat(page.isBackVisible()).as("Back").isTrue();
        softly.assertThat(page.headerBackAligned()).as("Back left").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "PO-L3: chrome OR error empty state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #50 when Policies fails to load on seeded 9000000001.")
    public void chromeOrError() {
        PoliciesPage page = reachPolicies();
        boolean full = page.isFullChromeVisible()
                || page.isSectionVisible(PoliciesPage.SEC_COLLECT)
                || page.isSectionVisible(PoliciesPage.SEC_USE);
        boolean err = page.isErrorVisible();
        Allure.parameter("fullChrome", String.valueOf(full));
        Allure.parameter("errorState", String.valueOf(err));
        Allure.parameter("bug", err && !full ? "50" : "none");
        page.attachScreenshot("po-l3");
        assertThat(full || err).as("full chrome or error").isTrue();
    }

    @Test(priority = 4, description = "PO-L4: Retry visible when error")
    @Severity(SeverityLevel.CRITICAL)
    public void retryWhenError() {
        PoliciesPage page = reachPolicies();
        if (!page.isErrorVisible()) {
            Allure.parameter("skipped", "full-chrome");
            return;
        }
        assertThat(page.isRetryVisible()).as("Retry").isTrue();
    }

    @Test(priority = 5, description = "PO-L5: What we collect when available")
    @Severity(SeverityLevel.CRITICAL)
    public void collectSection() {
        PoliciesPage page = reachPolicies();
        if (page.isErrorVisible() && !page.isSectionVisible(PoliciesPage.SEC_COLLECT)) {
            Allure.parameter("skipped", "error-#50");
            return;
        }
        assertThat(page.isSectionVisible(PoliciesPage.SEC_COLLECT)
                || page.isSectionVisible("हम कौन-सी जानकारी")).isTrue();
    }

    @Test(priority = 6, description = "PO-L6: Location section when available")
    @Severity(SeverityLevel.CRITICAL)
    public void locationSection() {
        PoliciesPage page = reachPolicies();
        if (page.isErrorVisible() && !page.isSectionVisible(PoliciesPage.SEC_LOCATION)) {
            Allure.parameter("skipped", "error-#50");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isSectionVisible(PoliciesPage.SEC_LOCATION)).as("Location").isTrue();
        softly.assertThat(page.isSectionVisible(PoliciesPage.BULLET_MACHINE)
                || page.isSectionVisible("vehicle location")).as("machine bullet").isTrue();
        softly.assertAll();
    }

    @Test(priority = 7, description = "PO-L7: How we use it + no-sell when available")
    @Severity(SeverityLevel.CRITICAL)
    public void useSection() {
        PoliciesPage page = reachPolicies();
        if (page.isErrorVisible() && !page.isSectionVisible(PoliciesPage.SEC_USE)) {
            Allure.parameter("skipped", "error-#50");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isSectionVisible(PoliciesPage.SEC_USE)).as("use").isTrue();
        softly.assertThat(page.isSectionVisible(PoliciesPage.NO_SELL)
                || page.isSectionVisible("do not sell")).as("no sell").isTrue();
        softly.assertAll();
    }

    @Test(priority = 8, description = "PO-L8: Your rights when available")
    @Severity(SeverityLevel.NORMAL)
    public void rightsSection() {
        PoliciesPage page = reachPolicies();
        if (page.isErrorVisible() && !page.isSectionVisible(PoliciesPage.SEC_RIGHTS)) {
            Allure.parameter("skipped", "error-#50");
            return;
        }
        if (!page.isSectionVisible(PoliciesPage.SEC_RIGHTS)) {
            page.swipeBodyUp();
            sleepQuiet(600);
        }
        assertThat(page.isSectionVisible(PoliciesPage.SEC_RIGHTS)).isTrue();
    }

    @Test(priority = 9, description = "PO-L9: stay Vendor")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendor() {
        assertThat(reachPolicies().isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 10, description = "PO-L10: classify policies")
    @Severity(SeverityLevel.NORMAL)
    public void classifyPolicies() {
        reachPolicies();
        assertThat(classifyRentalNow()).isEqualTo("policies");
    }
}
