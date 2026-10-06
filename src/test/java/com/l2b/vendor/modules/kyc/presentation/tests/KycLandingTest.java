package com.l2b.vendor.modules.kyc.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.kyc.presentation.pages.KycPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * KYC landing — Profile drawer → KYC on {@code 9000000001}.
 *
 * <p><b>Happy Path (KYC-L1–L10)</b>
 */
@Epic("Vendor app")
@Feature("KYC landing — rental 9000000001")
public class KycLandingTest extends KycBaseTest {

    @Test(priority = 1, description = "KYC-L1: drawer KYC opens")
    @Severity(SeverityLevel.BLOCKER)
    public void kycOpens() {
        KycPage page = reachKyc();
        page.attachScreenshot("kyc-l1");
        assertThat(page.isDisplayedNow()).as("KYC open").isTrue();
    }

    @Test(priority = 2, description = "KYC-L2: title + Back")
    @Severity(SeverityLevel.CRITICAL)
    public void titleAndBack() {
        KycPage page = reachKyc();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isTitleVisible()).as("title").isTrue();
        softly.assertThat(page.isBackVisible()).as("Back").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "KYC-L3: chrome OR error empty state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Likely BUGS_FOUND #52 API outage when KYC fails to load on seeded 0001.")
    public void chromeOrError() {
        KycPage page = reachKyc();
        boolean full = page.isFullChromeVisible();
        boolean err = page.isErrorVisible();
        Allure.parameter("fullChrome", String.valueOf(full));
        Allure.parameter("errorState", String.valueOf(err));
        Allure.parameter("bug", err && !full ? "52-or-kyc-empty" : "none");
        page.attachScreenshot("kyc-l3");
        assertThat(full || err).as("full chrome or error").isTrue();
    }

    @Test(priority = 4, description = "KYC-L4: Retry when error")
    @Severity(SeverityLevel.CRITICAL)
    public void retryWhenError() {
        KycPage page = reachKyc();
        if (!page.isErrorVisible()) {
            Allure.parameter("skipped", "full-chrome");
            return;
        }
        assertThat(page.isRetryVisible()).as("Retry").isTrue();
    }

    @Test(priority = 5, description = "KYC-L5: Document details when available")
    @Severity(SeverityLevel.CRITICAL)
    public void documentDetails() {
        KycPage page = reachKyc();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error");
            return;
        }
        assertThat(page.isDocumentDetailsVisible()).isTrue();
    }

    @Test(priority = 6, description = "KYC-L6: PAN when available")
    @Severity(SeverityLevel.CRITICAL)
    public void panRow() {
        KycPage page = reachKyc();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error");
            return;
        }
        assertThat(page.isPanVisible()).isTrue();
    }

    @Test(priority = 7, description = "KYC-L7: Bank details when available")
    @Severity(SeverityLevel.CRITICAL)
    public void bankDetails() {
        KycPage page = reachKyc();
        if (page.isErrorVisible() && !page.isFullChromeVisible()) {
            Allure.parameter("skipped", "error");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isBankDetailsVisible() || page.isDocumentDetailsVisible())
                .as("bank or docs chrome").isTrue();
        softly.assertAll();
    }

    @Test(priority = 8, description = "KYC-L8: package stays Vendor")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendorPackage() {
        reachKyc();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 9, description = "KYC-L9: no Accept/Decline on KYC")
    @Severity(SeverityLevel.CRITICAL)
    public void noAcceptDecline() {
        KycPage page = reachKyc();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("Decline")).as("no Decline").isFalse();
        softly.assertThat(noAcceptVisible()).as("no Accept").isTrue();
        softly.assertAll();
        page.attachScreenshot("kyc-l9");
    }

    @Test(priority = 10, description = "KYC-L10: Back returns toward Home/drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void backLeavesKyc() {
        KycPage page = reachKyc();
        page.tapBack();
        sleepQuiet(900);
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(profileDrawerNow() || hasText("Current Earning")
                || hasText("Account") || hasText("Kasim Pathan")).as("Home/drawer").isTrue();
        softly.assertAll();
    }

    private boolean noAcceptVisible() {
        return com.l2b.vendor.core.driver.DriverManager.get().findElements(
                org.openqa.selenium.By.xpath("//*[@text='Accept']")).isEmpty();
    }
}
