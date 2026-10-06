package com.l2b.vendor.modules.refer.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
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
 * Refer & Earn edge — Never Accept / Decline / Log Out Confirm.
 *
 * <p><b>Critical Edge (RF-E1–E10)</b>
 */
@Epic("Vendor app")
@Feature("Refer edge — rental 9000000001")
public class ReferEdgeTest extends ReferBaseTest {

    @Test(priority = 1, description = "RF-E1: rapid Retry/Copy stays Vendor")
    @Severity(SeverityLevel.CRITICAL)
    public void rapidActionStaysVendor() {
        ReferPage page = reachRefer();
        for (int i = 0; i < 3; i++) {
            if (page.isRetryVisible()) {
                page.tapRetry();
            } else if (page.isCopyCodeVisible()) {
                page.tapCopyCode();
            }
            sleepQuiet(400);
        }
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("refer");
    }

    @Test(priority = 2, description = "RF-E2: layout Back left + Refer now bottom")
    @Severity(SeverityLevel.CRITICAL)
    public void layoutChecks() {
        ReferPage page = reachRefer();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.headerBackAligned()).as("Back left").isTrue();
        softly.assertThat(page.referNowNearBottom()).as("Refer now bottom").isTrue();
        softly.assertThat(page.isReferTitleVisible()).as("title").isTrue();
        softly.assertAll();
    }

    @Test(priority = 3, description = "RF-E3: unavailable persists or recovers after Retry")
    @Severity(SeverityLevel.CRITICAL)
    @Description("BUGS_FOUND #47 — document Retry outcome.")
    public void unavailableAfterRetry() {
        ReferPage page = reachRefer();
        if (!page.isUnavailableVisible()) {
            Allure.parameter("state", "full-chrome");
            assertThat(page.isReferralCodeValueVisible() || page.isHeroEarnVisible()).isTrue();
            return;
        }
        page.tapRetry();
        sleepQuiet(2500);
        page.attachScreenshot("rf-e3-after-retry");
        Allure.parameter("stillUnavailable", String.valueOf(page.isUnavailableVisible()));
        Allure.parameter("recoveredFull", String.valueOf(page.isFullChromeVisible()));
        assertThat(page.isDisplayedNow()).isTrue();
        // Soft: prefer recovery; if still empty, still Vendor + Retry chrome.
        SoftAssertions softly = new SoftAssertions();
        if (page.isUnavailableVisible()) {
            softly.assertThat(page.isRetryVisible()).as("Retry remains").isTrue();
        } else {
            softly.assertThat(page.isFullChromeVisible() || page.isHeroEarnVisible())
                    .as("recovered chrome").isTrue();
        }
        softly.assertAll();
    }

    @Test(priority = 4, description = "RF-E4: Refer now then header Back")
    @Severity(SeverityLevel.NORMAL)
    public void referNowThenBack() {
        ReferPage page = reachRefer();
        page.tapReferNow();
        sleepQuiet(1000);
        if (!page.isDisplayedNow()) {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
            page = dismissToRefer(page);
        }
        page.tapBack();
        sleepQuiet(800);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 5, description = "RF-E5: reopen after Home")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterHome() {
        ReferPage page = reachRefer();
        page.tapBack();
        sleepQuiet(700);
        DriverManager.get().navigate().back();
        sleepQuiet(800);
        HomePage home = ensureRentalHomeWarm();
        assertThat(home.isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
        page = reachRefer();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 6, description = "RF-E6: package through action + Back")
    @Severity(SeverityLevel.NORMAL)
    public void packageThroughActionAndBack() {
        ReferPage page = reachRefer();
        if (page.isRetryVisible()) {
            page.tapRetry();
        } else if (page.isCopyCodeVisible()) {
            page.tapCopyCode();
        }
        sleepQuiet(700);
        page.tapBack();
        sleepQuiet(800);
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 7, description = "RF-E7: no Accept/Decline on Refer path")
    @Severity(SeverityLevel.CRITICAL)
    public void noAcceptDecline() {
        ReferPage page = reachRefer();
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(hasText("Accept")).as("no Accept").isFalse();
        softly.assertThat(hasText("Decline")).as("no Decline").isFalse();
        softly.assertAll();
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 8, description = "RF-E8: double header Back safe")
    @Severity(SeverityLevel.NORMAL)
    public void doubleHeaderBackSafe() {
        ReferPage page = reachRefer();
        page.tapBack();
        sleepQuiet(700);
        if (profileDrawerNow()) {
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        Allure.parameter("after", classifyRentalNow());
        assertThat(vendorPackage()).contains("l2b");
        assertThat(classifyRentalNow()).isIn("home", "home-drawer", "refer", "launcher");
    }

    @Test(priority = 9, description = "RF-E9: Refer now still shown while unavailable")
    @Severity(SeverityLevel.NORMAL)
    @Description("UX note / #47 related: Refer now remains while empty state is shown.")
    public void referNowWhileUnavailable() {
        ReferPage page = reachRefer();
        if (!page.isUnavailableVisible()) {
            Allure.parameter("skipped", "full-chrome");
            assertThat(page.isReferNowVisible()).isTrue();
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isReferNowVisible()).as("Refer now still visible").isTrue();
        softly.assertThat(page.isRetryVisible()).as("Retry").isTrue();
        softly.assertAll();
    }

    @Test(priority = 10, description = "RF-E10: EN steps when full chrome")
    @Severity(SeverityLevel.NORMAL)
    public void englishStepsWhenAvailable() {
        ReferPage page = reachRefer();
        if (page.isUnavailableVisible() && !page.isStepShareVisible()) {
            Allure.parameter("skipped", "unavailable-#47");
            return;
        }
        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(page.isStepShareVisible()).as("Share your code").isTrue();
        softly.assertThat(page.isStepSignUpVisible()).as("They sign up").isTrue();
        softly.assertThat(page.isStepEarnVisible()).as("You both earn").isTrue();
        softly.assertAll();
    }
}
