package com.l2b.vendor.modules.earning.presentation.tests;

import com.l2b.vendor.core.driver.DriverManager;
import com.l2b.vendor.modules.earning.presentation.pages.EarningPage;
import com.l2b.vendor.modules.home.presentation.pages.HomePage;
import com.l2b.vendor.modules.home.presentation.tests.RentalHomeBaseTest;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;

/**
 * Shared 9000000001 path onto Earning. Close QB only. Never Accept / Decline /
 * Log Out. Never complete Withdraw Money (real wallet debit).
 */
public abstract class EarningBaseTest extends RentalHomeBaseTest {

    @Step("Reach Earning via bottom Earning tab")
    protected EarningPage reachEarningViaTab() {
        HomePage home = reachUsableRentalHomeOrFailExtendTime();
        home.tapDesc("Earning");
        EarningPage earning = new EarningPage();
        earning.waitUntilLoaded();
        Allure.parameter("entry", "bottom-tab");
        Allure.parameter("after", classifyRentalNow());
        return earning;
    }

    @Step("Dismiss overlays until Earning (or re-enter via tab)")
    protected EarningPage dismissToEarning(EarningPage earning) {
        for (int i = 0; i < 5; i++) {
            if (earning.isDisplayedNow()) {
                return earning;
            }
            DriverManager.get().navigate().back();
            sleepQuiet(700);
        }
        // Help & Support has no bottom tabs — relaunch Home path if needed.
        if (!earning.isDisplayedNow() && !hasBottomEarningTab()) {
            HomePage home = reachUsableRentalHomeOrFailExtendTime();
            home.tapDesc("Earning");
            earning.waitUntilLoaded();
            return earning;
        }
        if (!earning.isDisplayedNow() && hasBottomEarningTab()) {
            DriverManager.get().findElement(
                    org.openqa.selenium.By.xpath("//*[@content-desc='Earning']")).click();
            earning.waitUntilLoaded();
        }
        return earning;
    }

    protected boolean hasBottomEarningTab() {
        return !DriverManager.get().findElements(
                org.openqa.selenium.By.xpath("//*[@content-desc='Earning']")).isEmpty();
    }

    private static void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
