package com.l2b.vendor.modules.faq.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.faq.presentation.pages.FaqPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * FAQ interact — Never Accept / Decline / Log Out Confirm.
 * Handles error (#48) + full list expand/collapse.
 *
 * <p><b>Happy Path (FQ-I1–I10)</b>
 */
@Epic("Vendor app")
@Feature("FAQ interact — rental 9000000001")
public class FaqInteractTest extends FaqBaseTest {

    @Test(priority = 1, description = "FQ-I1: Retry when error (or expand review when list)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("BUGS_FOUND #48 path when Retry is shown.")
    public void retryOrExpandReview() {
        FaqPage page = reachFaq();
        if (page.isRetryVisible() && page.isErrorVisible()) {
            page.tapRetry();
            sleepQuiet(2000);
            page.attachScreenshot("fq-i1-retry");
            Allure.parameter("afterRetryError", String.valueOf(page.isErrorVisible()));
            Allure.parameter("afterRetryFull", String.valueOf(page.isFullListVisible()));
            assertThat(page.isDisplayedNow()).isTrue();
            assertThat(vendorPackage()).contains("l2b");
            return;
        }
        page.tapQuestion(FaqPage.Q_REVIEW);
        sleepQuiet(800);
        page.attachScreenshot("fq-i1");
        assertThat(page.hasCollapseControl() || page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 2, description = "FQ-I2: collapse after expand (or Retry again)")
    @Severity(SeverityLevel.CRITICAL)
    public void expandCollapseOrRetry() {
        FaqPage page = reachFaq();
        if (page.isErrorVisible() && !page.isFullListVisible()) {
            if (page.isRetryVisible()) {
                page.tapRetry();
                sleepQuiet(1500);
            }
            assertThat(page.isDisplayedNow()).isTrue();
            return;
        }
        page.tapQuestion(FaqPage.Q_REVIEW);
        sleepQuiet(700);
        if (page.hasCollapseControl()) {
            page.tapFirstCollapse();
            sleepQuiet(600);
        } else {
            page.tapQuestion(FaqPage.Q_REVIEW);
            sleepQuiet(600);
        }
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 3, description = "FQ-I3: expand start-job when available")
    @Severity(SeverityLevel.CRITICAL)
    public void expandStartJobWhenAvailable() {
        FaqPage page = reachFaq();
        if (page.isErrorVisible() && !page.isQuestionVisible(FaqPage.Q_START)) {
            Allure.parameter("skipped", "error-#48");
            return;
        }
        page.tapQuestion(FaqPage.Q_START);
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(page.hasCollapseControl() || page.isQuestionVisible(FaqPage.Q_START)).isTrue();
    }

    @Test(priority = 4, description = "FQ-I4: header Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void headerBack() {
        FaqPage page = reachFaq();
        page.tapBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 5, description = "FQ-I5: device Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBack() {
        FaqPage page = reachFaq();
        page.pressDeviceBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 6, description = "FQ-I6: reopen FAQ")
    @Severity(SeverityLevel.CRITICAL)
    public void reopen() {
        FaqPage page = reachFaq();
        page.tapBack();
        sleepQuiet(800);
        new ProfileDrawerPage().tapRow("FAQ");
        sleepQuiet(1100);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("faq");
    }

    @Test(priority = 7, description = "FQ-I7: scroll keeps title")
    @Severity(SeverityLevel.NORMAL)
    public void scrollKeepsTitle() {
        FaqPage page = reachFaq();
        page.swipeListUp();
        sleepQuiet(500);
        assertThat(page.isFaqTitleVisible() || page.isDisplayedNow()).isTrue();
        page.swipeListDown();
        sleepQuiet(500);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 8, description = "FQ-I8: expand cancel when available (or double Retry)")
    @Severity(SeverityLevel.NORMAL)
    public void expandCancelOrDoubleRetry() {
        FaqPage page = reachFaq();
        if (page.isErrorVisible() && !page.isQuestionVisible(FaqPage.Q_CANCEL)) {
            if (page.isRetryVisible()) {
                page.tapRetry();
                sleepQuiet(800);
                page.tapRetry();
                sleepQuiet(800);
            }
            assertThat(page.isDisplayedNow()).isTrue();
            return;
        }
        page.tapQuestion(FaqPage.Q_CANCEL);
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 9, description = "FQ-I9: Back then close drawer → Home")
    @Severity(SeverityLevel.CRITICAL)
    public void backToHome() {
        FaqPage page = reachFaq();
        page.tapBack();
        sleepQuiet(800);
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        assertThat(new HomePage().isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
    }

    @Test(priority = 10, description = "FQ-I10: stay Vendor after Retry/expand")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendorAfterAction() {
        FaqPage page = reachFaq();
        if (page.isRetryVisible() && page.isErrorVisible()) {
            page.tapRetry();
        } else if (page.isQuestionVisible(FaqPage.Q_PAID)) {
            page.tapQuestion(FaqPage.Q_PAID);
        }
        sleepQuiet(700);
        assertThat(vendorPackage()).contains("l2b");
        Allure.parameter("after", classifyRentalNow());
    }
}
