package com.l2b.vendor.modules.kyc.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.kyc.presentation.pages.KycPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * KYC interact — Retry / scroll / Back. Never Accept / Decline / Log Out Confirm.
 *
 * <p><b>Happy Path (KYC-I1–I10)</b>
 */
@Epic("Vendor app")
@Feature("KYC interact — rental 9000000001")
public class KycInteractTest extends KycBaseTest {

    @Test(priority = 1, description = "KYC-I1: open KYC")
    @Severity(SeverityLevel.BLOCKER)
    public void openKyc() {
        assertThat(reachKyc().isDisplayedNow()).isTrue();
    }

    @Test(priority = 2, description = "KYC-I2: Retry when error does not crash")
    @Severity(SeverityLevel.CRITICAL)
    public void retrySafe() {
        KycPage page = reachKyc();
        if (!page.isErrorVisible() || !page.isRetryVisible()) {
            Allure.parameter("skipped", "no-error");
            return;
        }
        page.tapRetry();
        sleepQuiet(2000);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(vendorPackage()).contains("l2b");
        softly.assertThat(page.isDisplayedNow() || page.isErrorVisible()
                || page.isFullChromeVisible()).isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "KYC-I3: scroll does not leave Vendor")
    @Severity(SeverityLevel.NORMAL)
    public void scrollSafe() {
        KycPage page = reachKyc();
        page.swipeListUp();
        sleepQuiet(600);
        assertThat(vendorPackage()).contains("l2b");
        assertThat(page.isDisplayedNow() || page.isErrorVisible()).isTrue();
    }

    @Test(priority = 4, description = "KYC-I4: Back to drawer/Home")
    @Severity(SeverityLevel.CRITICAL)
    public void backToDrawerOrHome() {
        KycPage page = reachKyc();
        page.tapBack();
        sleepQuiet(900);
        assertThat(profileDrawerNow() || hasText("Current Earning") || hasText("Account"))
                .isTrue();
    }

    @Test(priority = 5, description = "KYC-I5: re-open KYC after Back")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterBack() {
        KycPage page = reachKyc();
        page.tapBack();
        sleepQuiet(800);
        page = reachKyc();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 6, description = "KYC-I6: document chrome when loaded")
    @Severity(SeverityLevel.NORMAL)
    public void docsWhenLoaded() {
        KycPage page = reachKyc();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error");
            return;
        }
        assertThat(page.isDocumentDetailsVisible() || page.isPanVisible()).isTrue();
    }

    @Test(priority = 7, description = "KYC-I7: bank chrome when loaded")
    @Severity(SeverityLevel.NORMAL)
    public void bankWhenLoaded() {
        KycPage page = reachKyc();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error");
            return;
        }
        page.swipeListUp();
        sleepQuiet(500);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isBankDetailsVisible() || page.isDocumentDetailsVisible()
                || hasText("Yes Bank") || hasText("IFSC")).as("bank/docs").isTrue();
        softly.assertAll();
    }

    @Test(priority = 8, description = "KYC-I8: double Back safe")
    @Severity(SeverityLevel.NORMAL)
    public void doubleBackSafe() {
        KycPage page = reachKyc();
        page.tapBack();
        sleepQuiet(500);
        try {
            com.l2b.vendor.core.driver.DriverManager.get().navigate().back();
        } catch (RuntimeException ignored) {
        }
        sleepQuiet(700);
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 9, description = "KYC-I9: no Log Out Confirm from KYC")
    @Severity(SeverityLevel.CRITICAL)
    public void noLogoutConfirm() {
        reachKyc();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("Log Out?")).isFalse();
        softly.assertThat(hasText("Are you sure you want to log out")).isFalse();
        softly.assertAll();
    }

    @Test(priority = 10, description = "KYC-I10: stay on Vendor after Retry+scroll")
    @Severity(SeverityLevel.NORMAL)
    public void retryScrollStayVendor() {
        KycPage page = reachKyc();
        if (page.isRetryVisible()) {
            page.tapRetry();
            sleepQuiet(1200);
        }
        page.swipeListUp();
        sleepQuiet(500);
        assertThat(vendorPackage()).contains("l2b");
    }
}
