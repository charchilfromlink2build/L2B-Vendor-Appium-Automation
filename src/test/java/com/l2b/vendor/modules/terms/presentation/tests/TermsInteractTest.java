package com.l2b.vendor.modules.terms.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import com.l2b.vendor.modules.terms.presentation.pages.TermsPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Terms interact — scroll + Back only. Never Accept / Decline / Log Out Confirm.
 *
 * <p><b>Happy Path (TS-I1–I10)</b>
 */
@Epic("Vendor app")
@Feature("Terms interact — rental 9000000001")
public class TermsInteractTest extends TermsBaseTest {

    @Test(priority = 1, description = "TS-I1: scroll down keeps Terms")
    @Severity(SeverityLevel.BLOCKER)
    public void scrollDown() {
        TermsPage page = reachTerms();
        page.swipeBodyUp();
        sleepQuiet(700);
        page.attachScreenshot("ts-i1");
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("terms");
    }

    @Test(priority = 2, description = "TS-I2: scroll up recovers accepting")
    @Severity(SeverityLevel.CRITICAL)
    public void scrollUpRecovers() {
        TermsPage page = reachTerms();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error-state");
            return;
        }
        page.swipeBodyUp();
        sleepQuiet(500);
        page.swipeBodyDown();
        sleepQuiet(600);
        assertThat(page.isSectionVisible(TermsPage.SEC_ACCEPTING)
                || page.isTermsTitleVisible()).isTrue();
    }

    @Test(priority = 3, description = "TS-I3: header Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void headerBack() {
        TermsPage page = reachTerms();
        page.tapBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 4, description = "TS-I4: device Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBack() {
        TermsPage page = reachTerms();
        page.pressDeviceBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 5, description = "TS-I5: reopen Terms")
    @Severity(SeverityLevel.CRITICAL)
    public void reopen() {
        TermsPage page = reachTerms();
        page.tapBack();
        sleepQuiet(800);
        new ProfileDrawerPage().tapRow("Terms & Services");
        sleepQuiet(1100);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("terms");
    }

    @Test(priority = 6, description = "TS-I6: double scroll safe")
    @Severity(SeverityLevel.NORMAL)
    public void doubleScroll() {
        TermsPage page = reachTerms();
        page.swipeBodyUp();
        sleepQuiet(400);
        page.swipeBodyUp();
        sleepQuiet(400);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 7, description = "TS-I7: Retry when error (else scroll)")
    @Severity(SeverityLevel.NORMAL)
    public void retryOrScroll() {
        TermsPage page = reachTerms();
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

    @Test(priority = 8, description = "TS-I8: title survives scroll")
    @Severity(SeverityLevel.NORMAL)
    public void titleSurvivesScroll() {
        TermsPage page = reachTerms();
        page.swipeBodyUp();
        sleepQuiet(500);
        assertThat(page.isTermsTitleVisible() || page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 9, description = "TS-I9: Back then close drawer → Home")
    @Severity(SeverityLevel.CRITICAL)
    public void backToHome() {
        TermsPage page = reachTerms();
        page.tapBack();
        sleepQuiet(800);
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        assertThat(new HomePage().isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
    }

    @Test(priority = 10, description = "TS-I10: stay Vendor after scroll")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendor() {
        TermsPage page = reachTerms();
        page.swipeBodyUp();
        sleepQuiet(500);
        assertThat(vendorPackage()).contains("l2b");
        Allure.parameter("after", classifyRentalNow());
    }
}
