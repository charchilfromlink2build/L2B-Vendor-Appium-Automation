package com.l2b.vendor.modules.faq.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.faq.presentation.pages.FaqPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * FAQ edge — Never Accept / Decline / Log Out Confirm.
 *
 * <p><b>Critical Edge (FQ-E1–E10)</b>
 */
@Epic("Vendor app")
@Feature("FAQ edge — rental 9000000001")
public class FaqEdgeTest extends FaqBaseTest {

    @Test(priority = 1, description = "FQ-E1: rapid Retry/expand stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidActionStaysVendor() {
        FaqPage page = reachFaq();
        for (int i = 0; i < 3; i++) {
            if (page.isRetryVisible() && page.isErrorVisible()) {
                page.tapRetry();
            } else if (page.isQuestionVisible(FaqPage.Q_REVIEW)) {
                page.tapQuestion(FaqPage.Q_REVIEW);
            }
            sleepQuiet(400);
        }
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("faq");
    }

    @Test(priority = 2, description = "FQ-E2: layout Back left")
    @Severity(SeverityLevel.CRITICAL)
    public void layoutBack() {
        FaqPage page = reachFaq();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.headerBackAligned()).as("Back left").isTrue();
        softly.assertThat(page.isFaqTitleVisible()).as("title").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "FQ-E3: error persists or recovers after Retry")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #48 — document Retry outcome.")
    public void errorAfterRetry() {
        FaqPage page = reachFaq();
        if (!page.isErrorVisible()) {
            Allure.parameter("state", "full-list");
            assertThat(page.isFullListVisible()).isTrue();
            return;
        }
        page.tapRetry();
        sleepQuiet(2500);
        page.attachScreenshot("fq-e3-after-retry");
        Allure.parameter("stillError", String.valueOf(page.isErrorVisible()));
        Allure.parameter("recoveredFull", String.valueOf(page.isFullListVisible()));
        assertThat(page.isDisplayedNow()).isTrue();
        SoftAssertions softly = new SoftAssertions();
        if (page.isErrorVisible()) {
            softly.assertThat(page.isRetryVisible()).as("Retry remains").isTrue();
        } else {
            softly.assertThat(page.isFullListVisible()).as("recovered list").isTrue();
        }
        softly.assertAll();
    }

    @Test(priority = 4, description = "FQ-E4: all five questions when list loaded")
    @Severity(SeverityLevel.CRITICAL)
    public void allQuestionsWhenAvailable() {
        FaqPage page = reachFaq();
        if (page.isErrorVisible() && !page.isFullListVisible()) {
            Allure.parameter("skipped", "error-#48");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isQuestionVisible(FaqPage.Q_REVIEW)).as("review").isTrue();
        softly.assertThat(page.isQuestionVisible(FaqPage.Q_PAID)).as("paid").isTrue();
        softly.assertThat(page.isQuestionVisible(FaqPage.Q_START)).as("start").isTrue();
        softly.assertThat(page.isQuestionVisible(FaqPage.Q_CANCEL)).as("cancel").isTrue();
        softly.assertThat(page.isQuestionVisible(FaqPage.Q_HUMAN)
                || page.isQuestionVisible(FaqPage.Q_HUMAN_ALT)).as("human").isTrue();
        softly.assertAll();
    }

    @Test(priority = 5, description = "FQ-E5: reopen after Home")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterHome() {
        FaqPage page = reachFaq();
        page.tapBack();
        sleepQuiet(700);
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        ensureRentalHomeWarm();
        page = reachFaq();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 6, description = "FQ-E6: no Accept/Decline")
    @Severity(SeverityLevel.CRITICAL)
    public void noAcceptDecline() {
        FaqPage page = reachFaq();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("Accept")).as("no Accept").isFalse();
        softly.assertThat(hasText("Decline")).as("no Decline").isFalse();
        softly.assertAll();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 7, description = "FQ-E7: double Back safe")
    @Severity(SeverityLevel.NORMAL)
    public void doubleBack() {
        FaqPage page = reachFaq();
        page.tapBack();
        sleepQuiet(700);
        if (profileDrawerNow()) {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        Allure.parameter("after", classifyRentalNow());
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 8, description = "FQ-E8: scroll extremes stay FAQ")
    @Severity(SeverityLevel.NORMAL)
    public void scrollExtremes() {
        FaqPage page = reachFaq();
        page.swipeListUp();
        sleepQuiet(400);
        page.swipeListUp();
        sleepQuiet(400);
        assertThat(page.isFaqTitleVisible() || page.isDisplayedNow()).isTrue();
        page.swipeListDown();
        sleepQuiet(400);
        assertThat(classifyRentalNow()).isEqualTo("faq");
    }

    @Test(priority = 9, description = "FQ-E9: package through Retry/expand + Back")
    @Severity(SeverityLevel.NORMAL)
    public void packageActionBack() {
        FaqPage page = reachFaq();
        if (page.isRetryVisible() && page.isErrorVisible()) {
            page.tapRetry();
        } else if (page.isQuestionVisible(FaqPage.Q_CANCEL)) {
            page.tapQuestion(FaqPage.Q_CANCEL);
        }
        sleepQuiet(600);
        page.tapBack();
        sleepQuiet(800);
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 10, description = "FQ-E10: Home after drawer close")
    @Severity(SeverityLevel.NORMAL)
    public void homeAfterDrawerClose() {
        FaqPage page = reachFaq();
        page.tapBack();
        sleepQuiet(700);
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        assertThat(new HomePage().isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
    }
}
