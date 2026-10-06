package com.l2b.vendor.modules.refer.presentation.tests;

import static org.assertj.core.api.Assertions.assertThat;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.refer.presentation.pages.ReferPage;
import com.l2b.vendor.modules.settings.presentation.pages.ProfileDrawerPage;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import org.testng.annotations.Test;

/**
 * Refer & Earn interact — Never Accept / Decline / Log Out Confirm.
 * Handles unavailable (#47) + full chrome.
 *
 * <p><b>Happy Path (RF-I1–I10)</b>
 */
@Epic("Vendor app")
@Feature("Refer interact — rental 9000000001")
public class ReferInteractTest extends ReferBaseTest {

    @Test(priority = 1, description = "RF-I1: Retry when unavailable (or Copy when available)")
    @Severity(SeverityLevel.BLOCKER)
    @Description("BUGS_FOUND #47 path when Retry is shown.")
    public void retryOrCopy() {
        ReferPage page = reachRefer();
        if (page.isRetryVisible()) {
            page.tapRetry();
            sleepQuiet(2000);
            page.attachScreenshot("rf-i1-retry");
            Allure.parameter("afterRetryUnavailable", String.valueOf(page.isUnavailableVisible()));
            Allure.parameter("afterRetryFull", String.valueOf(page.isFullChromeVisible()));
            assertThat(page.isDisplayedNow()).isTrue();
            assertThat(vendorPackage()).contains("l2b");
            return;
        }
        assertThat(page.isCopyCodeVisible()).isTrue();
        page.tapCopyCode();
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("refer");
    }

    @Test(priority = 2, description = "RF-I2: Refer now → dismiss (share or no-op)")
    @Severity(SeverityLevel.CRITICAL)
    public void referNowDismiss() {
        ReferPage page = reachRefer();
        page.tapReferNow();
        sleepQuiet(1500);
        page.attachScreenshot("rf-i2");
        Allure.parameter("afterReferNow", classifyRentalNow());
        if (!page.isDisplayedNow()) {
            DriverManager.get().navigate().back();
            sleepQuiet(900);
            page = dismissToRefer(page);
        }
        assertThat(page.isDisplayedNow() || page.isReferNowVisible()
                || "refer".equals(classifyRentalNow())
                || "home-drawer".equals(classifyRentalNow()))
                .as("safe after Refer now").isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 3, description = "RF-I3: header Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void headerBackToDrawer() {
        ReferPage page = reachRefer();
        page.tapBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
        assertThat(profileDrawerNow()).isTrue();
    }

    @Test(priority = 4, description = "RF-I4: device Back → drawer")
    @Severity(SeverityLevel.CRITICAL)
    public void deviceBackToDrawer() {
        ReferPage page = reachRefer();
        page.pressDeviceBack();
        sleepQuiet(900);
        assertThat(classifyRentalNow()).isEqualTo("home-drawer");
    }

    @Test(priority = 5, description = "RF-I5: reopen Refer after Back")
    @Severity(SeverityLevel.CRITICAL)
    public void reopenAfterBack() {
        ReferPage page = reachRefer();
        page.tapBack();
        sleepQuiet(800);
        new ProfileDrawerPage().tapRow("Refer & Earn");
        sleepQuiet(1100);
        page.waitUntilLoaded();
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(classifyRentalNow()).isEqualTo("refer");
    }

    @Test(priority = 6, description = "RF-I6: double Retry or double Copy")
    @Severity(SeverityLevel.NORMAL)
    public void doubleRetryOrCopy() {
        ReferPage page = reachRefer();
        for (int i = 0; i < 2; i++) {
            if (page.isRetryVisible()) {
                page.tapRetry();
            } else if (page.isCopyCodeVisible()) {
                page.tapCopyCode();
            }
            sleepQuiet(700);
        }
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 7, description = "RF-I7: Refer now twice safe")
    @Severity(SeverityLevel.NORMAL)
    public void referNowTwice() {
        ReferPage page = reachRefer();
        for (int i = 0; i < 2; i++) {
            page.tapReferNow();
            sleepQuiet(1000);
            if (!page.isDisplayedNow()) {
                DriverManager.get().navigate().back();
                sleepQuiet(700);
                page = dismissToRefer(page);
            }
        }
        assertThat(page.isDisplayedNow() || page.isReferNowVisible()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }

    @Test(priority = 8, description = "RF-I8: scroll keeps title / CTA")
    @Severity(SeverityLevel.NORMAL)
    public void scrollKeepsChrome() {
        ReferPage page = reachRefer();
        DriverManager.get().executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 1400, "width", 600, "height", 600,
                        "direction", "up", "percent", 0.6));
        sleepQuiet(500);
        assertThat(page.isReferTitleVisible() || page.isReferNowVisible()
                || page.isUnavailableVisible()).isTrue();
        DriverManager.get().executeScript("mobile: swipeGesture",
                java.util.Map.of("left", 200, "top", 800, "width", 600, "height", 600,
                        "direction", "down", "percent", 0.6));
        sleepQuiet(500);
        assertThat(page.isDisplayedNow()).isTrue();
    }

    @Test(priority = 9, description = "RF-I9: Back then close drawer → Home")
    @Severity(SeverityLevel.CRITICAL)
    public void backThenCloseDrawerHome() {
        ReferPage page = reachRefer();
        page.tapBack();
        sleepQuiet(800);
        assertThat(profileDrawerNow()).isTrue();
        DriverManager.get().navigate().back();
        sleepQuiet(900);
        assertThat(new HomePage().isDisplayedNow() || "home".equals(classifyRentalNow())).isTrue();
    }

    @Test(priority = 10, description = "RF-I10: stay Vendor after Retry/Copy")
    @Severity(SeverityLevel.NORMAL)
    public void stayVendorAfterAction() {
        ReferPage page = reachRefer();
        if (page.isRetryVisible()) {
            page.tapRetry();
        } else if (page.isCopyCodeVisible()) {
            page.tapCopyCode();
        }
        sleepQuiet(800);
        assertThat(page.isDisplayedNow()).isTrue();
        assertThat(vendorPackage()).contains("l2b");
    }
}
