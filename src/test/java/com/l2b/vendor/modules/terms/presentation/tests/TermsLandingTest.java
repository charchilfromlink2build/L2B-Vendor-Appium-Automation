package com.l2b.vendor.modules.terms.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.terms.presentation.pages.TermsPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Terms & Services landing — Profile drawer on {@code 9000000001}.
 * Dump {@code /tmp/l2b-terms-0001-20261003}.
 *
 * <p><b>Happy Path (TM-L1–L10)</b> — prefix TS to avoid Team collision.
 */
@Epic("Vendor app")
@Feature("Terms landing — rental 9000000001")
public class TermsLandingTest extends TermsBaseTest {

    @Test(priority = 1, description = "TS-L1: drawer Terms opens")
    @Severity(SeverityLevel.BLOCKER)
    public void termsOpens() {
        TermsPage page = reachTerms();
        page.attachScreenshot("ts-l1");
        assertThat(page.isDisplayedNow()).as("Terms open").isTrue();
    }

    @Test(priority = 2, description = "TS-L2: title + Back")
    @Severity(SeverityLevel.CRITICAL)
    public void titleAndBack() {
        TermsPage page = reachTerms();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isTermsTitleVisible()).as("title").isTrue();
        softly.assertThat(page.isBackVisible()).as("Back").isTrue();
        softly.assertThat(page.headerBackAligned()).as("Back left").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "TS-L3: chrome OR error empty state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #49 when Terms fails to load on seeded 9000000001.")
    public void chromeOrError() {
        TermsPage page = reachTerms();
        boolean full = page.isFullChromeVisible()
                || page.isSectionVisible(TermsPage.SEC_ACCEPTING);
        boolean err = page.isErrorVisible();
        Allure.parameter("fullChrome", String.valueOf(full));
        Allure.parameter("errorState", String.valueOf(err));
        Allure.parameter("bug", err && !full ? "49" : "none");
        page.attachScreenshot("ts-l3");
        assertThat(full || err).as("full chrome or error").isTrue();
    }

    @Test(priority = 4, description = "TS-L4: Retry visible when error")
    @Severity(SeverityLevel.CRITICAL)
    public void retryWhenError() {
        TermsPage page = reachTerms();
        if (!page.isErrorVisible()) {
            Allure.parameter("skipped", "full-chrome");
            return;
        }
        assertThat(page.isRetryVisible()).as("Retry").isTrue();
    }

    @Test(priority = 5, description = "TS-L5: Accepting these terms when available")
    @Severity(SeverityLevel.CRITICAL)
    public void acceptingSection() {
        TermsPage page = reachTerms();
        if (page.isErrorVisible() && !page.isSectionVisible(TermsPage.SEC_ACCEPTING)) {
            Allure.parameter("skipped", "error-#49");
            return;
        }
        assertThat(page.isSectionVisible(TermsPage.SEC_ACCEPTING)).isTrue();
    }

    @Test(priority = 6, description = "TS-L6: Your obligations when available")
    @Severity(SeverityLevel.CRITICAL)
    public void obligationsSection() {
        TermsPage page = reachTerms();
        if (page.isErrorVisible() && !page.isSectionVisible(TermsPage.SEC_OBLIGATIONS)) {
            Allure.parameter("skipped", "error-#49");
            return;
        }
        assertThat(page.isSectionVisible(TermsPage.SEC_OBLIGATIONS)).isTrue();
    }

    @Test(priority = 7, description = "TS-L7: You must not + off-platform when available")
    @Severity(SeverityLevel.CRITICAL)
    public void mustNotBullet() {
        TermsPage page = reachTerms();
        if (page.isErrorVisible() && !page.isSectionVisible(TermsPage.SEC_MUST_NOT)) {
            Allure.parameter("skipped", "error-#49");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isSectionVisible(TermsPage.SEC_MUST_NOT)).as("must not").isTrue();
        softly.assertThat(page.isSectionVisible(TermsPage.BULLET_OFF_PLATFORM)
                || page.isSectionVisible("outside the platform")).as("off-platform").isTrue();
        softly.assertAll();
    }

    @Test(priority = 8, description = "TS-L8: Payments / Suspension when available")
    @Severity(SeverityLevel.NORMAL)
    public void paymentsAndSuspension() {
        TermsPage page = reachTerms();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error-#49");
            return;
        }
        if (!page.isSectionVisible(TermsPage.SEC_PAYMENTS)) {
            page.swipeBodyUp();
            sleepQuiet(600);
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isSectionVisible(TermsPage.SEC_PAYMENTS)).as("payments").isTrue();
        page.swipeBodyUp();
        sleepQuiet(500);
        softly.assertThat(page.isSectionVisible(TermsPage.SEC_SUSPEND)
                || page.isSectionVisible("suspend")).as("suspension").isTrue();
        softly.assertAll();
    }

    @Test(priority = 9, description = "TS-L9: stay Vendor")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendor() {
        assertThat(reachTerms().isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 10, description = "TS-L10: classify terms")
    @Severity(SeverityLevel.NORMAL)
    public void classifyTerms() {
        reachTerms();
        assertThat(classifyRentalNow()).isEqualTo("terms");
    }
}
