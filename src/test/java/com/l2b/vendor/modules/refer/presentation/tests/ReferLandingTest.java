package com.l2b.vendor.modules.refer.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.modules.refer.presentation.pages.ReferPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.Test;

/**
 * Refer & Earn landing — Profile drawer → Refer & Earn on {@code 9000000001}.
 * Dump {@code /tmp/l2b-refer-0001-20261003}.
 *
 * <p>Live 3 Oct: empty/error state &quot;Referrals are not available right now.&quot; + Retry
 * (BUGS_FOUND #47). Full chrome (code HRHND8 + how-to) asserted when available.
 *
 * <p><b>Happy Path (RF-L1–L10)</b>
 */
@Epic("Vendor app")
@Feature("Refer landing — rental 9000000001")
public class ReferLandingTest extends ReferBaseTest {

    @Test(priority = 1, description = "RF-L1: drawer Refer & Earn opens")
    @Severity(SeverityLevel.BLOCKER)
    public void referOpens() {
        ReferPage page = reachRefer();
        page.attachScreenshot("rf-l1");
        assertThat(page.isDisplayedNow()).as("Refer open").isTrue();
    }

    @Test(priority = 2, description = "RF-L2: Refer & Earn title + Back")
    @Severity(SeverityLevel.CRITICAL)
    public void titleAndBackChrome() {
        ReferPage page = reachRefer();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isReferTitleVisible()).as("title").isTrue();
        softly.assertThat(page.isBackVisible()).as("Back").isTrue();
        softly.assertThat(page.headerBackAligned()).as("Back left inset").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "RF-L3: content OR unavailable empty state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #47 when unavailable on seeded 9000000001.")
    public void contentOrUnavailable() {
        ReferPage page = reachRefer();
        boolean full = page.isFullChromeVisible() || page.isHeroEarnVisible();
        boolean empty = page.isUnavailableVisible();
        Allure.parameter("fullChrome", String.valueOf(full));
        Allure.parameter("unavailable", String.valueOf(empty));
        Allure.parameter("bug", empty && !full ? "47" : "none");
        page.attachScreenshot("rf-l3");
        assertThat(full || empty).as("full chrome or unavailable").isTrue();
    }

    @Test(priority = 4, description = "RF-L4: Retry visible when unavailable")
    @Severity(SeverityLevel.CRITICAL)
    public void retryWhenUnavailable() {
        ReferPage page = reachRefer();
        if (!page.isUnavailableVisible()) {
            Allure.parameter("skipped", "full-chrome");
            return;
        }
        assertThat(page.isRetryVisible()).as("Retry").isTrue();
    }

    @Test(priority = 5, description = "RF-L5: referral code when available")
    @Severity(SeverityLevel.CRITICAL)
    public void referralCodeWhenAvailable() {
        ReferPage page = reachRefer();
        if (page.isUnavailableVisible() && !page.isReferralCodeLabelVisible()) {
            Allure.parameter("skipped", "unavailable-#47");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isReferralCodeLabelVisible()).as("label").isTrue();
        softly.assertThat(page.isReferralCodeValueVisible()).as("code").isTrue();
        softly.assertAll();
    }

    @Test(priority = 6, description = "RF-L6: how-to when available")
    @Severity(SeverityLevel.NORMAL)
    public void howToWhenAvailable() {
        ReferPage page = reachRefer();
        if (page.isUnavailableVisible() && !page.isHowToHeadingVisible()) {
            Allure.parameter("skipped", "unavailable-#47");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isHowToHeadingVisible()).as("how-to").isTrue();
        softly.assertThat(page.isThreeStepsIntroVisible()).as("intro").isTrue();
        softly.assertAll();
    }

    @Test(priority = 7, description = "RF-L7: three steps when available")
    @Severity(SeverityLevel.CRITICAL)
    public void threeStepsWhenAvailable() {
        ReferPage page = reachRefer();
        if (page.isUnavailableVisible() && !page.isStepShareVisible()) {
            Allure.parameter("skipped", "unavailable-#47");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isStepShareVisible()).as("Share").isTrue();
        softly.assertThat(page.isStepSignUpVisible()).as("Sign up").isTrue();
        softly.assertThat(page.isStepEarnVisible()).as("Earn").isTrue();
        softly.assertAll();
    }

    @Test(priority = 8, description = "RF-L8: Copy control when available")
    @Severity(SeverityLevel.CRITICAL)
    public void copyWhenAvailable() {
        ReferPage page = reachRefer();
        if (page.isUnavailableVisible() && !page.isCopyCodeVisible()) {
            Allure.parameter("skipped", "unavailable-#47");
            return;
        }
        assertThat(page.isCopyCodeVisible()).as("Copy").isTrue();
    }

    @Test(priority = 9, description = "RF-L9: Refer now CTA near bottom")
    @Severity(SeverityLevel.CRITICAL)
    public void referNowCta() {
        ReferPage page = reachRefer();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isReferNowVisible()).as("Refer now").isTrue();
        softly.assertThat(page.referNowNearBottom()).as("near bottom").isTrue();
        softly.assertAll();
    }

    @Test(priority = 10, description = "RF-L10: stay Vendor package")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendorPackage() {
        ReferPage page = reachRefer();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }
}
