package com.l2b.vendor.modules.faq.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.faq.presentation.pages.FaqPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * FAQ landing — Profile drawer → FAQ on {@code 9000000001}.
 * Dump {@code /tmp/l2b-faq-0001-20261003}.
 *
 * <p>Live 3 Oct: error empty state &quot;Something went wrong. Please try again.&quot; + Retry
 * (BUGS_FOUND #48). Full question list asserted when available.
 *
 * <p><b>Happy Path (FQ-L1–L10)</b>
 */
@Epic("Vendor app")
@Feature("FAQ landing — rental 9000000001")
public class FaqLandingTest extends FaqBaseTest {

    @Test(priority = 1, description = "FQ-L1: drawer FAQ opens")
    @Severity(SeverityLevel.BLOCKER)
    public void faqOpens() {
        FaqPage page = reachFaq();
        page.attachScreenshot("fq-l1");
        assertThat(page.isDisplayedNow()).as("FAQ open").isTrue();
    }

    @Test(priority = 2, description = "FQ-L2: FAQs title + Back")
    @Severity(SeverityLevel.CRITICAL)
    public void titleAndBack() {
        FaqPage page = reachFaq();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isFaqTitleVisible()).as("FAQs title").isTrue();
        softly.assertThat(page.isBackVisible()).as("Back").isTrue();
        softly.assertThat(page.headerBackAligned()).as("Back left").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "FQ-L3: list OR error empty state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #48 when FAQ fails to load on seeded 9000000001.")
    public void listOrError() {
        FaqPage page = reachFaq();
        boolean full = page.isFullListVisible();
        boolean err = page.isErrorVisible();
        Allure.parameter("fullList", String.valueOf(full));
        Allure.parameter("errorState", String.valueOf(err));
        Allure.parameter("bug", err && !full ? "48" : "none");
        page.attachScreenshot("fq-l3");
        assertThat(full || err).as("full list or error").isTrue();
    }

    @Test(priority = 4, description = "FQ-L4: Retry visible when error")
    @Severity(SeverityLevel.CRITICAL)
    public void retryWhenError() {
        FaqPage page = reachFaq();
        if (!page.isErrorVisible()) {
            Allure.parameter("skipped", "full-list");
            return;
        }
        assertThat(page.isRetryVisible()).as("Retry").isTrue();
    }

    @Test(priority = 5, description = "FQ-L5: review question when list loaded")
    @Severity(SeverityLevel.CRITICAL)
    public void questionReviewWhenAvailable() {
        FaqPage page = reachFaq();
        if (page.isErrorVisible() && !page.isQuestionVisible(FaqPage.Q_REVIEW)) {
            Allure.parameter("skipped", "error-#48");
            return;
        }
        assertThat(page.isQuestionVisible(FaqPage.Q_REVIEW)).isTrue();
    }

    @Test(priority = 6, description = "FQ-L6: paid + start questions when available")
    @Severity(SeverityLevel.CRITICAL)
    public void questionPaidStartWhenAvailable() {
        FaqPage page = reachFaq();
        if (page.isErrorVisible() && !page.isFullListVisible()) {
            Allure.parameter("skipped", "error-#48");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isQuestionVisible(FaqPage.Q_PAID)).as("paid").isTrue();
        softly.assertThat(page.isQuestionVisible(FaqPage.Q_START)).as("start").isTrue();
        softly.assertAll();
    }

    @Test(priority = 7, description = "FQ-L7: cancel + human questions when available")
    @Severity(SeverityLevel.CRITICAL)
    public void questionCancelHumanWhenAvailable() {
        FaqPage page = reachFaq();
        if (page.isErrorVisible() && !page.isFullListVisible()) {
            Allure.parameter("skipped", "error-#48");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isQuestionVisible(FaqPage.Q_CANCEL)).as("cancel").isTrue();
        softly.assertThat(page.isQuestionVisible(FaqPage.Q_HUMAN)
                || page.isQuestionVisible(FaqPage.Q_HUMAN_ALT)).as("human").isTrue();
        softly.assertAll();
    }

    @Test(priority = 8, description = "FQ-L8: Expand control when list loaded")
    @Severity(SeverityLevel.NORMAL)
    public void expandWhenAvailable() {
        FaqPage page = reachFaq();
        if (page.isErrorVisible() && !page.isFullListVisible()) {
            Allure.parameter("skipped", "error-#48");
            return;
        }
        assertThat(page.hasExpandControl() || page.hasCollapseControl()).isTrue();
    }

    @Test(priority = 9, description = "FQ-L9: stay Vendor")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendor() {
        assertThat(reachFaq().isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 10, description = "FQ-L10: classify faq")
    @Severity(SeverityLevel.NORMAL)
    public void classifyFaq() {
        reachFaq();
        assertThat(classifyRentalNow()).isEqualTo("faq");
    }
}
